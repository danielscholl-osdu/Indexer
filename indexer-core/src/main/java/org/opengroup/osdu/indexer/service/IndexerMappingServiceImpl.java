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
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import org.apache.http.HttpStatus;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetFieldMappingsRequest;
import org.elasticsearch.client.indices.GetFieldMappingsResponse;
import org.elasticsearch.client.indices.PutMappingRequest;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.opengroup.osdu.core.common.Constants;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.indexer.IndexSchema;
import org.opengroup.osdu.core.common.search.Preconditions;
import org.opengroup.osdu.indexer.util.ElasticClientHandler;
import org.opengroup.osdu.indexer.util.TypeMapper;
import org.springframework.stereotype.Service;

@Service
public class IndexerMappingServiceImpl extends MappingServiceImpl implements IndexerMappingService {

    @Inject
    private JaxRsDpsLog log;
    @Inject
    private ElasticClientHandler elasticClientHandler;
    private TimeValue REQUEST_TIMEOUT = TimeValue.timeValueMinutes(1);


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
        Map<String, Object> metaMapping = new HashMap<>();
        for (Map.Entry<String, Object> entry : schema.getMetaSchema().entrySet()) {
            metaMapping.put(entry.getKey(), TypeMapper.getMetaAttributeIndexerMapping(entry.getKey()));
        }

        // data-source attributes
        Map<String, Object> dataMapping = new HashMap<>();
        if (schema.getDataSchema() != null) {
            for (Map.Entry<String, Object> entry : schema.getDataSchema().entrySet()) {
                dataMapping.put(entry.getKey(), TypeMapper.getDataAttributeIndexerMapping(entry.getValue()));
            }

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

    @Override
    public void updateIndexMappingForIndicesOfSameType(Set<String> indices, String fieldName) throws Exception {
        try (RestHighLevelClient restClient = this.elasticClientHandler.createRestClient()) {
            if(!updateMappingToEnableKeywordIndexingForField(restClient,indices,fieldName)){
                throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Elastic error", "Error updating index mapping.", String.format("Failed to get confirmation from elastic server mapping update for indices: %s", indices));
            }
        }
    }

    private boolean updateMappingToEnableKeywordIndexingForField(RestHighLevelClient client, Set<String> indicesSet, String fieldName) throws IOException {
        String[] indices = indicesSet.toArray(new String[indicesSet.size()]);
        Map<String, Map<String, Map<String, GetFieldMappingsResponse.FieldMappingMetadata>>> indexMappingMap = getIndexFieldMap(new String[]{"data."+fieldName}, client, indices);
        boolean failure = false;
        for (String index : indicesSet) {
            if (indexMappingMap.get(index)!=null && updateMappingForAllIndicesOfSameTypeToEnableKeywordIndexingForField(client, index, indexMappingMap.get(index), fieldName)) {
                log.info(String.format("Updated field: %s | index:  %s", fieldName, index));
            } else {
                failure=true;
                log.warning(String.format("Failed to update field: %s | index  %s", fieldName, index));
            }
        }
        return !failure;
    }

    private Map<String, Map<String, Map<String, GetFieldMappingsResponse.FieldMappingMetadata>>> getIndexFieldMap(String[] fieldNames, RestHighLevelClient client, String[] indices) throws IOException  {
        Map<String, Map<String, Map<String, GetFieldMappingsResponse.FieldMappingMetadata>>> indexMappingMap = new HashMap<>();
        GetFieldMappingsRequest request = new GetFieldMappingsRequest();
        request.indices(indices);
        request.fields(fieldNames);
        try {
            GetFieldMappingsResponse response = client.indices().getFieldMapping(request, RequestOptions.DEFAULT);
            if (response != null && !response.mappings().isEmpty()) {
                final Map<String, Map<String, GetFieldMappingsResponse.FieldMappingMetadata>> mappings = response.mappings();
                for (String index : indices) {
                    if (mappings != null && !mappings.isEmpty()) {
                        indexMappingMap.put(index, mappings);
                    }
                }
            }

            return indexMappingMap;
        } catch (ElasticsearchException e) {
            log.error(String.format("Failed to get indices: %s. | Error: %s", Arrays.toString(indices), e));
            throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Elastic error", "Error getting indices.", String.format("Failed to get indices error: %s", Arrays.toString(indices)));
        }
    }
    
    private boolean updateMappingForAllIndicesOfSameTypeToEnableKeywordIndexingForField(
            RestHighLevelClient client, String index, Map<String, Map<String, GetFieldMappingsResponse.FieldMappingMetadata>> indexMapping, String fieldName) throws IOException {

        PutMappingRequest request = new PutMappingRequest(index);
        String type = indexMapping.keySet().iterator().next();
        if(type.isEmpty()) {
        	log.error(String.format("Could not find type of the mappings for index: %s.", index));
            return false;
        }

        request.setTimeout(REQUEST_TIMEOUT);
        Map<String, GetFieldMappingsResponse.FieldMappingMetadata> metaData = indexMapping.get(type);
        if(metaData==null || metaData.get("data." + fieldName)==null) {
            log.error(String.format("Could not find field: %s in the mapping of index: %s.", fieldName, index));
            return false;
        }

        GetFieldMappingsResponse.FieldMappingMetadata fieldMetaData = metaData.get("data." + fieldName);
        Map<String, Object> source = fieldMetaData.sourceAsMap();
        if(!source.containsKey(fieldName)){
            log.error(String.format("Could not find field: %s in the mapping of index: %s.", fieldName, index));
            return false;
        }

        //Index the field with additional keyword type
        Map<String, Object> keywordMap = new HashMap<>();
        keywordMap.put(Constants.TYPE, "keyword");
        Map<String, Object> fieldIndexTypeMap = new HashMap<>();
        fieldIndexTypeMap.put("keyword", keywordMap);
        Map<String, Object> dataFieldMap = (Map<String, Object>) source.get(fieldName);
        dataFieldMap.put("fields", fieldIndexTypeMap);
        Map<String, Object> dataProperties = new HashMap<>();
        dataProperties.put(fieldName, dataFieldMap);
        Map<String, Object> mapping = new HashMap<>();
        mapping.put(Constants.PROPERTIES, dataProperties);
        Map<String, Object> data = new HashMap<>();
        data.put(Constants.DATA,mapping);
        Map<String, Object> properties = new HashMap<>();
        properties.put(Constants.PROPERTIES, data);

        request.source(new Gson().toJson(properties), XContentType.JSON);

        try {
            AcknowledgedResponse response = client.indices().putMapping(request, RequestOptions.DEFAULT);
            boolean isIndicesUpdated = updateIndices(client, index);
            return response.isAcknowledged() && isIndicesUpdated;

        } catch (Exception e) {
            log.error(String.format("Could not update mapping of index: %s. | Error: %s", index, e));
            return false;
        }
    }

    private boolean updateIndices(RestHighLevelClient client, String index) throws IOException {
        UpdateByQueryRequest request = new UpdateByQueryRequest(index);
        request.setConflicts("proceed");
        BulkByScrollResponse response = client.updateByQuery(request, RequestOptions.DEFAULT);
        if(!response.getBulkFailures().isEmpty()) {
        	log.error(String.format("Could not update index: %s.",index));
        	return false;
        }
		return true;
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