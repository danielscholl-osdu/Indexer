/*
 * Copyright © Schlumberger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.indexer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import org.apache.commons.collections.CollectionUtils;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.indexer.OperationType;
import org.opengroup.osdu.core.common.model.indexer.RecordInfo;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.core.common.model.search.SortOrder;
import org.opengroup.osdu.core.common.model.search.SortQuery;
import org.opengroup.osdu.core.common.model.storage.RecordData;
import org.opengroup.osdu.core.common.model.storage.Schema;
import org.opengroup.osdu.core.common.model.storage.SchemaItem;
import org.opengroup.osdu.core.common.provider.interfaces.IRequestInfo;
import org.opengroup.osdu.indexer.cache.partitionsafe.*;
import org.opengroup.osdu.indexer.config.IndexerConfigurationProperties;
import org.opengroup.osdu.indexer.model.*;
import org.opengroup.osdu.indexer.model.indexproperty.*;
import org.opengroup.osdu.indexer.util.IndexerQueueTaskBuilder;
import org.opengroup.osdu.indexer.util.PropertyUtil;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.net.URISyntaxException;
import java.util.*;


@Component
public class PropertyConfigurationsServiceImpl implements PropertyConfigurationsService {
    private static final String ASSOCIATED_IDENTITIES_PROPERTY = "AssociatedIdentities";
    private static final String VERSION_PROPERTY = "version";
    private static final String ASSOCIATED_IDENTITIES_PROPERTY_STORAGE_FORMAT_TYPE = "[]string";
    private static final String INDEX_PROPERTY_PATH_CONFIGURATION_KIND = "osdu:wks:reference-data--IndexPropertyPathConfiguration:*";
    private static final String ANCESTRY_KINDS_DELIMITER = ",";
    private static final String PARENT_CHILDREN_CONFIGURATION_QUERY_FORMAT =
            "nested(data.Configurations, nested(data.Configurations.Paths, (RelatedObjectsSpec.RelationshipDirection: ParentToChildren AND RelatedObjectsSpec.RelatedObjectKind:\"%s\")))";
    private static final String CHILDREN_PARENT_CONFIGURATION_QUERY_FORMAT =
            "nested(data.Configurations, nested(data.Configurations.Paths, (RelatedObjectsSpec.RelationshipDirection: ChildToParent AND RelatedObjectsSpec.RelatedObjectKind:\"%s\")))";
    private static final String HAS_CONFIGURATIONS_QUERY_FORMAT =  "data.Code: \"%s\" OR nested(data.Configurations, nested(data.Configurations.Paths, (RelatedObjectsSpec.RelatedObjectKind:\"%s\")))";
    private static final int MAX_SEARCH_LIMIT = 1000;

    private static final String PROPERTY_DELIMITER = ".";
    private static final String NESTED_OBJECT_DELIMITER = "[].";
    private static final String ARRAY_SYMBOL = "[]";
    private static final String SCHEMA_NESTED_KIND = "nested";

    private static final String STRING_KIND = "string";

    private static final String STRING_ARRAY_KIND = "[]string";

    private static final PropertyConfigurations EMPTY_CONFIGURATIONS = new PropertyConfigurations();
    private static final String SEARCH_GENERAL_ERROR = "Failed to call search service.";

    private final Gson gson = new Gson();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    private IndexerConfigurationProperties configurationProperties;
    @Inject
    private PropertyConfigurationsCache propertyConfigurationCache;
    @Inject
    private ConfigurationsEnabledCache propertyConfigurationsEnabledCache;
    @Inject
    private ChildRelationshipSpecsCache parentChildRelationshipSpecsCache;
    @Inject
    private ChildrenKindsCache childrenKindsCache;
    @Inject
    private KindCache kindCache;
    @Inject
    private RelatedObjectCache relatedObjectCache;
    @Inject
    private RecordChangeInfoCache recordChangeInfoCache;
    @Inject
    private SearchService searchService;
    @Inject
    private SchemaService schemaService;
    @Inject
    private IndexerQueueTaskBuilder indexerQueueTaskBuilder;
    @Inject
    private IRequestInfo requestInfo;
    @Inject
    private JaxRsDpsLog jaxRsDpsLog;

    @Override
    public boolean isPropertyConfigurationsEnabled(String kind) {
        kind = PropertyUtil.getKindWithMajor(kind);
        if (Strings.isNullOrEmpty(kind))
            return false;

        Boolean enabled = propertyConfigurationsEnabledCache.get(kind);
        if(enabled == null) {
            SearchRequest searchRequest = new SearchRequest();
            searchRequest.setKind(INDEX_PROPERTY_PATH_CONFIGURATION_KIND);
            String query = String.format(HAS_CONFIGURATIONS_QUERY_FORMAT, kind, kind);
            searchRequest.setQuery(query);
            if(searchFirstRecord(searchRequest) != null) {
                enabled = true;
            }
            else {
                enabled = false;
            }
            propertyConfigurationsEnabledCache.put(kind, enabled);
        }

        return enabled;
    }

    @Override
    public PropertyConfigurations getPropertyConfigurations(String kind) {
        kind = PropertyUtil.getKindWithMajor(kind);
        if (Strings.isNullOrEmpty(kind))
            return null;

        PropertyConfigurations configuration = propertyConfigurationCache.get(kind);
        if (configuration == null) {
            configuration = searchConfigurations(kind);
            if (configuration != null) {
                if(configuration.isValid()) {
                    // Log for debug
                    if(configuration.hasInvalidConfigurations()) {
                        String msg = String.format("PropertyConfigurations: it has invalid PropertyConfiguration for configurations with name '%s':", configuration.getName());
                        this.jaxRsDpsLog.warning(msg);
                    }
                }
                else {
                    // Log for debug
                    StringBuilder msgBuilder = new StringBuilder();
                    msgBuilder.append(String.format("PropertyConfigurations: it is invalid for configurations with name '%s':", configuration.getName()));
                    if(!configuration.hasValidCode()) {
                        msgBuilder.append(System.lineSeparator());
                        msgBuilder.append(String.format("The code '%s' is invalid. It should be a valid kind with major version ended with '.'", configuration.getCode()));
                    }
                    if(!configuration.hasValidConfigurations()) {
                        msgBuilder.append(System.lineSeparator());
                        msgBuilder.append("It does not have any valid PropertyConfiguration");
                    }
                    this.jaxRsDpsLog.warning(msgBuilder.toString());

                    configuration = EMPTY_CONFIGURATIONS; // reset
                }

                propertyConfigurationCache.put(kind, configuration);
            } else {
                // It is common that a kind does not have extended property. So we need to cache an empty configuration
                // to avoid unnecessary search
                propertyConfigurationCache.put(kind, EMPTY_CONFIGURATIONS);
            }
        }

        if (!isNullOrEmptyConfigurations(configuration)) {
            return configuration;
        }

        return null;
    }

    @Override
    public Map<String, Object> getExtendedProperties(String objectId, Map<String, Object> originalDataMap, PropertyConfigurations propertyConfigurations) {
        // Get all data maps of the related objects in one query in order to improve the performance.
        Map<String, Map<String, Object>> idObjectDataMap = getRelatedObjectsData(originalDataMap, propertyConfigurations);

        Set<String> associatedIdentities = new HashSet<>();
        Map<String, Object> extendedDataMap = new HashMap<>();
        for (PropertyConfiguration configuration : propertyConfigurations.getConfigurations().stream().filter(c -> c.isValid()).toList()) {
            String extendedPropertyName = configuration.getExtendedPropertyName();
            if (originalDataMap.containsKey(extendedPropertyName) && originalDataMap.get(extendedPropertyName) != null) {
                // If the original record already has the property, then we should not override.
                // For example, if the trajectory record already SpatialLocation value, then it should not be overridden by the SpatialLocation of the well bore.
                continue;
            }

            Map<String, Object> allPropertyValues = new HashMap<>();
            for (PropertyPath path : configuration.getPaths().stream().filter(p -> p.hasValidValueExtraction()).toList()) {
                if (path.hasValidRelatedObjectsSpec()) {
                    RelatedObjectsSpec relatedObjectsSpec = path.getRelatedObjectsSpec();
                    if (relatedObjectsSpec.isChildToParent()) {
                        List<String> relatedObjectIds = getRelatedObjectIds(originalDataMap, relatedObjectsSpec);
                        for (String relatedObjectId : relatedObjectIds) {
                            // Store all ids
                            associatedIdentities.add(PropertyUtil.removeIdPostfix(relatedObjectId));
                        }

                        for (String relatedObjectId : relatedObjectIds) {
                            String id = PropertyUtil.removeIdPostfix(relatedObjectId);
                            Map<String, Object> relatedObject = idObjectDataMap.getOrDefault(id, new HashMap<>());
                            Map<String, Object> propertyValues = getExtendedPropertyValues(extendedPropertyName, relatedObject, path.getValueExtraction(), configuration.isExtractFirstMatch());
                            if (allPropertyValues.isEmpty() && configuration.isExtractFirstMatch()) {
                                allPropertyValues = propertyValues;
                                break;
                            } else {
                                allPropertyValues = PropertyUtil.combineObjectMap(allPropertyValues, propertyValues);
                            }
                        }
                    } else {
                        List<SearchRecord> childrenRecords = searchChildrenRecords(relatedObjectsSpec.getRelatedObjectKind(), relatedObjectsSpec.getRelatedObjectID(), objectId);
                        for (SearchRecord searchRecord : childrenRecords) {
                            // If the child record is in the cache, that means the searchRecord was updated very recently.
                            // In this case, use the cache's record instead of the searchRecord from search result
                            RecordData cachedRecordData = this.relatedObjectCache.get(searchRecord.getId());
                            Map<String, Object> childDataMap = (cachedRecordData != null)? cachedRecordData.getData() : searchRecord.getData();
                            Map<String, Object> propertyValues = getExtendedPropertyValues(extendedPropertyName, childDataMap, path.getValueExtraction(), configuration.isExtractFirstMatch());
                            if (allPropertyValues.isEmpty() && configuration.isExtractFirstMatch()) {
                                allPropertyValues = propertyValues;
                                break;
                            } else {
                                allPropertyValues = PropertyUtil.combineObjectMap(allPropertyValues, propertyValues);
                            }
                        }
                    }
                } else {
                    Map<String, Object> propertyValues = getExtendedPropertyValues(extendedPropertyName, originalDataMap, path.getValueExtraction(), configuration.isExtractFirstMatch());
                    if (allPropertyValues.isEmpty() && configuration.isExtractFirstMatch()) {
                        allPropertyValues = propertyValues;
                    } else {
                        allPropertyValues = PropertyUtil.combineObjectMap(allPropertyValues, propertyValues);
                    }
                }

                if (!allPropertyValues.isEmpty() && configuration.isExtractFirstMatch())
                    break;
            }

            extendedDataMap.putAll(allPropertyValues);
        }
        if (!associatedIdentities.isEmpty()) {
            extendedDataMap.put(ASSOCIATED_IDENTITIES_PROPERTY, Arrays.asList(associatedIdentities.toArray()));
        }

        return extendedDataMap;
    }

    @Override
    public List<SchemaItem> getExtendedSchemaItems(Schema originalSchema, Map<String, Schema> relatedObjectKindSchemas, PropertyConfigurations propertyConfigurations) {
        List<SchemaItem> extendedSchemaItems = new ArrayList<>();
        boolean hasChildToParentRelationship = false;
        for (PropertyConfiguration configuration : propertyConfigurations.getConfigurations().stream().filter(c -> c.isValid()).toList()) {
            Schema schema = null;
            PropertyPath propertyPath = null;
            for (PropertyPath path : configuration.getPaths().stream().filter(p -> p.hasValidRelatedObjectsSpec()).toList()) {
                RelatedObjectsSpec relatedObjectsSpec = path.getRelatedObjectsSpec();
                if (relatedObjectsSpec.isChildToParent()) {
                    hasChildToParentRelationship = true;
                }
                if (relatedObjectKindSchemas.containsKey(relatedObjectsSpec.getRelatedObjectKind())) {
                    // Refer to the schema of the related object
                    schema = relatedObjectKindSchemas.get(relatedObjectsSpec.getRelatedObjectKind());
                    propertyPath = path;
                    break;
                }
            }
            if (schema == null) {
                // Refer to the schema of the object itself
                schema = originalSchema;
                propertyPath = configuration.getPaths().stream().filter(p -> p.getRelatedObjectsSpec() == null && p.hasValidValueExtraction()).findFirst().orElse(null);
            }

            if (schema != null && propertyPath != null) {
                List<SchemaItem> schemaItems = getExtendedSchemaItems(schema, configuration, propertyPath);
                extendedSchemaItems.addAll(schemaItems);
            }
        }

        if (hasChildToParentRelationship) {
            extendedSchemaItems.add(createAssociatedIdentitiesSchemaItem());
        }

        return extendedSchemaItems;
    }

    @Override
    public String resolveConcreteKind(String kind) {
        if (Strings.isNullOrEmpty(kind) || PropertyUtil.isConcreteKind(kind)) {
            return kind;
        }

        String concreteKind = kindCache.get(kind);
        if (concreteKind == null) {
            concreteKind = getLatestVersionOfKind(kind);
            if (!Strings.isNullOrEmpty(concreteKind)) {
                kindCache.put(kind, concreteKind);
            }
        }
        return concreteKind;
    }

    @Override
    public void cacheDataRecord(String recordId, String kind, Map<String, Object> dataMap) {
        Map<String, Object> previousDataMap = this.getObjectData(kind, recordId);
        RecordInfo recordInfo = new RecordInfo();
        recordInfo.setId(recordId);
        recordInfo.setKind(kind);
        RecordChangeInfo changedInfo = new RecordChangeInfo();
        changedInfo.setRecordInfo(recordInfo);
        // Using recordChangeInfoCache is the best effort to avoid updating the associated records when unnecessary
        // It should only store the updated records (ids) with updated properties. However, in order to
        // handle the case that a new record is updated in a short period of time, the ids of the new records with OPT
        // OperationType.create should be cached too.
        if (previousDataMap == null || previousDataMap.isEmpty()) {
            recordInfo.setOp(OperationType.create.getValue());
        } else {
            recordInfo.setOp(OperationType.update.getValue());
            List<String> updatedProperties = PropertyUtil.getChangedProperties(previousDataMap, dataMap);

            RecordChangeInfo previousChangedInfo = recordChangeInfoCache.get(recordId);
            if(previousChangedInfo != null) {
                if(previousChangedInfo.getRecordInfo().getOp().equals(OperationType.create.getValue())) {
                    recordInfo.setOp(OperationType.create.getValue());
                }
                else if(previousChangedInfo.getUpdatedProperties() != null) {
                    previousChangedInfo.getUpdatedProperties().forEach(p -> {
                        if(!updatedProperties.contains(p))
                            updatedProperties.add(p);
                    });
                }
            }

            if(recordInfo.getOp().equals(OperationType.update.getValue()))
                changedInfo.setUpdatedProperties(updatedProperties);
        }
        recordChangeInfoCache.put(recordId, changedInfo);
        RecordData recordData = new RecordData();
        recordData.setData(dataMap);
        relatedObjectCache.put(recordId, recordData);
    }

    @Override
    public void updateAssociatedRecords(RecordChangedMessages message, Map<String, List<String>> upsertKindIds, Map<String, List<String>> deleteKindIds) {
        if (upsertKindIds == null) {
            upsertKindIds = new HashMap<>();
        }
        if (deleteKindIds == null) {
            deleteKindIds = new HashMap<>();
        }

        Map<String, String> attributes = message.getAttributes();
        String ancestors = attributes.containsKey(Constants.ANCESTRY_KINDS) ? attributes.get(Constants.ANCESTRY_KINDS) : "";
        Map<String, List<RecordChangeInfo>> recordChangeInfoMap = createRecordChangeInfoMap(upsertKindIds, deleteKindIds);
        for (Map.Entry<String, List<RecordChangeInfo>> entry : recordChangeInfoMap.entrySet()) {
            String kind = entry.getKey();
            List<RecordChangeInfo> recordChangeInfoList = entry.getValue();
            String updatedAncestors = Strings.isNullOrEmpty(ancestors) ? kind : ancestors + ANCESTRY_KINDS_DELIMITER + kind;

            updateAssociatedParentRecords(updatedAncestors, kind, recordChangeInfoList);
            updateAssociatedChildrenRecords(updatedAncestors, kind, recordChangeInfoList);
        }
    }

    /******************************************************** Private methods **************************************************************/
    private boolean isNullOrEmptyConfigurations(PropertyConfigurations configuration) {
        return configuration == null || Strings.isNullOrEmpty(configuration.getCode());
    }

    private SchemaItem createAssociatedIdentitiesSchemaItem() {
        SchemaItem extendedSchemaItem = new SchemaItem();
        extendedSchemaItem.setPath(ASSOCIATED_IDENTITIES_PROPERTY);
        extendedSchemaItem.setKind(ASSOCIATED_IDENTITIES_PROPERTY_STORAGE_FORMAT_TYPE);
        return extendedSchemaItem;
    }

    private String createIdsQuery(List<String> ids) {
        return String.format("id: (%s)", createIdsFilter(ids));
    }

    private String createIdsFilter(List<String> ids) {
        StringBuilder idsBuilder = new StringBuilder();
        for (String id : ids) {
            if (idsBuilder.length() > 0) {
                idsBuilder.append(" OR ");
            }
            idsBuilder.append("\"");
            idsBuilder.append(PropertyUtil.removeIdPostfix(id));
            idsBuilder.append("\"");
        }
        return idsBuilder.toString();
    }

    private void createWorkerTask(String ancestors, List<RecordInfo> recordInfos) {
        Map<String, String> attributes = new HashMap<>();
        DpsHeaders headers = this.requestInfo.getHeadersWithDwdAuthZ();
        attributes.put(DpsHeaders.ACCOUNT_ID, headers.getAccountId());
        attributes.put(DpsHeaders.DATA_PARTITION_ID, headers.getPartitionIdWithFallbackToAccountId());
        attributes.put(DpsHeaders.CORRELATION_ID, headers.getCorrelationId());
        attributes.put(Constants.ANCESTRY_KINDS, ancestors);

        RecordChangedMessages recordChangedMessages = RecordChangedMessages.builder().data(gson.toJson(recordInfos)).attributes(attributes).build();
        String recordChangedMessagePayload = gson.toJson(recordChangedMessages);
        this.indexerQueueTaskBuilder.createWorkerTask(recordChangedMessagePayload, 0L, this.requestInfo.getHeadersWithDwdAuthZ());
    }

    private Map<String, Object> getObjectData(String kind, String id) {
        RecordData recordData = relatedObjectCache.get(id);
        Map<String, Object> data = (recordData != null)? recordData.getData() : null;
        if (data == null) {
            SearchRequest searchRequest = new SearchRequest();
            searchRequest.setKind(kind);
            String query = String.format("id: \"%s\"", id);
            searchRequest.setQuery(query);
            SearchRecord searchRecord = searchFirstRecord(searchRequest);

            if (searchRecord != null) {
                data = searchRecord.getData();
                recordData = new RecordData();
                recordData.setData(data);
                relatedObjectCache.put(id, recordData);
            }
        }

        return data;
    }

    private Map<String, Map<String, Object>> getRelatedObjectsData(Map<String, Object> originalDataMap, PropertyConfigurations propertyConfigurations) {
        Map<String, Map<String, Object>> idData = new HashMap<>();
        Map<String, Set<String>> kindIds = new HashMap<>();
        for (PropertyConfiguration configuration : propertyConfigurations.getConfigurations().stream().filter(c -> c.isValid()).toList()) {
            for (PropertyPath path : configuration.getPaths().stream().filter(p -> p.hasValidValueExtraction()).toList()) {
                if (path.hasValidRelatedObjectsSpec()) {
                    RelatedObjectsSpec relatedObjectsSpec = path.getRelatedObjectsSpec();
                    List<String> relatedObjectIds = getRelatedObjectIds(originalDataMap, relatedObjectsSpec);
                    String relatedObjectKind = relatedObjectsSpec.getRelatedObjectKind();
                    if(!kindIds.containsKey(relatedObjectKind)) {
                        kindIds.put(relatedObjectKind, new HashSet<>());
                    }
                    kindIds.get(relatedObjectKind).addAll(relatedObjectIds);
                }
            }
        }

        if(!kindIds.isEmpty()) {
            List<String> kindsToSearch = new ArrayList<>();
            List<String> idsToSearch = new ArrayList<>();
            for (Map.Entry<String, Set<String>> entry : kindIds.entrySet()) {
                for (String recordId : entry.getValue()) {
                    String id = PropertyUtil.removeIdPostfix(recordId);
                    RecordData recordData = relatedObjectCache.get(id);
                    Map<String, Object> data = (recordData != null)? recordData.getData() : null;
                    if (data != null) {
                        idData.put(id, data);
                    } else {
                        kindsToSearch.add(entry.getKey());
                        idsToSearch.add(recordId);
                    }
                }
            }
            if (!kindsToSearch.isEmpty()) {
                List<SearchRecord> records = searchRelatedRecords(kindsToSearch, idsToSearch);
                for (SearchRecord searchRecord : records) {
                    Map<String, Object> data = searchRecord.getData();
                    String id = searchRecord.getId();
                    RecordData recordData = new RecordData();
                    recordData.setData(data);
                    relatedObjectCache.put(id, recordData);
                    idData.put(id, data);
                }
            }
        }
        return idData;
    }

    private Map<String, List<RecordChangeInfo>> createRecordChangeInfoMap(Map<String, List<String>> upsertKindIds, Map<String, List<String>> deleteKindIds) {
        Map<String, List<RecordChangeInfo>> recordChangeInfoMap = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : upsertKindIds.entrySet()) {
            List<RecordChangeInfo> recordChangeInfoList = getOrCreateRecordChangeInfoList(entry.getKey(), recordChangeInfoMap);
            for (String id : entry.getValue()) {
                RecordChangeInfo changeInfo = recordChangeInfoCache.get(id);
                if (changeInfo == null) {
                    changeInfo = new RecordChangeInfo();
                    changeInfo.setRecordInfo(this.createRecordInfo(entry.getKey(), id, OperationType.create));
                }
                recordChangeInfoList.add(changeInfo);
            }
        }
        for (Map.Entry<String, List<String>> entry : deleteKindIds.entrySet()) {
            List<RecordChangeInfo> recordChangeInfoList = getOrCreateRecordChangeInfoList(entry.getKey(), recordChangeInfoMap);
            for (String id : entry.getValue()) {
                RecordChangeInfo changeInfo = new RecordChangeInfo();
                changeInfo.setRecordInfo(this.createRecordInfo(entry.getKey(), id, OperationType.delete));
                recordChangeInfoList.add(changeInfo);
            }
        }

        return recordChangeInfoMap;
    }

    private List<RecordChangeInfo> getOrCreateRecordChangeInfoList(String kind, Map<String, List<RecordChangeInfo>> recordChangeInfoMap) {
        List<RecordChangeInfo> recordChangeInfoList;
        if (recordChangeInfoMap.containsKey(kind)) {
            recordChangeInfoList = recordChangeInfoMap.get(kind);
        } else {
            recordChangeInfoList = new ArrayList<>();
            recordChangeInfoMap.put(kind, recordChangeInfoList);
        }
        return recordChangeInfoList;
    }

    private RecordInfo createRecordInfo(String kind, String id, OperationType operationType) {
        RecordInfo recordInfo = new RecordInfo();
        recordInfo.setKind(kind);
        recordInfo.setId(id);
        recordInfo.setOp(operationType.getValue());
        return recordInfo;
    }

    private List<SchemaItem> getExtendedSchemaItems(Schema schema, PropertyConfiguration configuration, PropertyPath propertyPath) {
        String relatedPropertyPath = PropertyUtil.removeDataPrefix(propertyPath.getValueExtraction().getValuePath());
        List<SchemaItem> extendedSchemaItems;
        if (relatedPropertyPath.contains(ARRAY_SYMBOL)) { // Nested
            extendedSchemaItems = cloneExtendedSchemaItemsFromNestedSchema(Arrays.asList(schema.getSchema()), configuration, relatedPropertyPath);
        } else {// Flatten
            extendedSchemaItems = cloneExtendedSchemaItems(Arrays.asList(schema.getSchema()), configuration, relatedPropertyPath);
        }

        if (extendedSchemaItems.isEmpty()) {
            // It is possible that the format (or schema) of the source property is not defined.
            // In this case, we assume that the format of property is string in order to make its value(s) searchable
            SchemaItem extendedSchemaItem = new SchemaItem();
            extendedSchemaItem.setPath(configuration.getExtendedPropertyName());
            if (configuration.isExtractFirstMatch()) {
                extendedSchemaItem.setKind(STRING_KIND);
            } else {
                extendedSchemaItem.setKind(STRING_ARRAY_KIND);
            }
            extendedSchemaItems.add(extendedSchemaItem);
        }
        return extendedSchemaItems;
    }

    private List<SchemaItem> cloneExtendedSchemaItems(List<SchemaItem> schemaItems, PropertyConfiguration configuration, String relatedPropertyPath) {
        List<SchemaItem> extendedSchemaItems = new ArrayList<>();
        String extendedPropertyName = configuration.getExtendedPropertyName();
        for (SchemaItem schemaItem : schemaItems) {
            if (PropertyUtil.isPropertyPathMatched(schemaItem.getPath(), relatedPropertyPath)) {
                String path = schemaItem.getPath();
                path = path.replace(relatedPropertyPath, extendedPropertyName);
                SchemaItem extendedSchemaItem = new SchemaItem();
                extendedSchemaItem.setPath(path);
                if (configuration.isExtractFirstMatch()) {
                    extendedSchemaItem.setKind(schemaItem.getKind());
                } else {
                    extendedSchemaItem.setKind("[]" + schemaItem.getKind());
                }
                extendedSchemaItems.add(extendedSchemaItem);
            }
        }
        return extendedSchemaItems;
    }

    private List<SchemaItem> cloneExtendedSchemaItemsFromNestedSchema(List<SchemaItem> schemaItems, PropertyConfiguration configuration, String relatedPropertyPath) {
        if (relatedPropertyPath.contains(ARRAY_SYMBOL)) {
            List<SchemaItem> extendedSchemaItems = new ArrayList<>();
            int idx = relatedPropertyPath.indexOf(NESTED_OBJECT_DELIMITER);
            String prePath = relatedPropertyPath.substring(0, idx);
            String postPath = relatedPropertyPath.substring(idx + NESTED_OBJECT_DELIMITER.length());
            for (SchemaItem schemaItem : schemaItems) {
                if (schemaItem.getPath().equals(prePath)) {
                    if (schemaItem.getKind().equals(SCHEMA_NESTED_KIND) && schemaItem.getProperties() != null) {
                        schemaItems = Arrays.asList(schemaItem.getProperties());
                        extendedSchemaItems = cloneExtendedSchemaItemsFromNestedSchema(schemaItems, configuration, postPath);
                    }
                    break;
                }
            }
            return extendedSchemaItems;
        } else {
            return cloneExtendedSchemaItems(schemaItems, configuration, relatedPropertyPath);
        }
    }

    private List<String> getRelatedObjectIds(Map<String, Object> dataMap, RelatedObjectsSpec relatedObjectsSpec) {
        if (dataMap == null || dataMap.isEmpty() || relatedObjectsSpec == null || !relatedObjectsSpec.isValid())
            return new ArrayList<>();

        Map<String, Object> propertyValues = getPropertyValues(dataMap, relatedObjectsSpec.getRelatedObjectID(), relatedObjectsSpec, relatedObjectsSpec.hasValidCondition(), false);
        List<String> relatedObjectIds = new ArrayList<>();
        for (Object value : propertyValues.values()) {
            if (value instanceof List<? extends Object> values) {
                for (Object obj : values) {
                    relatedObjectIds.add(obj.toString());
                }
            } else {
                relatedObjectIds.add(value.toString());
            }
        }
        return relatedObjectIds;
    }

    private Map<String, Object> getExtendedPropertyValues(String extendedPropertyName, Map<String, Object> dataMap, ValueExtraction valueExtraction, boolean isExtractFirstMatch) {
        if (dataMap == null || dataMap.isEmpty() || valueExtraction == null || !valueExtraction.isValid())
            return new HashMap<>();
        Map<String, Object> propertyValues = getPropertyValues(dataMap, valueExtraction.getValuePath(), valueExtraction, valueExtraction.hasValidCondition(), isExtractFirstMatch);
        return PropertyUtil.replacePropertyPaths(extendedPropertyName, valueExtraction.getValuePath(), propertyValues);
    }

    private Map<String, Object> getPropertyValues(Map<String, Object> dataMap, String valuePath, RelatedCondition relatedCondition, boolean hasValidCondition, boolean isExtractFirstMatch) {
        valuePath = PropertyUtil.removeDataPrefix(valuePath);
        Map<String, Object> propertyValues = new HashMap<>();
        if (valuePath.contains(ARRAY_SYMBOL)) { // Nested
            String conditionProperty = null;
            List<String> conditionMatches = null;
            if (hasValidCondition) {
                conditionProperty = relatedCondition.getRelatedConditionProperty();
                conditionMatches = relatedCondition.getRelatedConditionMatches();
            }

            propertyValues = getPropertyValuesFromNestedObjects(dataMap, valuePath, conditionProperty, conditionMatches, hasValidCondition, isExtractFirstMatch);
        } else { // Flatten
            String conditionProperty = null;
            List<String> conditionMatches = null;
            if (hasValidCondition) {
                conditionProperty = relatedCondition.getRelatedConditionProperty();
                conditionMatches = relatedCondition.getRelatedConditionMatches();
            }
            propertyValues = getPropertyValueOfNoneNestedProperty(dataMap, valuePath, conditionProperty, conditionMatches, hasValidCondition);
            if(!isExtractFirstMatch) {
                Map<String, Object> tmpValues = new HashMap<>();
                for(Map.Entry<String, Object> entry : propertyValues.entrySet()) {
                    if (entry.getValue() instanceof List<? extends Object>) {
                        tmpValues.put(entry.getKey(), entry.getValue());
                    }
                    else {
                        List<Object> values = new ArrayList<>();
                        values.add(entry.getValue());
                        tmpValues.put(entry.getKey(), values);
                    }
                }
                propertyValues = tmpValues;
            }
        }

        return propertyValues;
    }

    private Map<String, Object> getPropertyValuesFromNestedObjects(Map<String, Object> dataMap, String valuePath, String conditionProperty, List<String> conditionMatches, boolean hasCondition, boolean isExtractFirstMatch) {
        Map<String, Object> propertyValues = new HashMap<>();

        if (valuePath.contains(ARRAY_SYMBOL)) {
            int idx = valuePath.indexOf(NESTED_OBJECT_DELIMITER);
            String prePath = valuePath.substring(0, idx);
            String postPath = valuePath.substring(idx + NESTED_OBJECT_DELIMITER.length());
            if(conditionProperty != null) {
                idx = conditionProperty.indexOf(NESTED_OBJECT_DELIMITER);
                if(idx > 0) {
                    conditionProperty = conditionProperty.substring(idx + NESTED_OBJECT_DELIMITER.length());
                }
                else {
                    // Should not reach here
                    conditionProperty = null;
                }
            }
            try {
                if (dataMap.containsKey(prePath) && dataMap.get(prePath) != null) {
                    List<Map<String, Object>> nestedObjects = (List<Map<String, Object>>) dataMap.get(prePath);
                    for (Map<String, Object> nestedObject : nestedObjects) {
                        Map<String, Object> subPropertyValues = getPropertyValuesFromNestedObjects(nestedObject, postPath, conditionProperty, conditionMatches, hasCondition, isExtractFirstMatch);
                        for (Map.Entry<String, Object> entry: subPropertyValues.entrySet()) {
                            String key = prePath + ARRAY_SYMBOL + PROPERTY_DELIMITER + entry.getKey();
                            if(isExtractFirstMatch) {
                                propertyValues.put(key, entry.getValue());
                            }
                            else {
                                List<Object> values = propertyValues.containsKey(key)
                                        ? (List<Object>)propertyValues.get(key)
                                        : new ArrayList<>();
                                if(entry.getValue() instanceof List<? extends Object> valueList) {
                                    values.addAll(valueList);
                                }
                                else {
                                    values.add(entry.getValue());
                                }
                                propertyValues.put(key, values);
                            }
                        }
                        if (isExtractFirstMatch)
                            break;
                    }
                }
            } catch (Exception ex) {
                //Ignore cast exception
            }
        } else {
            propertyValues = getPropertyValueOfNoneNestedProperty(dataMap, valuePath, conditionProperty, conditionMatches, hasCondition);
        }
        return propertyValues;
    }

    private Map<String, Object> getPropertyValueOfNoneNestedProperty(Map<String, Object> dataMap, String valuePath, String conditionProperty, List<String> conditionMatches, boolean hasCondition) {
        Map<String, Object> propertyValue = PropertyUtil.getValueOfNoneNestedProperty(valuePath, dataMap);
        if(!propertyValue.isEmpty() && hasCondition) {
            boolean matched = false;
            Map<String, Object> conditionPropertyValue = PropertyUtil.getValueOfNoneNestedProperty(conditionProperty, dataMap);
            if (conditionPropertyValue.containsKey(conditionProperty)) {
                for(String condition: conditionMatches) {
                    if(PropertyUtil.isMatch(conditionPropertyValue.get(conditionProperty).toString(), condition)) {
                        matched = true;
                        break;
                    }
                }
            }
            if(!matched) {
                // Reset the propertyValue if there is no match
                propertyValue = new HashMap<>();
            }
        }
        return propertyValue;
    }


    private List<String> getChildrenKinds(String parentKind) {
        final String parentKindWithMajor = PropertyUtil.getKindWithMajor(parentKind);
        ChildrenKinds childrenKinds = childrenKindsCache.get(parentKindWithMajor);
        if(childrenKinds == null) {
            childrenKinds = new ChildrenKinds();
            Set<String> kinds = new HashSet<>();
            for (PropertyConfigurations propertyConfigurations: searchChildrenKindConfigurations(parentKindWithMajor)) {
                kinds.add(propertyConfigurations.getCode());
            }
            childrenKinds.setKinds(new ArrayList<>(kinds));
            childrenKindsCache.put(parentKindWithMajor, childrenKinds);
        }

        return childrenKinds.getKinds();
    }

    private ParentChildRelationshipSpecs getParentChildRelatedObjectsSpecs(String childKind) {
        final String childKindWithMajor = PropertyUtil.getKindWithMajor(childKind);

        ParentChildRelationshipSpecs specs = parentChildRelationshipSpecsCache.get(childKindWithMajor);
        if (specs == null) {
            List<ParentChildRelationshipSpec> specsList = new ArrayList<>();
            specs = new ParentChildRelationshipSpecs();
            specs.setSpecList(specsList);

            List<PropertyConfigurations> configurationsList = searchParentKindConfigurations((childKindWithMajor));
            for (PropertyConfigurations configurations : configurationsList) {
                for (PropertyConfiguration configuration : configurations.getConfigurations()) {
                    List<PropertyPath> matchedPropertyPaths = configuration.getPaths().stream().filter(p ->
                                            p.hasValidRelatedObjectsSpec() &&
                                            p.getRelatedObjectsSpec().isParentToChildren() &&
                                            p.getRelatedObjectsSpec().getRelatedObjectKind().contains(childKindWithMajor))
                            .toList();
                    for(PropertyPath propertyPath: matchedPropertyPaths) {
                        ParentChildRelationshipSpec spec = toParentChildRelationshipSpec(propertyPath, configurations.getCode(), childKindWithMajor);
                        boolean merged = false;
                        for(ParentChildRelationshipSpec sp: specsList) {
                            if(sp.equals(spec)) {
                                List<String> childValuePaths = sp.getChildValuePaths();
                                if(!childValuePaths.contains(spec.getChildValuePaths().get(0))) {
                                    childValuePaths.add(spec.getChildValuePaths().get(0));
                                }
                                merged = true;
                                break;
                            }
                        }
                        if(!merged) {
                            specsList.add(spec);
                        }
                    }
                }
            }

            parentChildRelationshipSpecsCache.put(childKindWithMajor, specs);
        }

        return specs;
    }

    private ParentChildRelationshipSpec toParentChildRelationshipSpec(PropertyPath propertyPath, String parentKind, String childKind) {
        ParentChildRelationshipSpec spec = new ParentChildRelationshipSpec();
        spec.setParentKind(parentKind);
        spec.setParentObjectIdPath(propertyPath.getRelatedObjectsSpec().getRelatedObjectID());
        spec.setChildKind(childKind);
        String valuePath = PropertyUtil.removeDataPrefix(propertyPath.getValueExtraction().getValuePath());
        spec.getChildValuePaths().add(valuePath);
        return spec;
    }

    private void updateAssociatedParentRecords(String ancestors, String childKind, List<RecordChangeInfo> childRecordChangeInfos) {
        ParentChildRelationshipSpecs specs = getParentChildRelatedObjectsSpecs(childKind);
        Set<String> ancestorSet = new HashSet<>(Arrays.asList(ancestors.split(ANCESTRY_KINDS_DELIMITER)));
        for (ParentChildRelationshipSpec spec : specs.getSpecList()) {
            List<String> childRecordIds = getChildRecordIdsWithExtendedPropertiesChanged(spec, childRecordChangeInfos);

            List<String> parentIds = new ArrayList<>();
            if (!childRecordIds.isEmpty()) {
                parentIds = searchUniqueParentIds(childKind, childRecordIds, spec.getParentObjectIdPath());
            }
            if (parentIds.isEmpty())
                continue;

            final int limit = configurationProperties.getStorageRecordsByKindBatchSize();
            Map<String, List<String>> parentKindIds = searchKindIds(spec.getParentKind(), parentIds);
            List<RecordInfo> recordInfos = new ArrayList<>();
            for (Map.Entry<String, List<String>> entry : parentKindIds.entrySet()) {
                if (ancestorSet.contains(entry.getKey()))
                    continue; // circular indexing found.

                for (String id : entry.getValue()) {
                    RecordInfo recordInfo = new RecordInfo();
                    recordInfo.setKind(entry.getKey());
                    recordInfo.setId(id);
                    recordInfo.setOp(OperationType.update.getValue());
                    recordInfos.add(recordInfo);

                    if (recordInfos.size() >= limit) {
                        createWorkerTask(ancestors, recordInfos);
                        recordInfos = new ArrayList<>();
                    }
                }
            }
            if (!recordInfos.isEmpty()) {
                createWorkerTask(ancestors, recordInfos);
            }
        }
    }

    private List<String> getChildRecordIdsWithExtendedPropertiesChanged(ParentChildRelationshipSpec spec, List<RecordChangeInfo> childRecordChangeInfos) {
        List<String> childRecordIds = new ArrayList<>();
        for (RecordChangeInfo recordChangeInfo : childRecordChangeInfos) {
            if (recordChangeInfo.getRecordInfo().getOp().equals(OperationType.update.getValue())) {
                String updatedExtendedProperty = recordChangeInfo.getUpdatedProperties().stream().filter(p -> {
                    for (String valuePath : spec.getChildValuePaths()) {
                        if (PropertyUtil.isPropertyPathMatched(valuePath, p) ||
                            PropertyUtil.isPropertyPathMatched(p, valuePath)) {
                            return true;
                        }
                    }
                    return false;
                }).findFirst().orElse(null);

                if (updatedExtendedProperty != null) {
                    // The parent property that is extended by the children was updated
                    childRecordIds.add(recordChangeInfo.getRecordInfo().getId());
                }
            }
            else {
                childRecordIds.add(recordChangeInfo.getRecordInfo().getId());
            }
        }
        return childRecordIds;
    }

    private boolean areExtendedPropertiesChanged(String childKind, List<RecordChangeInfo> parentRecordChangeInfos) {
        if (parentRecordChangeInfos.stream().filter(info -> !info.getRecordInfo().getOp().equals(OperationType.update.getValue())).findFirst().orElse(null) != null) {
            // If there is any OP of the parent record(s) that is not OperationType.update. It must be OperationType.delete in this case. Then the child record should be updated
            return true;
        }

        PropertyConfigurations propertyConfigurations = this.getPropertyConfigurations(childKind);
        if(propertyConfigurations != null) {
            for (PropertyConfiguration propertyConfiguration : propertyConfigurations.getConfigurations()) {
                for (PropertyPath propertyPath : propertyConfiguration.getPaths().stream().filter(
                        p -> p.hasValidValueExtraction() && p.hasValidRelatedObjectsSpec()).toList()) {
                    String relatedObjectKind = propertyPath.getRelatedObjectsSpec().getRelatedObjectKind();
                    String valuePath = PropertyUtil.removeDataPrefix(propertyPath.getValueExtraction().getValuePath());

                    // Find any parent record which has changed property that is extended by the child (kind)
                    RecordChangeInfo parentRecordChangeInfo = parentRecordChangeInfos.stream().filter(info -> {
                        if (PropertyUtil.hasSameMajorKind(info.getRecordInfo().getKind(), relatedObjectKind)) {
                            List<String> matchedProperties = info.getUpdatedProperties().stream().filter(
                                    p -> PropertyUtil.isPropertyPathMatched(p, valuePath) || PropertyUtil.isPropertyPathMatched(valuePath, p)).toList();
                            return !matchedProperties.isEmpty();
                        }
                        return false;
                    }).findFirst().orElse(null);
                    if (parentRecordChangeInfo != null) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private void updateAssociatedChildrenRecords(String ancestors, String parentKind, List<RecordChangeInfo> recordChangeInfos) {
        List<String> processedIds = recordChangeInfos.stream().map(recordChangeInfo -> recordChangeInfo.getRecordInfo().getId()).toList();
        String query = String.format("data.%s:(%s)", ASSOCIATED_IDENTITIES_PROPERTY, createIdsFilter(processedIds));

        List<String> childrenKinds = getChildrenKinds(parentKind);
        for (String ancestryKind : ancestors.split(ANCESTRY_KINDS_DELIMITER)) {
            // Exclude the kinds in the ancestryKinds to prevent circular chasing
            childrenKinds.removeIf(ancestryKind::contains);
        }
        if(childrenKinds.isEmpty()) {
            return;
        }

        List<String> multiKinds = new ArrayList<>();
        for(String kind: childrenKinds) {
            String kindWithMajor = PropertyUtil.getKindWithMajor(kind) + "*.*";
            multiKinds.add(kindWithMajor);
        }
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setKind(multiKinds);
        searchRequest.setQuery(query);
        searchRequest.setReturnedFields(Arrays.asList("kind", "id", "data." + ASSOCIATED_IDENTITIES_PROPERTY));
        List<RecordInfo> recordInfos = new ArrayList<>();
        for (SearchRecord searchRecord : searchRecordsWithCursor(searchRequest)) {
            Map<String, Object> data = searchRecord.getData();
            if (!data.containsKey(ASSOCIATED_IDENTITIES_PROPERTY) || data.get(ASSOCIATED_IDENTITIES_PROPERTY) == null)
                continue;

            List<String> associatedParentIds = (List<String>) data.get(ASSOCIATED_IDENTITIES_PROPERTY);
            List<RecordChangeInfo> associatedParentRecordChangeInfos = recordChangeInfos.stream().filter(
                    info -> associatedParentIds.contains(info.getRecordInfo().getId())).toList();
            if (areExtendedPropertiesChanged(searchRecord.getKind(), associatedParentRecordChangeInfos)) {
                RecordInfo recordInfo = new RecordInfo();
                recordInfo.setKind(searchRecord.getKind());
                recordInfo.setId(searchRecord.getId());
                recordInfo.setOp(OperationType.update.getValue());
                recordInfos.add(recordInfo);

                if (recordInfos.size() >= configurationProperties.getStorageRecordsByKindBatchSize()) {
                    createWorkerTask(ancestors, recordInfos);
                    recordInfos = new ArrayList<>();
                }
            }
        }
        if (!recordInfos.isEmpty()) {
            createWorkerTask(ancestors, recordInfos);
        }
    }

    private String getLatestVersionOfKind(String kindWithMajor) {
        Kind kind = new Kind(kindWithMajor);
        String version = kind.getVersion();
        String[] subVersions = version.split("\\.");
        String majorVersion = subVersions[0];
        String latestKind = null;
        try {
            SchemaInfoResponse response = schemaService.getSchemaInfos(kind.getAuthority(), kind.getSource(), kind.getType(), majorVersion, null, null, true);
            if (response != null && !CollectionUtils.isEmpty(response.getSchemaInfos())) {
                SchemaInfo schemaInfo = response.getSchemaInfos().get(0);
                SchemaIdentity schemaIdentity = schemaInfo.getSchemaIdentity();
                latestKind = schemaIdentity.getAuthority() + ":" +
                        schemaIdentity.getSource() + ":" +
                        schemaIdentity.getEntityType() + ":" +
                        schemaIdentity.getSchemaVersionMajor() + "." +
                        schemaIdentity.getSchemaVersionMinor() + "." +
                        schemaIdentity.getSchemaVersionPatch();
            }
        } catch (Exception e) {
            jaxRsDpsLog.error("failed to get schema info", e);
        }

        return latestKind;
    }

    /****************************** search methods that use search service to get the data **************************************/
    private SearchRequest createSearchRequest(String kind, String query) {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setKind(kind);
        searchRequest.setQuery(query);
        return searchRequest;
    }

    private PropertyConfigurations searchConfigurations(String kind) {
        String query = String.format("data.Code: \"%s\"", kind);
        SearchRequest searchRequest = createSearchRequest(INDEX_PROPERTY_PATH_CONFIGURATION_KIND, query);
        // If there is more than PropertyConfigurations, pick the one that was last modified.
        // Given the property "modifyTime" is not set for new created record, we use property "version"
        // to sort the search result in descending order
        SortQuery sort = new SortQuery();
        sort.setField(Arrays.asList(VERSION_PROPERTY));
        sort.setOrder(Arrays.asList(SortOrder.DESC));
        searchRequest.setSort(sort);
        List<PropertyConfigurations> propertyConfigurationsList = searchConfigurations(searchRequest);
        if(!propertyConfigurationsList.isEmpty()) {
            if(propertyConfigurationsList.size() > 1) {
                jaxRsDpsLog.warning(String.format("There is more than one PropertyConfigurations for kind: %s", kind));
            }
            return propertyConfigurationsList.get(0);
        }
        return null;
    }

    private List<PropertyConfigurations> searchParentKindConfigurations(String childKind) {
        String query = String.format(PARENT_CHILDREN_CONFIGURATION_QUERY_FORMAT, childKind);
        SearchRequest searchRequest = createSearchRequest(INDEX_PROPERTY_PATH_CONFIGURATION_KIND, query);
        return searchConfigurations(searchRequest);
    }

    private List<PropertyConfigurations> searchChildrenKindConfigurations(String parentKind) {
        String query = String.format(CHILDREN_PARENT_CONFIGURATION_QUERY_FORMAT, parentKind);
        SearchRequest searchRequest = createSearchRequest(INDEX_PROPERTY_PATH_CONFIGURATION_KIND, query);
        return searchConfigurations(searchRequest);
    }

    private List<PropertyConfigurations> searchConfigurations(SearchRequest searchRequest) {
        List<PropertyConfigurations> configurationsList = new ArrayList<>();
        for (SearchRecord searchRecord : searchRecords(searchRequest)) {
            try {
                String data = objectMapper.writeValueAsString(searchRecord.getData());
                PropertyConfigurations configurations = objectMapper.readValue(data, PropertyConfigurations.class);
                String kind = PropertyUtil.getKindWithMajor(configurations.getCode());
                propertyConfigurationCache.put(kind, configurations);
                configurationsList.add(configurations);
            } catch (JsonProcessingException e) {
                jaxRsDpsLog.error("failed to deserialize PropertyConfigurations object", e);
            }
        }
        return configurationsList;
    }

    private List<SearchRecord> searchRelatedRecords(List<String> relatedObjectKinds, List<String> relatedObjectIds) {
        SearchRequest searchRequest = new SearchRequest();
        List<String> kinds = new ArrayList<>();
        for(String kind : relatedObjectKinds) {
            if(!PropertyUtil.isConcreteKind(kind))
                kind += "*";
            kinds.add(kind);
        }
        searchRequest.setKind(kinds);
        String query = createIdsQuery(relatedObjectIds);
        searchRequest.setQuery(query);
        return searchRecords(searchRequest);
    }

    private Map<String, List<String>> searchKindIds(String majorKind, List<String> ids) {
        Map<String, List<String>> kindIds = new HashMap<>();
        SearchRequest searchRequest = new SearchRequest();
        String kind = PropertyUtil.isConcreteKind(majorKind) ? majorKind : majorKind + "*";
        searchRequest.setKind(kind);
        String query = createIdsQuery(ids);
        searchRequest.setReturnedFields(Arrays.asList("kind", "id"));
        searchRequest.setQuery(query);
        for (SearchRecord searchRecord : searchRecords(searchRequest)) {
            if (kindIds.containsKey(searchRecord.getKind())) {
                kindIds.get(searchRecord.getKind()).add(searchRecord.getId());
            } else {
                List<String> idList = new ArrayList<>();
                idList.add(searchRecord.getId());
                kindIds.put(searchRecord.getKind(), idList);
            }
        }
        return kindIds;
    }

    private List<String> searchUniqueParentIds(String childKind, List<String> childRecordIds, String parentObjectIdPath) {
        Set<String> parentIds = new HashSet<>();
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setKind(childKind);
        String query = createIdsQuery(childRecordIds);
        searchRequest.setReturnedFields(Arrays.asList(parentObjectIdPath));
        searchRequest.setQuery(query);
        parentObjectIdPath = PropertyUtil.removeDataPrefix(parentObjectIdPath);
        for (SearchRecord searchRecord : searchRecords(searchRequest)) {
            if (searchRecord.getData().containsKey(parentObjectIdPath)) {
                Object id = searchRecord.getData().get(parentObjectIdPath);
                if (id != null && !parentIds.contains(id)) {
                    parentIds.add(id.toString());
                }
            }
        }
        return new ArrayList<>(parentIds);
    }

    private List<SearchRecord> searchChildrenRecords(String childrenObjectKind, String childrenObjectField, String parentId) {
        String kind = PropertyUtil.isConcreteKind(childrenObjectKind) ? childrenObjectKind : childrenObjectKind + "*";
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setKind(kind);
        String query = String.format("%s: \"%s\"", childrenObjectField, parentId);
        searchRequest.setQuery(query);
        return searchRecordsWithCursor(searchRequest);
    }

    private List<SearchRecord> searchRecordsWithCursor(SearchRequest searchRequest) {
        searchRequest.setLimit(MAX_SEARCH_LIMIT);
        List<SearchRecord> allRecords = new ArrayList<>();
        try {
            List<SearchRecord> results = null;
            do {
                SearchResponse searchResponse = searchService.queryWithCursor(searchRequest);
                results = searchResponse.getResults();
                if (!CollectionUtils.isEmpty(results)) {
                    allRecords.addAll(results);
                    if (!Strings.isNullOrEmpty(searchResponse.getCursor()) && results.size() == MAX_SEARCH_LIMIT) {
                        searchRequest.setCursor(searchResponse.getCursor());
                    }
                }
            } while(results != null && results.size() == MAX_SEARCH_LIMIT);
        } catch (URISyntaxException e) {
            jaxRsDpsLog.error(SEARCH_GENERAL_ERROR, e);
        }
        return allRecords;
    }

    // The search without cursor can return max. 10,000 records
    private List<SearchRecord> searchRecords(SearchRequest searchRequest) {
        searchRequest.setLimit(MAX_SEARCH_LIMIT);
        int offset = 0;
        List<SearchRecord> allRecords = new ArrayList<>();
        try {
            List<SearchRecord> results = null;
            do {
                SearchResponse searchResponse = searchService.query(searchRequest);
                results = searchResponse.getResults();
                if (!CollectionUtils.isEmpty(results)) {
                    allRecords.addAll(results);
                    offset += results.size();
                    searchRequest.setOffset(offset);
                }
            } while(results != null && results.size() == MAX_SEARCH_LIMIT);
        } catch (URISyntaxException e) {
            jaxRsDpsLog.error(SEARCH_GENERAL_ERROR, e);
        }
        return allRecords;
    }

    private SearchRecord searchFirstRecord(SearchRequest searchRequest) {
        searchRequest.setLimit(1);
        try {
            SearchResponse searchResponse = searchService.query(searchRequest);
            List<SearchRecord> results = searchResponse.getResults();
            if (results != null && !results.isEmpty()) {
                return results.get(0);
            }
        } catch (URISyntaxException e) {
            jaxRsDpsLog.error(SEARCH_GENERAL_ERROR, e);
        }
        return null;
    }
}
