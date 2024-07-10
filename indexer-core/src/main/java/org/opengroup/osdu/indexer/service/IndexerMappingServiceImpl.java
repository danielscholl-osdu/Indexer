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

import static org.opengroup.osdu.core.common.model.search.RecordMetaAttribute.BAG_OF_WORDS;
import static org.opengroup.osdu.indexer.config.IndexerConfigurationProperties.BAG_OF_WORDS_FEATURE_NAME;
import static org.opengroup.osdu.indexer.config.IndexerConfigurationProperties.KEYWORD_LOWER_FEATURE_NAME;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch.indices.GetMappingRequest;
import co.elastic.clients.elasticsearch.indices.GetMappingResponse;
import co.elastic.clients.elasticsearch.indices.PutMappingRequest;
import co.elastic.clients.elasticsearch.indices.PutMappingResponse;
import co.elastic.clients.elasticsearch.indices.get_mapping.IndexMappingRecord;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lambdaworks.redis.RedisException;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opengroup.osdu.core.common.Constants;
import org.opengroup.osdu.core.common.feature.IFeatureFlag;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.indexer.IndexSchema;
import org.opengroup.osdu.core.common.model.search.RecordMetaAttribute;
import org.opengroup.osdu.core.common.search.ElasticIndexNameResolver;
import org.opengroup.osdu.core.common.search.Preconditions;
import org.opengroup.osdu.indexer.cache.partitionsafe.IndexCache;
import org.opengroup.osdu.indexer.model.Kind;
import org.opengroup.osdu.indexer.service.exception.ElasticsearchMappingException;
import org.opengroup.osdu.indexer.util.TypeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IndexerMappingServiceImpl extends MappingServiceImpl implements IMappingService {

    private static final Time REQUEST_TIMEOUT = Time.of(builder -> builder.time("1m"));
    @Inject
    private JaxRsDpsLog log;
    @Autowired
    private IndexCache indexCache;
    @Autowired
    private ElasticIndexNameResolver elasticIndexNameResolver;
    @Autowired
    private IFeatureFlag featureFlagChecker;


    /**
     * Create a new type in Elasticsearch
     *
     * @param client Elasticsearch client
     * @param index  Index name
     * @param merge  Try to merge mapping if type already exists
     * @throws IOException if cannot create mapping
     */
    public String createMapping(ElasticsearchClient client, IndexSchema schema, String index, boolean merge) throws IOException {

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

        boolean bagOfWordsEnabled = this.featureFlagChecker.isFeatureEnabled(BAG_OF_WORDS_FEATURE_NAME);
        if(bagOfWordsEnabled){
            schema.getMetaSchema().put(BAG_OF_WORDS.getValue(), null);
        }

        for (Map.Entry<String, Object> entry : schema.getMetaSchema().entrySet()) {
            if (entry.getKey().equals(RecordMetaAttribute.AUTHORITY.getValue())) {
                metaMapping.put(entry.getKey(), TypeMapper.getMetaAttributeIndexerMapping(entry.getKey(), kind.getAuthority()));
            } else if (entry.getKey().equals(RecordMetaAttribute.SOURCE.getValue())) {
                metaMapping.put(entry.getKey(), TypeMapper.getMetaAttributeIndexerMapping(entry.getKey(), kind.getSource()));
            } else {
                metaMapping.put(entry.getKey(), TypeMapper.getMetaAttributeIndexerMapping(entry.getKey(), null));
            }
        }
        return metaMapping;
    }

    private Map<String, Object> getDataMapping(IndexSchema schema) {
        Map<String, Object> dataMapping = new HashMap<>();
        boolean keywordLowerEnabled = this.featureFlagChecker.isFeatureEnabled(KEYWORD_LOWER_FEATURE_NAME);
        boolean bagOfWordsEnabled = this.featureFlagChecker.isFeatureEnabled(BAG_OF_WORDS_FEATURE_NAME);

        if (schema.getDataSchema() == null || schema.getDataSchema().isEmpty()) return dataMapping;

        for (Map.Entry<String, Object> entry : schema.getDataSchema().entrySet()) {
            dataMapping.put(entry.getKey(), TypeMapper.getDataAttributeIndexerMapping(entry.getValue(), keywordLowerEnabled, bagOfWordsEnabled));
        }
        return dataMapping;
    }

    @Override
    public void syncMetaAttributeIndexMappingIfRequired(ElasticsearchClient restClient, IndexSchema schema) throws Exception {
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
            if (attribute.equals(RecordMetaAttribute.AUTHORITY.getValue())) {
                properties.put(attribute, TypeMapper.getMetaAttributeIndexerMapping(attribute, kind.getAuthority()));
            } else if (attribute.equals(RecordMetaAttribute.SOURCE.getValue())) {
                properties.put(attribute, TypeMapper.getMetaAttributeIndexerMapping(attribute, kind.getSource()));
            } else {
                properties.put(attribute, TypeMapper.getMetaAttributeIndexerMapping(attribute, null));
            }
        }
        boolean bagOfWordsEnabled = this.featureFlagChecker.isFeatureEnabled(BAG_OF_WORDS_FEATURE_NAME);
        if (bagOfWordsEnabled) {
            // sync data-source attributes
            Map<String, Object> dataMapping = this.getDataMapping(schema);
            if (!dataMapping.isEmpty()) {
                // inner properties.data.properties block
                Map<String, Object> dataProperties = new HashMap<>();
                dataProperties.put(Constants.PROPERTIES, dataMapping);

                // data & meta block
                properties.put(Constants.DATA, dataProperties);
            }
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
    private void createMappingWithJson(ElasticsearchClient client, String index, String type, String mapping, boolean merge)
            throws IOException {

        boolean mappingExist = isTypeExist(client, index, type);
        if (merge || !mappingExist) {
            createTypeWithMappingInElasticsearch(client, index, mapping);
        }
    }



    /**
     * Check if a type (mapping) already exists
     *
     * @param client Elasticsearch client
     * @param index  Index name
     * @param type   Type (mapping) name
     * @return true if type (mapping) already exists
     * @throws IOException in case Elasticsearch responded with a status code that indicated an error
     */
    public boolean isTypeExist(ElasticsearchClient client, String index, String type) throws IOException {
        GetMappingRequest request = new GetMappingRequest.Builder()
            .index(index)
            .build();

        GetMappingResponse response = client.indices().getMapping(request);

        Map<String, IndexMappingRecord> mappings = response.result();

        if (mappings.containsKey(index)) {
            IndexMappingRecord mappingRecord = mappings.get(index);
            return mappingRecord.mappings().properties().containsKey(type);
        }

        return false;
    }

    /**
     * Create a new type in Elasticsearch
     *
     * @param client  Elasticsearch client
     * @param index   Index name
     * @param mapping Mapping if any, null if no specific mapping
     * @throws IOException if mapping cannot be created
     */
    private Boolean createTypeWithMappingInElasticsearch(ElasticsearchClient client, String index, String mapping) throws IOException {
        Preconditions.checkNotNull(client, "client cannot be null");
        Preconditions.checkNotNull(index, "index cannot be null");

        try {
            if (mapping != null) {
                PutMappingRequest request = PutMappingRequest.of(b -> b
                    .index(index)
                    .timeout(REQUEST_TIMEOUT)
                    .withJson(new StringReader(mapping))
                );
                PutMappingResponse response = client.indices().putMapping(request);
                return response.acknowledged();
            }
        } catch (ElasticsearchException e) {
            throw new ElasticsearchMappingException("Failed to create mapping: " + e.getMessage(), e.status());
        }
        return false;
    }
}
