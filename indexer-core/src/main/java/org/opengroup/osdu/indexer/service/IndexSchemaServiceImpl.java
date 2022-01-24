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
import org.apache.http.HttpStatus;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.client.RestHighLevelClient;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.RequestStatus;
import org.opengroup.osdu.core.common.model.indexer.ElasticType;
import org.opengroup.osdu.core.common.model.indexer.IndexSchema;
import org.opengroup.osdu.core.common.model.indexer.OperationType;
import org.opengroup.osdu.core.common.model.search.RecordMetaAttribute;
import org.opengroup.osdu.core.common.model.storage.Schema;
import org.opengroup.osdu.core.common.model.storage.SchemaItem;
import org.opengroup.osdu.core.common.search.ElasticIndexNameResolver;
import org.opengroup.osdu.indexer.provider.interfaces.ISchemaCache;
import org.opengroup.osdu.indexer.schema.converter.exeption.SchemaProcessingException;
import org.opengroup.osdu.indexer.util.ElasticClientHandler;
import org.opengroup.osdu.indexer.util.TypeMapper;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class IndexSchemaServiceImpl implements IndexSchemaService {

    private static final String FLATTENED_SCHEMA = "_flattened";

    private final Gson gson = new Gson();

    @Inject
    private JaxRsDpsLog log;
    @Inject
    private SchemaService schemaProvider;
    @Inject
    private ElasticClientHandler elasticClientHandler;
    @Inject
    private ElasticIndexNameResolver elasticIndexNameResolver;
    @Inject
    private IMappingService mappingService;
    @Inject
    private IndicesService indicesService;
    @Inject
    private ISchemaCache schemaCache;

    public void processSchemaMessages(Map<String, OperationType> schemaMsgs) throws IOException {
        try (RestHighLevelClient restClient = this.elasticClientHandler.createRestClient()) {
            schemaMsgs.entrySet().forEach(msg -> {
                try {
                    processSchemaEvents(restClient, msg);
                } catch (IOException | ElasticsearchStatusException | URISyntaxException e) {
                    throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "unable to process schema update", e.getMessage());
                }
            });
        }
    }

    private void processSchemaEvents(RestHighLevelClient restClient, Map.Entry<String, OperationType> msg) throws IOException, ElasticsearchStatusException, URISyntaxException {
        String kind = msg.getKey();
        String index = this.elasticIndexNameResolver.getIndexNameFromKind(kind);

        boolean indexExist = this.indicesService.isIndexExist(restClient, index);

        if (msg.getValue() == OperationType.create_schema) {
            this.processSchemaUpsertEvent(restClient, kind);
        } else if (msg.getValue() == OperationType.purge_schema) {
            if (indexExist) {
                // reset schema cache
                this.invalidateCache(kind);
            } else {
                // log warning
                log.warning(String.format("Kind: %s not found", kind));
            }
        }
    }

    @Override
    public void processSchemaUpsertEvent(RestHighLevelClient restClient, String kind) throws IOException, ElasticsearchStatusException, URISyntaxException {
        String index = this.elasticIndexNameResolver.getIndexNameFromKind(kind);
        boolean indexExist = this.indicesService.isIndexExist(restClient, index);

        // reset cache and get new schema
        this.invalidateCache(kind);
        IndexSchema schemaObj = this.getIndexerInputSchema(kind, true);
        if (schemaObj.isDataSchemaMissing()) {
            log.warning(String.format("schema not found for kind: %s", kind));
            return;
        }

        if (indexExist) {
            try {
                // merge the mapping
                this.mappingService.createMapping(restClient, schemaObj, index, true);
            } catch (AppException e) {
                // acknowledge for TaskQueue and not retry
                if (e.getError().getCode() == HttpStatus.SC_BAD_REQUEST) {
                    throw new AppException(RequestStatus.SCHEMA_CONFLICT, e.getError().getReason(), "error creating or merging index mapping");
                }
                throw e;
            }
        } else {
            // create index with mapping
            Map<String, Object> mapping = this.mappingService.getIndexMappingFromRecordSchema(schemaObj);
            this.indicesService.createIndex(restClient, index, null, schemaObj.getType(), mapping);
        }
    }

    @Override
    public IndexSchema getIndexerInputSchema(String kind, List<String> errors) throws AppException, UnsupportedEncodingException, URISyntaxException {
        try {
            return getIndexerInputSchema(kind, false);
        } catch (SchemaProcessingException ex) {
            log.error(ex.getMessage(), ex);
            errors.add(ex.getMessage());
        } catch (RuntimeException ex) {
            String msg = String.format("Failed to get the schema from the Schema service, kind: %s | message: %s", kind, ex.getMessage());
            log.error(msg, ex);
            errors.add(msg);
        }
        return this.getEmptySchema(kind);
    }

    @Override
    public IndexSchema getIndexerInputSchema(String kind, boolean invalidateCached) throws AppException, UnsupportedEncodingException, URISyntaxException {

        if (invalidateCached) {
            this.invalidateCache(kind);
        }

        String schema = (String) this.schemaCache.get(kind);
        if (Strings.isNullOrEmpty(schema)) {
            // get from storage
            schema = this.schemaProvider.getSchema(kind);
            if (Strings.isNullOrEmpty(schema)) {
                return this.getEmptySchema(kind);
            } else {
                // cache the schema
                this.schemaCache.put(kind, schema);
                // get flatten schema and cache it
                IndexSchema flatSchemaObj = normalizeSchema(schema);
                if (flatSchemaObj != null) {
                    this.schemaCache.put(kind + FLATTENED_SCHEMA, gson.toJson(flatSchemaObj));
                }
                return flatSchemaObj;
            }
        } else {
            // search flattened schema in memcache
            String flattenedSchema = (String) this.schemaCache.get(kind + FLATTENED_SCHEMA);
            if (Strings.isNullOrEmpty(flattenedSchema)) {
                return this.getEmptySchema(kind);
            }
            return this.gson.fromJson(flattenedSchema, IndexSchema.class);
        }
    }

    private IndexSchema getEmptySchema(String kind) {
        Schema basicSchema = Schema.builder().kind(kind).build();
        return normalizeSchema(gson.toJson(basicSchema));
    }

    public void syncIndexMappingWithStorageSchema(String kind) throws ElasticsearchException, IOException, AppException, URISyntaxException {
        String index = this.elasticIndexNameResolver.getIndexNameFromKind(kind);
        try (RestHighLevelClient restClient = this.elasticClientHandler.createRestClient()) {
            if (this.indicesService.isIndexExist(restClient, index)) {
                this.indicesService.deleteIndex(restClient, index);
                this.log.info(String.format("deleted index: %s", index));
            }
            IndexSchema schemaObj = this.getIndexerInputSchema(kind, true);
            this.indicesService.createIndex(restClient, index, null, schemaObj.getType(), this.mappingService.getIndexMappingFromRecordSchema(schemaObj));
        }
    }

    public boolean isStorageSchemaSyncRequired(String kind, boolean forceClean) throws IOException {
        String index = this.elasticIndexNameResolver.getIndexNameFromKind(kind);
        try (RestHighLevelClient restClient = this.elasticClientHandler.createRestClient()) {
            boolean indexExist = this.indicesService.isIndexExist(restClient, index);
            return !indexExist || forceClean;
        }
    }

    private void invalidateCache(String kind) {
        String schema = (String) this.schemaCache.get(kind);
        if (!Strings.isNullOrEmpty(schema)) this.schemaCache.delete(kind);

        String flattenSchema = (String) this.schemaCache.get(kind + FLATTENED_SCHEMA);
        if (!Strings.isNullOrEmpty(flattenSchema)) this.schemaCache.delete(kind + FLATTENED_SCHEMA);
    }

    private IndexSchema normalizeSchema(String schemaStr) throws AppException {

        try {
            Schema schemaObj = this.gson.fromJson(schemaStr, Schema.class);

            if (schemaObj == null) return null;

            Map<String, Object> data = new HashMap<>();
            Map<String, Object> meta = new HashMap<>();

            if (schemaObj.getSchema() != null && schemaObj.getSchema().length > 0) {
                for (SchemaItem schemaItem : schemaObj.getSchema()) {
                    String dataType = schemaItem.getKind();
                    Object elasticDataType = TypeMapper.getIndexerType(dataType, ElasticType.TEXT.getValue());
                    if (schemaItem.getProperties() != null) {
                        HashMap<String, Object> propertiesMap = normalizeInnerProperties(schemaItem);
                        elasticDataType = TypeMapper.getObjectsArrayMapping(dataType, propertiesMap);
                    }
                    data.put(schemaItem.getPath(), elasticDataType);
                }
            }

            String[] parts = schemaObj.getKind().split(":");
            String authority = parts[0];
            String source = parts[1];
            String type = parts[2];

            // mandatory attributes
            meta.put(RecordMetaAttribute.ID.getValue(), TypeMapper.getIndexerType(RecordMetaAttribute.ID));
            meta.put(RecordMetaAttribute.NAMESPACE.getValue(), TypeMapper.getIndexerType(RecordMetaAttribute.NAMESPACE));
            meta.put(RecordMetaAttribute.VERSION.getValue(), TypeMapper.getIndexerType(RecordMetaAttribute.VERSION));
            meta.put(RecordMetaAttribute.KIND.getValue(), TypeMapper.getIndexerType(RecordMetaAttribute.KIND));
            meta.put(RecordMetaAttribute.TYPE.getValue(), TypeMapper.getIndexerType(RecordMetaAttribute.TYPE));
            meta.put(RecordMetaAttribute.ACL.getValue(), TypeMapper.getIndexerType(RecordMetaAttribute.ACL));
            meta.put(RecordMetaAttribute.X_ACL.getValue(), TypeMapper.getIndexerType(RecordMetaAttribute.X_ACL));
            meta.put(RecordMetaAttribute.TAGS.getValue(), TypeMapper.getIndexerType(RecordMetaAttribute.TAGS));
            meta.put(RecordMetaAttribute.LEGAL.getValue(), TypeMapper.getIndexerType(RecordMetaAttribute.LEGAL));
            meta.put(RecordMetaAttribute.ANCESTRY.getValue(), TypeMapper.getIndexerType(RecordMetaAttribute.ANCESTRY));
            meta.put(RecordMetaAttribute.INDEX_STATUS.getValue(), TypeMapper.getIndexerType(RecordMetaAttribute.INDEX_STATUS));
            meta.put(RecordMetaAttribute.AUTHORITY.getValue(), TypeMapper.getConstantIndexerType(RecordMetaAttribute.AUTHORITY, authority));
            meta.put(RecordMetaAttribute.SOURCE.getValue(), TypeMapper.getConstantIndexerType(RecordMetaAttribute.SOURCE, source));
            meta.put(RecordMetaAttribute.CREATE_USER.getValue(), TypeMapper.getIndexerType(RecordMetaAttribute.CREATE_USER));
            meta.put(RecordMetaAttribute.CREATE_TIME.getValue(), TypeMapper.getIndexerType(RecordMetaAttribute.CREATE_TIME));
            meta.put(RecordMetaAttribute.MODIFY_USER.getValue(), TypeMapper.getIndexerType(RecordMetaAttribute.MODIFY_USER));
            meta.put(RecordMetaAttribute.MODIFY_TIME.getValue(), TypeMapper.getIndexerType(RecordMetaAttribute.MODIFY_TIME));

            return IndexSchema.builder().dataSchema(data).metaSchema(meta).kind(schemaObj.getKind()).type(type).build();

        } catch (Exception e) {
            throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Schema normalization error", "An error has occurred while normalizing the schema.", e);
        }
    }

    private HashMap<String, Object> normalizeInnerProperties(SchemaItem schemaItem) {
        HashMap<String, Object> propertiesMap = new HashMap<>();
        for (SchemaItem propertiesItem : schemaItem.getProperties()) {
            String propertiesItemKind = propertiesItem.getKind();
            Object propertiesElasticType = TypeMapper.getIndexerType(propertiesItemKind, ElasticType.TEXT.getValue());
            if (propertiesItem.getProperties() != null) {
                HashMap<String, Object> innerProperties = normalizeInnerProperties(propertiesItem);
                propertiesElasticType = TypeMapper.getObjectsArrayMapping(propertiesItemKind, innerProperties);
            }
            propertiesMap.put(propertiesItem.getPath(), propertiesElasticType);
        }
        return propertiesMap;
    }

}