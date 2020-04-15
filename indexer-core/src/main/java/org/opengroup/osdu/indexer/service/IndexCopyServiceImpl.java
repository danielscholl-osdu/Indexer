// Copyright 2017-2019, Schlumberger
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.indexer.service;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestHighLevelClient;
import org.opengroup.osdu.core.common.model.search.ClusterSettings;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.search.ElasticIndexNameResolver;
import org.opengroup.osdu.core.common.search.IndicesService;
import org.opengroup.osdu.core.common.search.Preconditions;
import org.opengroup.osdu.indexer.logging.AuditLogger;
import org.opengroup.osdu.core.common.model.indexer.IElasticSettingService;
import org.opengroup.osdu.indexer.util.ElasticClientHandler;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

@Service
public class IndexCopyServiceImpl implements IndexCopyService {

    @Inject
    private ElasticClientHandler elasticClientHandler;
    @Inject
    private ElasticIndexNameResolver elasticIndexNameResolver;
    @Inject
    private IndicesService indicesService;
    @Inject
    private IElasticSettingService elasticSettingService;
    @Inject
    private IndexerMappingService mappingService;
    @Inject
    private DpsHeaders headersInfo;
    @Inject
    private AuditLogger auditLogger;

    @Override
    public String fetchTaskStatus(String taskId) {
        try (RestHighLevelClient restClient = this.elasticClientHandler.createRestClient()) {
            Request request = new Request("GET", String.format("/_tasks/%s", taskId));
            Response response = restClient.getLowLevelClient().performRequest(request);
            this.auditLogger.getTaskStatus(Lists.newArrayList(taskId));
            return EntityUtils.toString(response.getEntity());
        } catch (IOException e) {
            throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Unknown error", e.getMessage(), e);
        }
    }

    /**
     * This method is used to copy one index from common cluster to tenant cluster.
     *
     * @param kind request sent to ask the copying operation, includes kind and tenant
     * @throws IOException if upstream server cannot process the request
     */
    @Override
    public String copyIndex(String kind) throws IOException {
        Preconditions.checkNotNull(kind, "kind can't be null");

        String originalAccountId = this.headersInfo.getPartitionId();
        String toBeCopiedIndex = this.elasticIndexNameResolver.getIndexNameFromKind(kind);

        String typeOfToBeCopiedIndex = kind.split(":")[2];
        if (typeOfToBeCopiedIndex == null) {
            throw new AppException(HttpStatus.SC_NOT_FOUND, "Fail to find the type of the index", "Fail to find the type of the given index in common cluster.");
        }

        Map<String, Object> mappingsMap = this.getIndexMappingsFromCommonCluster(toBeCopiedIndex, typeOfToBeCopiedIndex);
        String[] commonCluster = this.getCommonClusterInformation();

        this.createIndexInTenantCluster(originalAccountId, toBeCopiedIndex, typeOfToBeCopiedIndex, mappingsMap);
        String taskStatus = this.reindexInTenantCluster(originalAccountId, toBeCopiedIndex, commonCluster);
        this.auditLogger.copyIndex(Lists.newArrayList(String.format("Kind:%s", kind), String.format("Task status: %s", taskStatus)));
        return taskStatus;
    }

    /**
     * This method is used to format the body of remote reindex request.
     *
     * @param host     host of the remote cluster
     * @param username username of the remote cluster
     * @param password password of the remote cluster
     * @param index    the index to be copied
     * @return request body in json string format
     */
    private String formatReindexRequestBody(String host, String username, String password, String index) {
        Map<String, String> remoteMap = new HashMap<>();
        remoteMap.put("host", host);
        remoteMap.put("username", username);
        remoteMap.put("password", password);

        Map<String, Object> sourceMap = new HashMap<>();
        sourceMap.put("index", index);
        sourceMap.put("remote", remoteMap);
        sourceMap.put("size", 10000);

        Map<String, String> destMap = new HashMap<>();
        destMap.put("index", index);

        Map<String, Object> map = new HashMap<>();
        map.put("source", sourceMap);
        map.put("dest", destMap);

        Gson gson = new Gson();
        return gson.toJson(map);
    }

    /**
     * This method is used to extract cluster information from cluster settings.
     *
     * @param setting setting of the cluster, including host, port, username and password
     * @return the host which combined the hostname, scheme and port together, username, and password
     */
    private String[] extractInfoFromClusterSetting(ClusterSettings setting) {

        String[] clusterInfo = new String[3];

        StringBuilder host = new StringBuilder("https://");
        host.append(setting.getHost())
                .append(":")
                .append(setting.getPort());
        clusterInfo[0] = host.toString();

        String userAndPwd = setting.getUserNameAndPassword();
        int indexOfColon = userAndPwd.indexOf(':');
        clusterInfo[1] = userAndPwd.substring(0, indexOfColon);
        clusterInfo[2] = userAndPwd.substring(indexOfColon + 1);

        return clusterInfo;
    }

    /**
     * This method is used to cast mapping to fit the parameter requirement.
     *
     * @param mapping input map in json string format
     * @return output map in Map<String, Object> format
     */
    private Map<String, Object> castMappingsMap(String mapping, String indexType, String index) {
        Type type = new TypeToken<Map<String, Map<String, Map<String, Map<String, Object>>>>>() {}.getType();
        Map<String, Map<String, Map<String, Map<String, Object>>>> indexMap = new Gson().fromJson(mapping, type);
        Map<String, Map<String, Map<String, Object>>> mappingMap = indexMap.get(index);
        Map<String, Map<String, Object>> mappingWithoutMappings = mappingMap.get("mappings");

        return mappingWithoutMappings.get(indexType);
    }

    public Map<String, Object> getIndexMappingsFromCommonCluster(String toBeCopiedIndex, String typeOfToBeCopiedIndex) throws IOException {
        this.headersInfo.getHeaders().put(DpsHeaders.ACCOUNT_ID, TenantInfo.COMMON);
        this.headersInfo.getHeaders().put(DpsHeaders.DATA_PARTITION_ID, TenantInfo.COMMON);
        try (RestHighLevelClient commonClient = this.elasticClientHandler.createRestClient()) {
            String indexMapping = this.mappingService.getIndexMapping(commonClient, toBeCopiedIndex);
            return castMappingsMap(indexMapping, typeOfToBeCopiedIndex, toBeCopiedIndex);
        } catch (Exception e) {
            throw new IOException("Fail to get mapping for the given index from common cluster.");
        }
    }

    public String[] getCommonClusterInformation() throws IOException {
        this.headersInfo.getHeaders().put(DpsHeaders.ACCOUNT_ID, TenantInfo.COMMON);
        this.headersInfo.getHeaders().put(DpsHeaders.DATA_PARTITION_ID, TenantInfo.COMMON);
        String[] commonCluster = extractInfoFromClusterSetting(this.elasticSettingService.getElasticClusterInformation());
        if (commonCluster.length != 3) {
            throw new IOException("fail to get the information of common cluster.");
        }
        return commonCluster;
    }

    public void createIndexInTenantCluster(String originalAccountId, String toBeCopiedIndex, String typeOfToBeCopiedIndex, Map<String, Object> mappingsMap) throws IOException {
        this.headersInfo.getHeaders().put(DpsHeaders.ACCOUNT_ID, originalAccountId);
        this.headersInfo.getHeaders().put(DpsHeaders.DATA_PARTITION_ID, originalAccountId);
        try (RestHighLevelClient tenantClient = this.elasticClientHandler.createRestClient()) {
            if (!this.indicesService.createIndex(tenantClient, toBeCopiedIndex, null, typeOfToBeCopiedIndex, mappingsMap)) {
                throw new AppException(HttpStatus.SC_NOT_FOUND, "Fail to create new index", "Fail to create new corresponding new index in tenant cluster.");
            }
        }
    }

    public String reindexInTenantCluster(String originalAccountId, String toBeCopiedIndex, String[] commonCluster) throws IOException {
        this.headersInfo.getHeaders().put(DpsHeaders.ACCOUNT_ID, originalAccountId);
        this.headersInfo.getHeaders().put(DpsHeaders.DATA_PARTITION_ID, originalAccountId);
        try (RestHighLevelClient tenantClient = this.elasticClientHandler.createRestClient()) {
            String json = formatReindexRequestBody(commonCluster[0], commonCluster[1], commonCluster[2], toBeCopiedIndex);
            HttpEntity requestBody = new NStringEntity(json, ContentType.APPLICATION_JSON);
            Request request = new Request("POST", "/_reindex?wait_for_completion=false");
            request.setEntity(requestBody);
            Response response = tenantClient.getLowLevelClient().performRequest(request);
            return EntityUtils.toString(response.getEntity());
        }
    }
}
