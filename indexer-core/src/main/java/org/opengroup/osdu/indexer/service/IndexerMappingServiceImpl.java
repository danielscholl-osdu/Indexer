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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lambdaworks.redis.RedisException;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.PutMappingRequest;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.opengroup.osdu.core.common.Constants;
import org.opengroup.osdu.core.common.feature.IFeatureFlag;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.indexer.IndexSchema;
import org.opengroup.osdu.core.common.model.search.RecordMetaAttribute;
import org.opengroup.osdu.core.common.search.ElasticIndexNameResolver;
import org.opengroup.osdu.core.common.search.Preconditions;
import org.opengroup.osdu.indexer.cache.partitionsafe.IndexCache;
import org.opengroup.osdu.indexer.model.Kind;
import org.opengroup.osdu.indexer.util.TypeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.opengroup.osdu.indexer.config.IndexerConfigurationProperties.KEYWORD_LOWER_FEATURE_NAME;

@Service
public class IndexerMappingServiceImpl extends MappingServiceImpl implements IMappingService {

    private static TimeValue REQUEST_TIMEOUT = TimeValue.timeValueMinutes(1);

    @Inject
    private JaxRsDpsLog log;
    @Autowired
    private IndexCache indexCache;
    @Autowired
    private ElasticIndexNameResolver elasticIndexNameResolver;
    @Autowired
    private IFeatureFlag keywordLowerFeatureFlag;


    /**
     * Create a new type in Elasticsearch
     *
     * @param client Elasticsearch client
     * @param index  Index name
     * @param merge  Try to merge mapping if type already exists
     * @throws IOException if cannot create mapping
     */
    public String createMapping(RestHighLevelClient client, IndexSchema schema, String index, boolean merge) throws IOException {

        Map<String, Object> mappingMap = this.getIndexMappingFromRecordSchema(schema);
        String mapping = new Gson().toJson(mappingMap, Map.class);
        this.createMappingWithJson(client, index, schema.getType(), mapping, merge);
        return mapping;
    }

    /*
     * Read schema mapping
     *
     * @param schema Index schema
     * @param type   Mapping type
     * @return String JSON representation of type and elastic type
     *
     * sample index mapping:
     * "properties": {
     *      all meta attributes
     *      "acl": {
     *          "properties": {
                    mapping of all roles
     *          }
     *      },
     *      "legal": {
     *          "properties": {
     *              mapping of all legal properties
     *          }
     *      }
     *      "data": {
     *          "properties": {
     *              all data-source attributes
     *           }
     *       }
     *  }
     * */
    public Map<String, Object> getIndexMappingFromRecordSchema(IndexSchema schema) {

        // entire property block
        Map<String, Object> properties = new HashMap<>();

        // meta  attribute
        Map<String, Object> metaMapping = this.getMetaMapping(schema);

        // data-source attributes
        Map<String, Object> dataMapping = this.getDataMapping(schema);
        if (!dataMapping.isEmpty()) {
            // inner properties.data.properties block
            Map<String, Object> dataProperties = new HashMap<>();
            dataProperties.put(Constants.PROPERTIES, dataMapping);

            // data & meta block
            properties.put(Constants.DATA, dataProperties);
        }
        properties.putAll(metaMapping);

        // entire document properties block
        Map<String, Object> documentMapping = new HashMap<>();
        documentMapping.put(Constants.PROPERTIES, properties);

        // don't add dynamic mapping
        documentMapping.put("dynamic", false);
        return documentMapping;
    }

    private Map<String, Object> getMetaMapping(IndexSchema schema) {
        Map<String, Object> metaMapping = new HashMap<>();
        Kind kind = new Kind(schema.getKind());

        for (Map.Entry<String, Object> entry : schema.getMetaSchema().entrySet()) {
            if (entry.getKey() == RecordMetaAttribute.AUTHORITY.getValue()) {
                metaMapping.put(entry.getKey(), TypeMapper.getMetaAttributeIndexerMapping(entry.getKey(), kind.getAuthority()));
            } else if (entry.getKey() == RecordMetaAttribute.SOURCE.getValue()) {
                metaMapping.put(entry.getKey(), TypeMapper.getMetaAttributeIndexerMapping(entry.getKey(), kind.getSource()));
            } else {
                metaMapping.put(entry.getKey(), TypeMapper.getMetaAttributeIndexerMapping(entry.getKey(), null));
            }
        }
        return metaMapping;
    }

    private Map<String, Object> getDataMapping(IndexSchema schema) {
        Map<String, Object> dataMapping = new HashMap<>();
        boolean keywordLowerEnabled = this.keywordLowerFeatureFlag.isFeatureEnabled(KEYWORD_LOWER_FEATURE_NAME);;

        if (schema.getDataSchema() == null || schema.getDataSchema().isEmpty()) return dataMapping;

        for (Map.Entry<String, Object> entry : schema.getDataSchema().entrySet()) {
            dataMapping.put(entry.getKey(), TypeMapper.getDataAttributeIndexerMapping(entry.getValue(), keywordLowerEnabled));
        }
        return dataMapping;
    }

    @Override
    public void syncIndexMappingIfRequired(RestHighLevelClient restClient, IndexSchema schema) throws Exception {
        String index = this.elasticIndexNameResolver.getIndexNameFromKind(schema.getKind());
        final String cacheKey = String.format("metaAttributeMappingSynced-%s", index);

        try {
            Boolean mappingSynced = this.indexCache.get(cacheKey);
            if (mappingSynced != null && mappingSynced) return;
        } catch (RedisException ex) {
            //In case the format of cache changes then clean the cache
            this.indexCache.delete(cacheKey);
        }

        String jsonResponse = this.getIndexMapping(restClient, index);
        Type type = new TypeToken<Map<String, Object>>() {
        }.getType();
        Map<String, Object> mappings = new Gson().fromJson(jsonResponse, type);

        if (mappings == null || mappings.isEmpty()) return;

        Map<String, Object> props = (Map<String, Object>) mappings.get("properties");

        if (props == null || props.isEmpty()) return;

        List<String> missing = new ArrayList<>();
        for (String attribute : TypeMapper.getMetaAttributesKeys()) {
            if (props.containsKey(attribute)) continue;
            missing.add(attribute);
        }

        Map<String, Object> properties = new HashMap<>();
        Kind kind = new Kind(schema.getKind());
        for (String attribute : missing) {
            if (attribute == RecordMetaAttribute.AUTHORITY.getValue()) {
                properties.put(attribute, TypeMapper.getMetaAttributeIndexerMapping(attribute, kind.getAuthority()));
            } else if (attribute == RecordMetaAttribute.SOURCE.getValue()) {
                properties.put(attribute, TypeMapper.getMetaAttributeIndexerMapping(attribute, kind.getSource()));
            } else {
                properties.put(attribute, TypeMapper.getMetaAttributeIndexerMapping(attribute, null));
            }
        }

        // sync data-source attributes
        Map<String, Object> dataMapping = this.getDataMapping(schema);
        if (!dataMapping.isEmpty()) {
            // inner properties.data.properties block
            Map<String, Object> dataProperties = new HashMap<>();
            dataProperties.put(Constants.PROPERTIES, dataMapping);

            // data & meta block
            properties.put(Constants.DATA, dataProperties);
        }

        if (properties.isEmpty()) {
            this.indexCache.put(cacheKey, true);
            return;
        }

        Map<String, Object> documentMapping = new HashMap<>();
        documentMapping.put(Constants.PROPERTIES, properties);

        String mapping = new Gson().toJson(documentMapping, Map.class);
        this.createMappingWithJson(restClient, index, "_doc", mapping, true);

        this.indexCache.put(cacheKey, true);
    }

    /**
     * Create a new type in Elasticsearch
     *
     * @param client  Elasticsearch client
     * @param index   Index name
     * @param type    Type name
     * @param mapping Mapping if any, null if no specific mapping
     * @param merge   Try to merge mapping if type already exists
     * @throws IOException if cannot create index mapping with input json
     */
    private void createMappingWithJson(RestHighLevelClient client, String index, String type, String mapping, boolean merge)
            throws IOException {

        boolean mappingExist = isTypeExist(client, index, type);
        if (merge || !mappingExist) {
            createTypeWithMappingInElasticsearch(client, index, type, mapping);
        }
    }

    /**
     * Check if a type already exists
     *
     * @param client Elasticsearch client
     * @param index  Index name
     * @param type   Type name
     * @return true if type already exists
     * @throws IOException in case Elasticsearch responded with a status code that indicated an error
     */
    public boolean isTypeExist(RestHighLevelClient client, String index, String type) throws IOException {

        Request request = new Request("HEAD", "/" + index + "/_mapping/" + type);
        Response response = client.getLowLevelClient().performRequest(request);
        return response.getStatusLine().getStatusCode() == 200;
    }

    /**
     * Create a new type in Elasticsearch
     *
     * @param client  Elasticsearch client
     * @param index   Index name
     * @param type    Type name
     * @param mapping Mapping if any, null if no specific mapping
     * @throws IOException if mapping cannot be created
     */
    private Boolean createTypeWithMappingInElasticsearch(RestHighLevelClient client, String index, String type, String mapping) throws IOException {

        Preconditions.checkNotNull(client, "client cannot be null");
        Preconditions.checkNotNull(index, "index cannot be null");
        Preconditions.checkNotNull(type, "type cannot be null");

        try {
            if (mapping != null) {
                PutMappingRequest request = new PutMappingRequest(index);
                request.source(mapping, XContentType.JSON);
                request.setTimeout(REQUEST_TIMEOUT);
                AcknowledgedResponse response = client.indices().putMapping(request, RequestOptions.DEFAULT);
                return response.isAcknowledged();
            }
        } catch (ElasticsearchException e) {
            throw new AppException(
                    e.status().getStatus(),
                    e.getMessage(),
                    String.format("Could not create type mapping %s/%s.", index, type),
                    String.format("Failed creating mapping: %s", mapping),
                    e);
        }
        return false;
    }
}
