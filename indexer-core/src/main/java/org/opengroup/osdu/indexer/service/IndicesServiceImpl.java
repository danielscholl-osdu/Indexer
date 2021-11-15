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

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lambdaworks.redis.RedisException;
import org.apache.http.HttpStatus;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestStatus;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.search.IndexInfo;
import org.opengroup.osdu.core.common.provider.interfaces.IIndexCache;
import org.opengroup.osdu.core.common.search.ElasticIndexNameResolver;
import org.opengroup.osdu.core.common.search.Preconditions;
import org.opengroup.osdu.indexer.util.ElasticClientHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequestScope
public class IndicesServiceImpl implements IndexerIndicesService {

    @Autowired
    private ElasticClientHandler elasticClientHandler;
    @Autowired
    private ElasticIndexNameResolver elasticIndexNameResolver;
    @Autowired
    private IIndexCache indexCache;
    @Autowired
    private JaxRsDpsLog log;

    private TimeValue REQUEST_TIMEOUT = TimeValue.timeValueMinutes(1);

    private static final Settings DEFAULT_INDEX_SETTINGS = Settings.builder()
            .put("index.refresh_interval", "30s")
            .put("index.number_of_replicas", "1")
            .put("index.number_of_shards", "1").build();

    /**
     * Create a new index in Elasticsearch
     *
     * @param client   Elasticsearch client
     * @param index    Index name
     * @param settings Settings if any, null if no specific settings
     * @param type     type in index, required if type is specified
     * @param mapping  mapping if any, must be a json map
     * @throws ElasticsearchStatusException, IOException if it cannot create index
     */
    public boolean createIndex(RestHighLevelClient client, String index, Settings settings, String type, Map<String, Object> mapping) throws ElasticsearchStatusException, IOException {

        Preconditions.checkArgument(client, Objects::nonNull, "client cannot be null");
        Preconditions.checkArgument(index, Objects::nonNull, "index cannot be null");

        try {
            CreateIndexRequest request = new CreateIndexRequest(index);
            request.settings(settings != null ? settings : DEFAULT_INDEX_SETTINGS);
            if (mapping != null) {
                String mappingJsonString = new Gson().toJson(mapping, Map.class);
                request.mapping(mappingJsonString, XContentType.JSON);
            }
            request.setTimeout(REQUEST_TIMEOUT);
            CreateIndexResponse response = client.indices().create(request, RequestOptions.DEFAULT);
            // cache the index status
            boolean indexStatus = response.isAcknowledged() && response.isShardsAcknowledged();
            if (indexStatus) this.indexCache.put(index, true);

            return indexStatus;
        } catch (ElasticsearchStatusException e) {
            if (e.status() == RestStatus.BAD_REQUEST && (e.getMessage().contains("resource_already_exists_exception"))) {
                log.info("Index already exists. Ignoring error...");
                // cache the index status
                this.indexCache.put(index, true);
                return true;
            }
            throw e;
        }
    }

    /**
     * Check if an index already exists
     *
     * @param index Index name
     * @return index details if index already exists
     * @throws IOException if request cannot be processed
     */
    public boolean isIndexExist(RestHighLevelClient client, String index) throws IOException {
        try {
            try {
                Boolean isIndexExist = (Boolean) this.indexCache.get(index);
                if (isIndexExist != null && isIndexExist) return true;
            } catch (RedisException ex) {
                //In case the format of cache changes then clean the cache
                this.indexCache.delete(index);
            }
            GetIndexRequest request = new GetIndexRequest(index);
            boolean exists = client.indices().exists(request, RequestOptions.DEFAULT);
            if (exists) this.indexCache.put(index, true);
            return exists;
        } catch (ElasticsearchException exception) {
            if (exception.status() == RestStatus.NOT_FOUND) return false;
            throw new AppException(
                    exception.status().getStatus(),
                    exception.getMessage(),
                    String.format("Error getting index: %s status", index),
                    exception);
        }
    }

    /**
     * Check if an index already exists
     *
     * @param index Index name
     * @return index details if index already exists
     * @throws IOException if request cannot be processed
     */
    public boolean isIndexReady(RestHighLevelClient client, String index) throws IOException {
        try {
            try {
                Boolean isIndexExist = (Boolean) this.indexCache.get(index);
                if (isIndexExist != null && isIndexExist) return true;
            } catch (RedisException ex) {
                //In case the format of cache changes then clean the cache
                this.indexCache.delete(index);
            }
            GetIndexRequest request = new GetIndexRequest(index);
            boolean exists = client.indices().exists(request, RequestOptions.DEFAULT);
            if (!exists) return false;
            ClusterHealthRequest indexHealthRequest = new ClusterHealthRequest();
            indexHealthRequest.indices(index);
            indexHealthRequest.timeout(REQUEST_TIMEOUT);
            indexHealthRequest.waitForYellowStatus();
            ClusterHealthResponse healthResponse = client.cluster().health(indexHealthRequest, RequestOptions.DEFAULT);
            if (healthResponse.status() == RestStatus.OK) {
                this.indexCache.put(index, true);
                return true;
            }
            return false;
        } catch (ElasticsearchException exception) {
            if (exception.status() == RestStatus.NOT_FOUND) return false;
            throw new AppException(
                    exception.status().getStatus(),
                    exception.getMessage(),
                    String.format("Error getting index: %s status", index),
                    exception);
        }
    }

    /**
     * Deletes index if user has required role: search.admin
     *
     * @param client Elasticsearch client
     * @param index  Index name
     */
    public boolean deleteIndex(RestHighLevelClient client, String index) throws ElasticsearchException, IOException, AppException {
        boolean responseStatus = removeIndexInElasticsearch(client, index);
        if (responseStatus) {
            this.clearCacheOnIndexDeletion(index);
        }
        return responseStatus;
    }

    /**
     * Deletes index if user has required role: search.admin
     *
     * @param index Index name
     */
    public boolean deleteIndex(String index) throws ElasticsearchException, IOException, AppException {
        try (RestHighLevelClient client = this.elasticClientHandler.createRestClient()) {
            return deleteIndex(client, index);
        }
    }

    /**
     * Remove index in Elasticsearch
     *
     * @param client Elasticsearch client
     * @param index  Index name
     * @throws Exception Throws {@link AppException} if index is not found or elastic cannot delete the index
     */
    private boolean removeIndexInElasticsearch(RestHighLevelClient client, String index) throws ElasticsearchException, IOException, AppException {

        Preconditions.checkArgument(client, Objects::nonNull, "client cannot be null");
        Preconditions.checkArgument(index, Objects::nonNull, "index cannot be null");

        try {
            DeleteIndexRequest request = new DeleteIndexRequest(index);
            request.timeout(REQUEST_TIMEOUT);
            AcknowledgedResponse response = client.indices().delete(request, RequestOptions.DEFAULT);
            if (!response.isAcknowledged()) {
                throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Index deletion error", String.format("Could not delete index %s", index));
            }
            return true;
        } catch (ElasticsearchException exception) {
            if (exception.status() == RestStatus.NOT_FOUND) {
                throw new AppException(HttpStatus.SC_NOT_FOUND, "Index deletion error", notFoundErrorMessage(index), exception);
            } else if (exception.status() == RestStatus.BAD_REQUEST && exception.getMessage().contains("Cannot delete indices that are being snapshotted")) {
                throw new AppException(HttpStatus.SC_CONFLICT, "Index deletion error", "Unable to delete the index because it is currently locked. Try again in few minutes.", exception);
            }
            throw exception;
        }
    }

    // cron may not have kind but index delete api may
    private String notFoundErrorMessage(String index) {
        String kind = this.elasticIndexNameResolver.getKindFromIndexName(index);
        return Strings.isNullOrEmpty(kind) ? String.format("Index %s not found", index) : String.format("Kind %s not found", kind);
    }

    /**
     * Remove index in Elasticsearch
     *
     * @param client       Elasticsearch client
     * @param indexPattern Index pattern
     * @throws IOException Throws {@link IOException} if elastic cannot complete the request
     */
    public List<IndexInfo> getIndexInfo(RestHighLevelClient client, String indexPattern) throws IOException {

        Preconditions.checkArgument(client, Objects::nonNull, "client cannot be null");

        String requestUrl = Strings.isNullOrEmpty(indexPattern)
                ? "/_cat/indices/*,-.*?h=index,docs.count,creation.date&s=docs.count:asc&format=json"
                : String.format("/_cat/indices/%s?h=index,docs.count,creation.date&format=json", indexPattern);

        Request request = new Request("GET", requestUrl);
        Response response = client.getLowLevelClient().performRequest(request);
        String str = EntityUtils.toString(response.getEntity());
        final Type typeOf = new TypeToken<List<IndexInfo>>() {
        }.getType();
        return new Gson().fromJson(str, typeOf);
    }

    private void clearCacheOnIndexDeletion(String index) {
        final String syncCacheKey = String.format("metaAttributeMappingSynced-%s", index);
        this.indexCache.delete(index);
        this.indexCache.delete(syncCacheKey);
    }
}