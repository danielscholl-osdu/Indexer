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
import com.google.gson.GsonBuilder;
import org.apache.http.HttpStatus;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestStatus;
import org.opengroup.osdu.core.common.Constants;
import org.opengroup.osdu.core.common.feature.IFeatureFlag;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.entitlements.Acl;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.http.RequestStatus;
import org.opengroup.osdu.core.common.model.indexer.*;
import org.opengroup.osdu.core.common.model.indexer.RecordIndexerPayload.Record;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.core.common.model.search.RecordMetaAttribute;
import org.opengroup.osdu.core.common.provider.interfaces.IRequestInfo;
import org.opengroup.osdu.core.common.search.ElasticIndexNameResolver;
import org.opengroup.osdu.indexer.logging.AuditLogger;
import org.opengroup.osdu.indexer.model.BulkRequestResult;
import org.opengroup.osdu.indexer.model.indexproperty.AugmenterConfiguration;
import org.opengroup.osdu.indexer.provider.interfaces.IPublisher;
import org.opengroup.osdu.indexer.util.AugmenterSetting;
import org.opengroup.osdu.indexer.util.ElasticClientHandler;
import org.opengroup.osdu.indexer.util.IndexerQueueTaskBuilder;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.opengroup.osdu.indexer.config.IndexerConfigurationProperties.AS_INGESTED_COORDINATES_FEATURE_NAME;

@Service
@Primary
public class IndexerServiceImpl implements IndexerService {

    private static final TimeValue BULK_REQUEST_TIMEOUT = TimeValue.timeValueMinutes(1);

    private static final List<RestStatus> RETRY_ELASTIC_EXCEPTION = new ArrayList<>(Arrays.asList(RestStatus.TOO_MANY_REQUESTS, RestStatus.BAD_GATEWAY, RestStatus.SERVICE_UNAVAILABLE, RestStatus.FORBIDDEN));

    private static final String MAPPER_PARSING_EXCEPTION_TYPE = "type=mapper_parsing_exception";

    private final Gson gson = new GsonBuilder().serializeNulls().create();

    // we index a normalized kind (authority + source + entity type + major version) as a tags attribute for all records
    private static final String NORMALIZATION_KIND_TAG_ATTRIBUTE_NAME = "normalizedKind";
    private static final String VERTICAL_COORDINATE_REFERENCE_SYSTEM_ID = "VerticalCoordinateReferenceSystemID";

    @Inject
    private JaxRsDpsLog jaxRsDpsLog;
    @Inject
    private AuditLogger auditLogger;
    @Inject
    private StorageService storageService;
    @Inject
    private IndexSchemaService schemaService;
    @Inject
    private IndicesService indicesService;
    @Inject
    private IMappingService mappingService;
    @Inject
    private IPublisher progressPublisher;
    @Inject
    private ElasticClientHandler elasticClientHandler;
    @Inject
    private IndexerQueueTaskBuilder indexerQueueTaskBuilder;
    @Inject
    private ElasticIndexNameResolver elasticIndexNameResolver;
    @Inject
    private StorageIndexerPayloadMapper storageIndexerPayloadMapper;
    @Inject
    private IRequestInfo requestInfo;
    @Inject
    private JobStatus jobStatus;
    @Inject
    private AugmenterConfigurationService augmenterConfigurationService;
    @Inject
    private AugmenterSetting augmenterSetting;

    @Autowired
    private IFeatureFlag asIngestedCoordinatesFeatureFlag;

    private DpsHeaders headers;

    @Override
    public JobStatus processRecordChangedMessages(RecordChangedMessages message, List<RecordInfo> recordInfos) throws Exception {

        // this should not happen
        if (recordInfos.isEmpty()) return null;

        String errorMessage = "";
        List<String> retryRecordIds = new LinkedList<>();

        // get auth header with service account Authorization
        this.headers = this.requestInfo.getHeadersWithDwdAuthZ();

        // initialize status for all messages.
        this.jobStatus.initialize(recordInfos);

        try {
            auditLogger.indexStarted(recordInfos.stream()
                    .map(entry -> String.format("id=%s kind=%s operationType=%s", entry.getId(), entry.getKind(), entry.getOp()))
                    .collect(Collectors.toList()));

            // get upsert records
            Map<String, Map<String, OperationType>> upsertRecordMap = RecordInfo.getUpsertRecordIds(recordInfos);
            if (upsertRecordMap != null && !upsertRecordMap.isEmpty()) {
                List<String> upsertFailureRecordIds = processUpsertRecords(upsertRecordMap, recordInfos);
                retryRecordIds.addAll(upsertFailureRecordIds);
            }

            // get delete records
            Map<String, List<String>> deleteRecordMap = RecordInfo.getDeleteRecordIds(recordInfos);
            if (deleteRecordMap != null && !deleteRecordMap.isEmpty()) {
                List<String> deleteFailureRecordIds = processDeleteRecords(deleteRecordMap);
                retryRecordIds.addAll(deleteFailureRecordIds);
            }

            // process legacy storage schema change messages
            Map<String, OperationType> schemaMsgs = RecordInfo.getSchemaMsgs(recordInfos);
            if (schemaMsgs != null && !schemaMsgs.isEmpty()) {
                this.schemaService.processSchemaMessages(schemaMsgs);
            }

            // process failed records
            if (!retryRecordIds.isEmpty()) {
                retryAndEnqueueFailedRecords(recordInfos, retryRecordIds, message);
            }

            if (this.augmenterSetting.isEnabled()) {
                try {
                    Map<String, List<String>> upsertKindIds = getUpsertRecordIdsForConfigurationsEnabledKinds(upsertRecordMap, retryRecordIds);
                    Map<String, List<String>> deleteKindIds = getDeleteRecordIdsForConfigurationsEnabledKinds(deleteRecordMap, retryRecordIds);
                    if (!upsertKindIds.isEmpty() || !deleteKindIds.isEmpty()) {
                        augmenterConfigurationService.updateAssociatedRecords(message, upsertKindIds, deleteKindIds);
                    }
                }
                catch(Exception ex) {
                    jaxRsDpsLog.error("Augmenter: Failed to update associated records", ex);
                }
            }
        } catch (IOException e) {
            errorMessage = e.getMessage();
            throw new AppException(HttpStatus.SC_GATEWAY_TIMEOUT, "Internal communication failure", errorMessage, e);
        } catch (AppException e) {
            errorMessage = e.getMessage();
            throw e;
        } catch (Exception e) {
            errorMessage = "error indexing records";
            throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Unknown error", "An unknown error has occurred.", e);
        } finally {
            this.jobStatus.finalizeRecordStatus(errorMessage);
            this.updateAuditLog();
            this.progressPublisher.publishStatusChangedTagsToTopic(this.headers, this.jobStatus);
        }

        return jobStatus;
    }

    @Override
    public void processSchemaMessages(List<RecordInfo> recordInfos) throws IOException {
        Map<String, OperationType> schemaMsgs = RecordInfo.getSchemaMsgs(recordInfos);
        if (schemaMsgs != null && !schemaMsgs.isEmpty()) {
            try (RestHighLevelClient restClient = elasticClientHandler.createRestClient()) {
                schemaMsgs.entrySet().forEach(msg -> {
                    try {
                        processSchemaEvents(restClient, msg);
                    } catch (IOException | ElasticsearchStatusException e) {
                        throw new AppException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR.value(), "unable to process schema delete", e.getMessage());
                    }
                });
            }
        }
    }

    private Map<String, List<String>> getUpsertRecordIdsForConfigurationsEnabledKinds(Map<String, Map<String, OperationType>> upsertRecordMap, List<String> retryRecordIds) {
        Map<String, List<String>> upsertKindIds = new HashMap<>();
        for (Map.Entry<String, Map<String, OperationType>> entry : upsertRecordMap.entrySet()) {
            String kind = entry.getKey();
            if(augmenterConfigurationService.isConfigurationEnabled(kind)) {
                List<String> processedIds = entry.getValue().keySet().stream().filter(id -> !retryRecordIds.contains(id)).collect(Collectors.toList());
                if (!processedIds.isEmpty()) {
                    upsertKindIds.put(kind, processedIds);
                }
            }
        }
        return upsertKindIds;
    }

    private Map<String, List<String>> getDeleteRecordIdsForConfigurationsEnabledKinds(Map<String, List<String>> deleteRecordMap, List<String> retryRecordIds) {
        Map<String, List<String>> deletedRecordKindIdsMap = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : deleteRecordMap.entrySet()) {
            String kind = entry.getKey();
            if(augmenterConfigurationService.isConfigurationEnabled(kind)) {
                List<String> processedIds = entry.getValue().stream().filter(id -> !retryRecordIds.contains(id)).collect(Collectors.toList());
                if (!processedIds.isEmpty()) {
                    deletedRecordKindIdsMap.put(kind, processedIds);
                }
            }
        }
        return deletedRecordKindIdsMap;
    }

    private void processSchemaEvents(RestHighLevelClient restClient,
                                     Map.Entry<String, OperationType> msg) throws IOException, ElasticsearchStatusException {
        String kind = msg.getKey();
        String index = elasticIndexNameResolver.getIndexNameFromKind(kind);

        boolean indexExist = indicesService.isIndexExist(restClient, index);
        if (indexExist && msg.getValue() == OperationType.purge_schema) {
            indicesService.deleteIndex(restClient, index);
            schemaService.invalidateSchemaCache(kind);
        }
    }

    private List<String> processUpsertRecords(Map<String, Map<String, OperationType>> upsertRecordMap, List<RecordInfo> recordChangedInfos) throws Exception {
        // get schema for kind
        Map<String, IndexSchema> schemas = this.getSchema(upsertRecordMap);

        if (schemas.isEmpty()) return new LinkedList<>();

        // get recordIds with valid upsert index-status
        List<String> recordIds = this.jobStatus.getIdsByValidUpsertIndexingStatus();

        if (recordIds.isEmpty()) return new LinkedList<>();

        // get records via storage api
        Records storageRecords = this.storageService.getStorageRecords(recordIds, recordChangedInfos);
        List<String> failedOrRetryRecordIds = new LinkedList<>(storageRecords.getMissingRetryRecords());

        // map storage records to indexer payload
        RecordIndexerPayload recordIndexerPayload = this.getIndexerPayload(upsertRecordMap, schemas, storageRecords);

        jaxRsDpsLog.info(String.format("records change messages received : %s | valid storage bulk records: %s | valid index payload: %s", recordIds.size(), storageRecords.getRecords().size(), recordIndexerPayload.getRecords().size()));

        // index records
        failedOrRetryRecordIds.addAll(processElasticMappingAndUpsertRecords(recordIndexerPayload));

        return failedOrRetryRecordIds;
    }

    private Map<String, IndexSchema> getSchema(Map<String, Map<String, OperationType>> upsertRecordMap) {

        Map<String, IndexSchema> schemas = new HashMap<>();

        try {
            for (Map.Entry<String, Map<String, OperationType>> entry : upsertRecordMap.entrySet()) {

                String kind = entry.getKey();
                List<String> errors = new ArrayList<>();
                IndexSchema schemaObj = this.schemaService.getIndexerInputSchema(kind, errors);
                if (!errors.isEmpty()) {
                    this.jobStatus.addOrUpdateRecordStatus(entry.getValue().keySet(), IndexingStatus.WARN, HttpStatus.SC_BAD_REQUEST, String.join("|", errors), String.format("error  | kind: %s", kind));
                } else if (schemaObj.isDataSchemaMissing()) {
                    this.jobStatus.addOrUpdateRecordStatus(entry.getValue().keySet(), IndexingStatus.WARN, HttpStatus.SC_NOT_FOUND, "schema not found", String.format("schema not found | kind: %s", kind));
                }

                schemas.put(kind, schemaObj);
            }
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Get schema error", "An error has occurred while getting schema", e);
        }

        return schemas;
    }

    private RecordIndexerPayload getIndexerPayload(Map<String, Map<String, OperationType>> upsertRecordMap, Map<String, IndexSchema> kindSchemaMap, Records records) {
        List<Records.Entity> storageValidRecords = records.getRecords();
        List<RecordIndexerPayload.Record> indexerPayload = new ArrayList<>();
        List<IndexSchema> schemas = new ArrayList<>();

        for (Records.Entity storageRecord : storageValidRecords) {

            Map<String, OperationType> idOperationMap = upsertRecordMap.get(storageRecord.getKind());

            // skip if storage returned record with same id but different kind
            if (idOperationMap == null) {
                String message = String.format("storage service returned incorrect record | requested kind: %s | received kind: %s", this.jobStatus.getRecordKindById(storageRecord.getId()), storageRecord.getKind());
                this.jobStatus.addOrUpdateRecordStatus(storageRecord.getId(), IndexingStatus.SKIP, RequestStatus.STORAGE_CONFLICT, message, String.format("%s | record-id: %s", message, storageRecord.getId()));
                continue;
            }

            IndexSchema schema = kindSchemaMap.get(storageRecord.getKind());

            ArrayList<String> asIngestedCoordinatesPaths = null;
            if (this.asIngestedCoordinatesFeatureFlag.isFeatureEnabled(AS_INGESTED_COORDINATES_FEATURE_NAME)) {
                // enrich schema with AsIngestedCoordinates properties
                asIngestedCoordinatesPaths = findAsIngestedCoordinatesPaths(storageRecord.getData(), "");
                addAsIngestedCoordinatesFieldsToSchema(schema, asIngestedCoordinatesPaths);
            }
            schemas.add(schema);

            // skip indexing of records if data block is empty
            RecordIndexerPayload.Record document = prepareIndexerPayload(schema, storageRecord, idOperationMap, asIngestedCoordinatesPaths);
            if (document != null) {
                indexerPayload.add(document);
            }
        }

        // this should only happen if storage service returned WRONG records with kind for all the records in the messages
        if (indexerPayload.isEmpty()) {
            throw new AppException(RequestStatus.STORAGE_CONFLICT, "Indexer error", "upsert record failed, storage service returned incorrect records");
        }

        return RecordIndexerPayload.builder().records(indexerPayload).schemas(schemas).build();
    }

    private ArrayList<String> findAsIngestedCoordinatesPaths(Map<String, Object> dataMap, String path) {
        ArrayList<String> paths = new ArrayList<>();
        for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
            if (entry.getKey().equals(Constants.AS_INGESTED_COORDINATES)) {
                paths.add(path + entry.getKey());
                break;
            }
            if (entry.getValue() instanceof Map nested) {
                paths.addAll(findAsIngestedCoordinatesPaths(nested, path + entry.getKey() + "."));
            }
        }
        return paths;
    }

    private void addAsIngestedCoordinatesFieldsToSchema(IndexSchema schemaObj, ArrayList<String> asIngestedCoordinatesPaths) {
        Map<String, String> asIngestedProperties = new HashMap<>();

        Map<String, String> propertyKeyTypeMap = new HashMap<>();
        propertyKeyTypeMap.put("FirstPoint.X", "long");
        propertyKeyTypeMap.put("FirstPoint.Y", "long");
        propertyKeyTypeMap.put("FirstPoint.Z", "long");
        propertyKeyTypeMap.put(Constants.COORDINATE_REFERENCE_SYSTEM_ID, "text");
        propertyKeyTypeMap.put(VERTICAL_COORDINATE_REFERENCE_SYSTEM_ID, "text");
        propertyKeyTypeMap.put(Constants.VERTICAL_UNIT_ID, "text");

        for(String path: asIngestedCoordinatesPaths){
            for(Map.Entry<String,String> propertyKeyTypeEntry: propertyKeyTypeMap.entrySet()) {
                String pathPropertyKey = path + "." + propertyKeyTypeEntry.getKey();
                String propertyValue = propertyKeyTypeEntry.getValue();
                asIngestedProperties.put(pathPropertyKey, propertyValue);
            }
        }
        schemaObj.getDataSchema().putAll(asIngestedProperties);
    }

    private RecordIndexerPayload.Record prepareIndexerPayload(IndexSchema schemaObj, Records.Entity storageRecord, Map<String, OperationType> idToOperationMap, ArrayList<String> asIngestedCoordinatesPaths) {

        RecordIndexerPayload.Record document = null;
        
        try {
            Map<String, Object> storageRecordData = storageRecord.getData();
            document = new RecordIndexerPayload.Record();
            if (storageRecordData == null || storageRecordData.isEmpty()) {
                String message = "empty or null data block found in the storage record";
                this.jobStatus.addOrUpdateRecordStatus(storageRecord.getId(), IndexingStatus.WARN, HttpStatus.SC_NOT_FOUND, message, String.format("record-id: %s | %s", storageRecord.getId(), message));
            } else if (schemaObj.isDataSchemaMissing()) {
                document.setSchemaMissing(true);
            } else {
                Map<String, Object> dataMap = this.storageIndexerPayloadMapper.mapDataPayload(asIngestedCoordinatesPaths, schemaObj, storageRecordData, storageRecord.getId());
                if (dataMap.isEmpty()) {
                    document.setMappingMismatch(true);
                    String message = String.format("complete schema mismatch: none of the data attribute can be mapped | data: %s", storageRecordData);
                    this.jobStatus.addOrUpdateRecordStatus(storageRecord.getId(), IndexingStatus.WARN, HttpStatus.SC_NOT_FOUND, message, String.format("record-id: %s | %s", storageRecord.getId(), message));
                }

                if(this.augmenterSetting.isEnabled()) {
                    try {
                        if (augmenterConfigurationService.isConfigurationEnabled(storageRecord.getKind())) {
                            AugmenterConfiguration augmenterConfiguration = augmenterConfigurationService.getConfiguration(storageRecord.getKind());
                            if (augmenterConfiguration != null) {
                                // Merge extended properties
                                dataMap = mergeDataFromPropertyConfiguration(storageRecord.getId(), dataMap, augmenterConfiguration);
                            }
                            // We cache the dataMap in case the update of this object will trigger update of the related objects.
                            augmenterConfigurationService.cacheDataRecord(storageRecord.getId(), storageRecord.getKind(), dataMap);
                        }
                    }
                    catch(Exception ex) {
                        jaxRsDpsLog.error(String.format("Augmenter: Failed to merge extended properties of the record with id: '%s' and kind: '%s'", storageRecord.getId(), storageRecord.getKind()), ex);
                    }
                }

                document.setData(dataMap);
            }
        } catch (AppException e) {
            this.jobStatus.addOrUpdateRecordStatus(storageRecord.getId(), IndexingStatus.FAIL, HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            jaxRsDpsLog.warning(String.format("record-id: %s | %s", storageRecord.getId(), e.getMessage()), e);
        } catch (Exception e) {
            this.jobStatus.addOrUpdateRecordStatus(storageRecord.getId(), IndexingStatus.FAIL, HttpStatus.SC_INTERNAL_SERVER_ERROR, String.format("error parsing records against schema, error-message: %s", e.getMessage()));
            jaxRsDpsLog.error(String.format("record-id: %s | error parsing records against schema, error-message: %s", storageRecord.getId(), e.getMessage()), e);
        }

        try {
            // index individual parts of kind
            String[] kindParts = storageRecord.getKind().split(":");
            String authority = kindParts[0];
            String source = kindParts[1];
            String type = kindParts[2];
            String[] versionParts = kindParts[3].split("\\.");
            document.setKind(storageRecord.getKind());
            document.setNamespace(authority + ":" + source);
            document.setAuthority(authority);
            document.setSource(source);
            document.setType(type);
            document.setId(storageRecord.getId());
            document.setVersion(storageRecord.getVersion());
            document.setAcl(storageRecord.getAcl());
            document.setLegal(storageRecord.getLegal());
            if (storageRecord.getTags() == null) {
                storageRecord.setTags(new HashMap<>());
            }
            storageRecord.getTags().put(IndexerServiceImpl.NORMALIZATION_KIND_TAG_ATTRIBUTE_NAME, String.format("%s:%s:%s:%s", authority, source, type, versionParts[0]));
            document.setTags(storageRecord.getTags());
            document.setCreateUser(storageRecord.getCreateUser());
            document.setCreateTime(storageRecord.getCreateTime());
            if (!Strings.isNullOrEmpty(storageRecord.getModifyUser())) {
                document.setModifyUser(storageRecord.getModifyUser());
            }
            if (!Strings.isNullOrEmpty(storageRecord.getModifyTime())) {
                document.setModifyTime(storageRecord.getModifyTime());
            }
            RecordStatus recordStatus = this.jobStatus.getJobStatusByRecordId(storageRecord.getId());
            if (recordStatus.getIndexProgress().getStatusCode() == 0) {
                recordStatus.getIndexProgress().setStatusCode(HttpStatus.SC_OK);
            }
            document.setIndexProgress(recordStatus.getIndexProgress());
            if (storageRecord.getAncestry() != null) document.setAncestry(storageRecord.getAncestry());
            document.setOperationType(idToOperationMap.get(storageRecord.getId()));
        } catch (Exception e) {
            this.jobStatus.addOrUpdateRecordStatus(storageRecord.getId(), IndexingStatus.FAIL, HttpStatus.SC_INTERNAL_SERVER_ERROR, String.format("error parsing meta data, error-message: %s", e.getMessage()));
            jaxRsDpsLog.error(String.format("record-id: %s | error parsing meta data, error-message: %s", storageRecord.getId(), e.getMessage()), e);
        }
        return document;
    }

    private Map<String, Object> mergeDataFromPropertyConfiguration(String objectId, Map<String, Object> originalDataMap, AugmenterConfiguration augmenterConfiguration) {
        Map<String, Object> extendedDataMap = augmenterConfigurationService.getExtendedProperties(objectId, originalDataMap, augmenterConfiguration);
        if (!extendedDataMap.isEmpty()) {
            originalDataMap.putAll(extendedDataMap);
        }

        return originalDataMap;
    }

    private List<String> processElasticMappingAndUpsertRecords(RecordIndexerPayload recordIndexerPayload) throws Exception {

        try (RestHighLevelClient restClient = this.elasticClientHandler.createRestClient()) {
            List<IndexSchema> schemas = recordIndexerPayload.getSchemas();
            if (schemas == null || schemas.isEmpty()) {
                return new LinkedList<>();
            }

            // process the schema
            this.cacheOrCreateElasticMapping(schemas, restClient);

            // process the records
            List<RecordIndexerPayload.Record> records = recordIndexerPayload.getRecords();
            BulkRequestResult bulkRequestResult = this.upsertRecords(records, restClient);
            List<String> failedRecordIds = bulkRequestResult.getFailureRecordIds();

            processRetryUpsertRecords(restClient, records, bulkRequestResult, failedRecordIds);

            return failedRecordIds;
        }
    }

    private void processRetryUpsertRecords(RestHighLevelClient restClient, List<Record> records,
        BulkRequestResult bulkRequestResult, List<String> failedRecordIds) {
        List<String> retryUpsertRecordIds = bulkRequestResult.getRetryUpsertRecordIds();
        if (!retryUpsertRecordIds.isEmpty()) {
            List<Record> retryUpsertRecords = records.stream()
                .filter(record -> retryUpsertRecordIds.contains(record.getId()))
                .collect(Collectors.toList());
            retryUpsertRecords.forEach(record -> {
                record.setData(Collections.emptyMap());
                record.setTags(Collections.emptyMap());
            });
            bulkRequestResult = upsertRecords(retryUpsertRecords, restClient);
            failedRecordIds.addAll(bulkRequestResult.getFailureRecordIds());
        }
    }

    private void cacheOrCreateElasticMapping(List<IndexSchema> schemas, RestHighLevelClient restClient) throws Exception {

        for (IndexSchema schema : schemas) {
            String index = this.elasticIndexNameResolver.getIndexNameFromKind(schema.getKind());

            // check if index exist and sync meta attribute schema if required
            if (this.indicesService.isIndexReady(restClient, index)) {
                this.mappingService.syncIndexMappingIfRequired(restClient, schema);
                continue;
            }

            // create index
            Map<String, Object> mapping = this.mappingService.getIndexMappingFromRecordSchema(schema);
            if (!this.indicesService.createIndex(restClient, index, null, schema.getType(), mapping)) {
                throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Elastic error", "Error creating index.", String.format("Failed to get confirmation from elastic server for index: %s", index));
            }
        }
    }

    private BulkRequestResult upsertRecords(List<RecordIndexerPayload.Record> records, RestHighLevelClient restClient) throws AppException {
        if (records == null || records.isEmpty()) return new BulkRequestResult(Collections.emptyList(), Collections.emptyList());

        BulkRequest bulkRequest = new BulkRequest();
        bulkRequest.timeout(BULK_REQUEST_TIMEOUT);

        for (RecordIndexerPayload.Record record : records) {
            if ((record.getData() == null || record.getData().isEmpty()) && !record.skippedDataIndexing()) {
                // it will come here when schema is missing
                // TODO: rollback once we know what is causing the problem
                jaxRsDpsLog.warning(String.format("data not found for record: %s", record));
            }

            Map<String, Object> sourceMap = getSourceMap(record);
            String index = this.elasticIndexNameResolver.getIndexNameFromKind(record.getKind());
            IndexRequest indexRequest = new IndexRequest(index).id(record.getId()).source(this.gson.toJson(sourceMap), XContentType.JSON);
            bulkRequest.add(indexRequest);
        }

        return processBulkRequest(restClient, bulkRequest);
    }

    private List<String> processDeleteRecords(Map<String, List<String>> deleteRecordMap) throws Exception {
        BulkRequest bulkRequest = new BulkRequest();
        bulkRequest.timeout(BULK_REQUEST_TIMEOUT);

        for (Map.Entry<String, List<String>> record : deleteRecordMap.entrySet()) {

            String index = this.elasticIndexNameResolver.getIndexNameFromKind(record.getKey());

            for (String id : record.getValue()) {
                DeleteRequest deleteRequest = new DeleteRequest(index, id);
                bulkRequest.add(deleteRequest);
            }
        }

        try (RestHighLevelClient restClient = this.elasticClientHandler.createRestClient()) {
            return processBulkRequest(restClient, bulkRequest).getFailureRecordIds();
        }
    }

    private BulkRequestResult processBulkRequest(RestHighLevelClient restClient, BulkRequest bulkRequest) throws AppException {

        if (bulkRequest.numberOfActions() == 0) return new BulkRequestResult(Collections.emptyList(), Collections.emptyList());
        List<String> failureRecordIds = new LinkedList<>();
        List<String> retryUpsertRecordIds = new LinkedList<>();
        int failedRequestStatus = 500;
        Exception failedRequestCause = null;

        try {
            long startTime = System.currentTimeMillis();
            BulkResponse bulkResponse = restClient.bulk(bulkRequest, RequestOptions.DEFAULT);
            long stopTime = System.currentTimeMillis();

            // log failed bulk requests
            ArrayList<String> bulkFailures = new ArrayList<>();
            int succeededResponses = 0;
            int failedResponses = 0;

            for (BulkItemResponse bulkItemResponse : bulkResponse.getItems()) {
                if (bulkItemResponse.isFailed()) {
                    BulkItemResponse.Failure failure = bulkItemResponse.getFailure();
                    bulkFailures.add(String.format("elasticsearch bulk service status: %s | id: %s | message: %s", failure.getStatus(), failure.getId(), failure.getMessage()));
                    this.jobStatus.addOrUpdateRecordStatus(bulkItemResponse.getId(), IndexingStatus.FAIL, failure.getStatus().getStatus(), bulkItemResponse.getFailureMessage());

                    if (RestStatus.BAD_REQUEST.equals(failure.getStatus()) && failure.getCause() != null && failure.getCause().getMessage().contains(MAPPER_PARSING_EXCEPTION_TYPE)) {
                        retryUpsertRecordIds.add(bulkItemResponse.getId());
                    } else if (canIndexerRetry(bulkItemResponse)) {
                        failureRecordIds.add(bulkItemResponse.getId());

                        if (failedRequestCause == null) {
                            failedRequestCause = failure.getCause();
                            failedRequestStatus = failure.getStatus().getStatus();
                        }
                    }

                    failedResponses++;
                } else {
                    succeededResponses++;
                    this.jobStatus.addOrUpdateRecordStatus(bulkItemResponse.getId(), IndexingStatus.SUCCESS, HttpStatus.SC_OK, "Indexed Successfully");
                }
            }
            if (!bulkFailures.isEmpty()) this.jaxRsDpsLog.warning(bulkFailures);

            jaxRsDpsLog.info(String.format("records in elasticsearch service bulk request: %s | successful: %s | failed: %s | time taken for bulk request: %d milliseconds", bulkRequest.numberOfActions(), succeededResponses, failedResponses, stopTime-startTime));

            // retry entire message if all records are failing
            if (bulkRequest.numberOfActions() == failureRecordIds.size()) throw new AppException(failedRequestStatus,  "Elastic error", failedRequestCause.getMessage(), failedRequestCause);
        } catch (IOException e) {
            // throw explicit 504 for IOException
            throw new AppException(HttpStatus.SC_GATEWAY_TIMEOUT, "Elastic error", "Request cannot be completed in specified time.", e);
        } catch (ElasticsearchStatusException e) {
            throw new AppException(e.status().getStatus(), "Elastic error", e.getMessage(), e);
        } catch (Exception e) {
            if (e instanceof AppException) {
                throw e;
            }
            throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Elastic error", "Error indexing records.", e);
        }
        return new BulkRequestResult(failureRecordIds, retryUpsertRecordIds);
    }

    private Map<String, Object> getSourceMap(RecordIndexerPayload.Record record) {

        Map<String, Object> indexerPayload = new HashMap<>();

        // get the key and get the corresponding object from the individualRecord object
        if (record.getData() != null) {
            Map<String, Object> data = new HashMap<>();
            for (Map.Entry<String, Object> entry : record.getData().entrySet()) {
                data.put(entry.getKey(), entry.getValue());
            }
            indexerPayload.put(Constants.DATA, data);
        }

        indexerPayload.put(RecordMetaAttribute.ID.getValue(), record.getId());
        indexerPayload.put(RecordMetaAttribute.KIND.getValue(), record.getKind());
        indexerPayload.put(RecordMetaAttribute.AUTHORITY.getValue(), record.getAuthority());
        indexerPayload.put(RecordMetaAttribute.SOURCE.getValue(), record.getSource());
        indexerPayload.put(RecordMetaAttribute.NAMESPACE.getValue(), record.getNamespace());
        indexerPayload.put(RecordMetaAttribute.TYPE.getValue(), record.getType());
        indexerPayload.put(RecordMetaAttribute.VERSION.getValue(), record.getVersion());
        indexerPayload.put(RecordMetaAttribute.ACL.getValue(), record.getAcl());
        indexerPayload.put(RecordMetaAttribute.TAGS.getValue(), record.getTags());
        indexerPayload.put(RecordMetaAttribute.X_ACL.getValue(), Acl.flattenAcl(record.getAcl()));
        indexerPayload.put(RecordMetaAttribute.LEGAL.getValue(), record.getLegal());
        indexerPayload.put(RecordMetaAttribute.INDEX_STATUS.getValue(), record.getIndexProgress());
        if (record.getAncestry() != null) {
            indexerPayload.put(RecordMetaAttribute.ANCESTRY.getValue(), record.getAncestry());
        }
        indexerPayload.put(RecordMetaAttribute.CREATE_USER.getValue(), record.getCreateUser());
        indexerPayload.put(RecordMetaAttribute.CREATE_TIME.getValue(), record.getCreateTime());
        if (!Strings.isNullOrEmpty(record.getModifyUser())) {
            indexerPayload.put(RecordMetaAttribute.MODIFY_USER.getValue(), record.getModifyUser());
        }
        if (!Strings.isNullOrEmpty(record.getModifyTime())) {
            indexerPayload.put(RecordMetaAttribute.MODIFY_TIME.getValue(), record.getModifyTime());
        }
        return indexerPayload;
    }

    private boolean canIndexerRetry(BulkItemResponse bulkItemResponse) {
        if (RETRY_ELASTIC_EXCEPTION.contains(bulkItemResponse.status())) return true;

        if ((bulkItemResponse.getOpType() == DocWriteRequest.OpType.CREATE || bulkItemResponse.getOpType() == DocWriteRequest.OpType.UPDATE)
                && bulkItemResponse.status() == RestStatus.NOT_FOUND) {
            return true;
        }

        return false;
    }

    private void retryAndEnqueueFailedRecords(List<RecordInfo> recordInfos, List<String> failuresRecordIds, RecordChangedMessages message) throws IOException {

        jaxRsDpsLog.info(String.format("queuing bulk failed records back to task-queue for retry | count: %s | records: %s", failuresRecordIds.size(), failuresRecordIds));
        List<RecordInfo> retryRecordInfos = new LinkedList<>();
        for (String recordId : failuresRecordIds) {
            for (RecordInfo origMessage : recordInfos) {
                if (origMessage.getId().equalsIgnoreCase(recordId)) {
                    retryRecordInfos.add(origMessage);
                }
            }
        }

        RecordChangedMessages newMessage = RecordChangedMessages.builder()
                .messageId(message.getMessageId())
                .publishTime(message.getPublishTime())
                .data(this.gson.toJson(retryRecordInfos))
                .attributes(message.getAttributes()).build();

        String payLoad = this.gson.toJson(newMessage);
        this.indexerQueueTaskBuilder.createWorkerTask(payLoad, this.headers);
    }

    private void updateAuditLog() {
        logAuditEvents(OperationType.create, this.auditLogger::indexCreateRecordSuccess, this.auditLogger::indexCreateRecordFail);
        logAuditEvents(OperationType.update, this.auditLogger::indexUpdateRecordSuccess, this.auditLogger::indexUpdateRecordFail);
        logAuditEvents(OperationType.purge, this.auditLogger::indexPurgeRecordSuccess, this.auditLogger::indexPurgeRecordFail);
        logAuditEvents(OperationType.delete, this.auditLogger::indexDeleteRecordSuccess, this.auditLogger::indexDeleteRecordFail);
    }

    private void logAuditEvents(OperationType operationType, Consumer<List<String>> successEvent, Consumer<List<String>> failedEvent) {
        List<RecordStatus> succeededRecords = this.jobStatus.getRecordStatuses(IndexingStatus.SUCCESS, operationType);
        if (!succeededRecords.isEmpty()) {
            successEvent.accept(succeededRecords.stream().map(RecordStatus::succeededAuditLogMessage).collect(Collectors.toList()));
        }
        List<RecordStatus> skippedRecords = this.jobStatus.getRecordStatuses(IndexingStatus.SKIP, operationType);
        List<RecordStatus> failedRecords = this.jobStatus.getRecordStatuses(IndexingStatus.FAIL, operationType);
        failedRecords.addAll(skippedRecords);
        if (!failedRecords.isEmpty()) {
            failedEvent.accept(failedRecords.stream().map(RecordStatus::failedAuditLogMessage).collect(Collectors.toList()));
        }
    }
}
