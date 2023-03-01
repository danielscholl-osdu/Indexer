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
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.indexer.OperationType;
import org.opengroup.osdu.core.common.model.indexer.RecordInfo;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.core.common.model.storage.Schema;
import org.opengroup.osdu.core.common.model.storage.SchemaItem;
import org.opengroup.osdu.core.common.provider.interfaces.IRequestInfo;
import org.opengroup.osdu.indexer.cache.KindCache;
import org.opengroup.osdu.indexer.cache.ParentChildRelatedObjectsSpecsCache;
import org.opengroup.osdu.indexer.cache.PropertyConfigurationsCache;
import org.opengroup.osdu.indexer.cache.RelatedObjectCache;
import org.opengroup.osdu.indexer.config.IndexerConfigurationProperties;
import org.opengroup.osdu.indexer.model.Constants;
import org.opengroup.osdu.indexer.model.SearchRecord;
import org.opengroup.osdu.indexer.model.SearchRequest;
import org.opengroup.osdu.indexer.model.SearchResponse;
import org.opengroup.osdu.indexer.model.indexproperty.*;
import org.opengroup.osdu.indexer.util.IndexerQueueTaskBuilder;
import org.opengroup.osdu.indexer.util.PropertyUtil;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;


@Component
public class PropertyConfigurationsServiceImpl implements PropertyConfigurationsService {
    private static final String ASSOCIATED_IDENTITIES_PROPERTY = "AssociatedIdentities";
    private static final String ASSOCIATED_IDENTITIES_PROPERTY_STORAGE_FORMAT_TYPE = "[]string";
    private static final String WILD_CARD_KIND = "*:*:*:*";
    private static final String INDEX_PROPERTY_PATH_CONFIGURATION_KIND = "osdu:wks:reference-data--IndexPropertyPathConfiguration:*";
    private static final String ANCESTRY_KINDS_DELIMITER = ",";
    private static final String PARENT_CHILDREN_CONFIGURATION_QUERY_FORMAT =
            "nested(data.Configurations, nested(data.Configurations.Paths, (RelatedObjectsSpec.RelationshipDirection: ParentToChildren AND RelatedObjectsSpec.RelatedObjectKind:\"%s\")))";
    private static final String EMPTY_CODE = "__EMPTY_CODE__";
    private static final int MAX_SEARCH_LIMIT = 1000;

    private static final String PROPERTY_DELIMITER = ".";
    private static final String ARRAY_SYMBOL = "[]";
    private static final String SCHEMA_NESTED_KIND = "nested";

    private final Gson gson = new Gson();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PropertyConfigurations EMPTY_CONFIGURATIONS = new PropertyConfigurations() {{
        setCode(EMPTY_CODE);
    }};


    @Inject
    private IndexerConfigurationProperties configurationProperties;
    @Inject
    private PropertyConfigurationsCache propertyConfigurationCache;
    @Inject
    private ParentChildRelatedObjectsSpecsCache parentChildRelatedObjectsSpecsCache;
    @Inject
    private KindCache kindCache;
    @Inject
    private RelatedObjectCache relatedObjectCache;
    @Inject
    private SearchService searchService;
    @Inject
    private IndexerQueueTaskBuilder indexerQueueTaskBuilder;
    @Inject
    private IRequestInfo requestInfo;
    @Inject
    private JaxRsDpsLog jaxRsDpsLog;

    @Override
    public PropertyConfigurations getPropertyConfiguration(String kind) {
        if (Strings.isNullOrEmpty(kind))
            return null;
        String dataPartitionId = requestInfo.getHeaders().getPartitionId();
        kind = PropertyUtil.getKindWithMajor(kind);
        String key = dataPartitionId + " | " + kind;

        PropertyConfigurations configuration = propertyConfigurationCache.get(key);
        if (configuration == null) {
            configuration = searchConfigurations(kind);
            if (configuration != null) {
                propertyConfigurationCache.put(key, configuration);
            } else {
                // It is common that a kind does not have extended property. So we need to cache an empty configuration
                // to avoid unnecessary search
                propertyConfigurationCache.put(key, EMPTY_CONFIGURATIONS);
            }
        }

        if (configuration != null && !EMPTY_CODE.equals(configuration.getCode())) {
            return configuration;
        }

        return null;
    }

    @Override
    public Map<String, Object> getRelatedObjectData(String relatedObjectKind, String relatedObjectId) {
        if (Strings.isNullOrEmpty(relatedObjectKind) || Strings.isNullOrEmpty(relatedObjectId)) {
            return null;
        }

        String key = PropertyUtil.removeIdPostfix(relatedObjectId);
        Map<String, Object> relatedObject = relatedObjectCache.get(key);
        if (relatedObject == null) {
            SearchRecord searchRecord = searchRelatedRecord(relatedObjectKind, relatedObjectId);
            if (searchRecord != null) {
                relatedObject = searchRecord.getData();
                relatedObjectCache.put(key, relatedObject);
            }
        }

        return relatedObject;
    }

    @Override
    public Map<String, Object> getExtendedProperties(String objectId, Map<String, Object> originalDataMap, PropertyConfigurations propertyConfigurations) {
        Set<String> associatedIdentities = new HashSet<>();
        Map<String, Object> extendedDataMap = new HashMap<>();
        for (PropertyConfiguration configuration : propertyConfigurations.getConfigurations().stream().filter(c -> c.isValid()).collect(Collectors.toList())) {
            if (originalDataMap.containsKey(configuration.getName()) && originalDataMap.get(configuration.getName()) != null) {
                // If the original record already has the property, then we should not override.
                // For example, if the trajectory record already SpatialLocation value, then it should not be overridden by the SpatialLocation of the well bore.
                continue;
            }

            Map<String, Object> allPropertyValues = new HashMap<>();
            for (PropertyPath path : configuration.getPaths().stream().filter(p -> p.hasValidValueExtraction()).collect(Collectors.toList())) {
                if (path.hasValidRelatedObjectsSpec()) {
                    RelatedObjectsSpec relatedObjectsSpec = path.getRelatedObjectsSpec();
                    if (relatedObjectsSpec.isChildToParent()) {
                        List<String> relatedObjectIds = getRelatedObjectIds(originalDataMap, relatedObjectsSpec);
                        for (String relatedObjectId : relatedObjectIds) {
                            // Store all ids
                            associatedIdentities.add(PropertyUtil.removeIdPostfix(relatedObjectId));
                        }

                        for (String relatedObjectId : relatedObjectIds) {
                            Map<String, Object> relatedObject = getRelatedObjectData(relatedObjectsSpec.getRelatedObjectKind(), relatedObjectId);
                            Map<String, Object> propertyValues = getPropertyValues(relatedObject, path.getValueExtraction(), configuration.isExtractFirstMatch());
                            propertyValues = PropertyUtil.replacePropertyPaths(configuration.getName(), path.getValueExtraction().getValuePath(), propertyValues);

                            if (allPropertyValues.isEmpty() && configuration.isExtractFirstMatch()) {
                                allPropertyValues = propertyValues;
                                break;
                            } else {
                                allPropertyValues = PropertyUtil.combineObjectMap(allPropertyValues, propertyValues);
                            }
                        }
                    } else {
                        List<SearchRecord> childrenRecords = searchChildrenRecords(relatedObjectsSpec.getRelatedObjectKind(), relatedObjectsSpec.getRelatedObjectID(), objectId);
                        for (SearchRecord record : childrenRecords) {
                            Map<String, Object> propertyValues = getPropertyValues(record.getData(), path.getValueExtraction(), configuration.isExtractFirstMatch());
                            propertyValues = PropertyUtil.replacePropertyPaths(configuration.getName(), path.getValueExtraction().getValuePath(), propertyValues);
                            if (allPropertyValues.isEmpty() && configuration.isExtractFirstMatch()) {
                                allPropertyValues = propertyValues;
                                break;
                            } else {
                                allPropertyValues = PropertyUtil.combineObjectMap(allPropertyValues, propertyValues);
                            }
                        }
                    }
                } else {
                    Map<String, Object> propertyValues = getPropertyValues(originalDataMap, path.getValueExtraction(), configuration.isExtractFirstMatch());
                    propertyValues = PropertyUtil.replacePropertyPaths(configuration.getName(), path.getValueExtraction().getValuePath(), propertyValues);

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
            extendedDataMap.put(ASSOCIATED_IDENTITIES_PROPERTY, associatedIdentities.toArray());
        }

        return extendedDataMap;
    }

    @Override
    public List<SchemaItem> getExtendedSchemaItems(Schema originalSchema, Map<String, Schema> relatedObjectKindSchemas, PropertyConfigurations propertyConfigurations) {
        List<SchemaItem> extendedSchemaItems = new ArrayList<>();
        boolean hasChildToParentRelationship = false;
        for (PropertyConfiguration configuration : propertyConfigurations.getConfigurations().stream().filter(c -> c.isValid()).collect(Collectors.toList())) {
            Schema schema = null;
            PropertyPath propertyPath = null;
            for (PropertyPath path : configuration.getPaths().stream().filter(p -> p.hasValidRelatedObjectsSpec()).collect(Collectors.toList())) {
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
        if (Strings.isNullOrEmpty(kind)) {
            return null;
        }

        if (PropertyUtil.isConcreteKind(kind)) {
            return kind;
        }

        if (kindCache.containsKey(kind)) {
            return kindCache.get(kind);
        } else {
            String concreteKind = searchConcreteKind(kind);
            if (!Strings.isNullOrEmpty(concreteKind)) {
                kindCache.put(kind, concreteKind);
            }
            return concreteKind;
        }
    }

    @Override
    public void updateAssociatedRecords(RecordChangedMessages message, Map<String, List<String>> processedKindIdsMap) {
        if (processedKindIdsMap == null || processedKindIdsMap.isEmpty())
            return;

        Map<String, String> attributes = message.getAttributes();
        final String ancestors = attributes.containsKey(Constants.ANCESTRY_KINDS) ? attributes.get(Constants.ANCESTRY_KINDS) : "";
        for (Map.Entry<String, List<String>> entry : processedKindIdsMap.entrySet()) {
            String kind = entry.getKey();
            String updatedAncestors = ancestors.isEmpty() ? kind : ancestors + ANCESTRY_KINDS_DELIMITER + kind;

            updateAssociatedParentRecords(updatedAncestors, entry.getKey(), entry.getValue());
            updateAssociatedChildrenRecords(updatedAncestors, entry.getValue());
        }
    }

    /******************************************************** Private methods **************************************************************/

    private SchemaItem createAssociatedIdentitiesSchemaItem() {
        SchemaItem extendedSchemaItem = new SchemaItem();
        extendedSchemaItem.setPath(ASSOCIATED_IDENTITIES_PROPERTY);
        extendedSchemaItem.setKind(ASSOCIATED_IDENTITIES_PROPERTY_STORAGE_FORMAT_TYPE);
        return extendedSchemaItem;
    }

    private List<SchemaItem> getExtendedSchemaItems(Schema originalSchema, PropertyConfiguration configuration, PropertyPath propertyPath) {
        List<SchemaItem> extendedSchemaItems = new ArrayList<>();
        String relatedPropertyPath = PropertyUtil.removeDataPrefix(propertyPath.getValueExtraction().getValuePath());
        if (relatedPropertyPath.contains(ARRAY_SYMBOL)) { // Nested
            extendedSchemaItems = getExtendedSchemaItemsFromNestedSchema(Arrays.asList(originalSchema.getSchema()), configuration, relatedPropertyPath);
            if (extendedSchemaItems.isEmpty()) {
                // It is possible that the format of the source property is not defined
                // In this case, we assume that the format of property is string in order to make its value(s) searchable
                SchemaItem extendedSchemaItem = new SchemaItem();
                extendedSchemaItem.setPath(configuration.getName());
                if (configuration.isExtractFirstMatch()) {
                    extendedSchemaItem.setKind("string");
                } else {
                    extendedSchemaItem.setKind("[]string");
                }
                extendedSchemaItems.add(extendedSchemaItem);
            }
        } else {// Flatten
            for (SchemaItem schemaItem : originalSchema.getSchema()) {
                if (PropertyUtil.isPropertyPathMatched(schemaItem.getPath(), relatedPropertyPath)) {
                    String path = schemaItem.getPath();
                    path = path.replace(relatedPropertyPath, configuration.getName());
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
        }
        return extendedSchemaItems;
    }

    private List<SchemaItem> getExtendedSchemaItemsFromNestedSchema(List<SchemaItem> schemaItems, PropertyConfiguration configuration, String relatedPropertyPath) {
        List<SchemaItem> extendedSchemaItems = new ArrayList<>();
        if (relatedPropertyPath.contains(PROPERTY_DELIMITER)) {
            int idx = relatedPropertyPath.indexOf(PROPERTY_DELIMITER);
            String prePath = relatedPropertyPath.substring(0, idx);
            String postPath = relatedPropertyPath.substring(idx + 1);
            if (prePath.endsWith(ARRAY_SYMBOL)) {
                prePath = prePath.replace(ARRAY_SYMBOL, "");
            }
            for (SchemaItem schemaItem : schemaItems) {
                if (schemaItem.getPath().equals(prePath)) {
                    if (schemaItem.getKind().equals(SCHEMA_NESTED_KIND) && schemaItem.getProperties() != null) {
                        schemaItems = Arrays.asList(schemaItem.getProperties());
                        extendedSchemaItems = getExtendedSchemaItemsFromNestedSchema(schemaItems, configuration, postPath);
                    }
                    break;
                }
            }
        } else {
            for (SchemaItem schemaItem : schemaItems) {
                if (schemaItem.getPath().equals(relatedPropertyPath)) {
                    SchemaItem extendedSchemaItem = new SchemaItem();
                    extendedSchemaItem.setPath(configuration.getName());
                    if (configuration.isExtractFirstMatch()) {
                        extendedSchemaItem.setKind(schemaItem.getKind());
                    } else {
                        extendedSchemaItem.setKind("[]" + schemaItem.getKind());
                    }
                    extendedSchemaItems.add(extendedSchemaItem);
                }
            }
        }

        return extendedSchemaItems;
    }

    private List<String> getRelatedObjectIds(Map<String, Object> dataMap, RelatedObjectsSpec relatedObjectsSpec) {
        if (dataMap == null || dataMap.isEmpty() || relatedObjectsSpec == null || !relatedObjectsSpec.isValid())
            return new ArrayList<>();

        String relatedObjectId = PropertyUtil.removeDataPrefix(relatedObjectsSpec.getRelatedObjectID());
        Map<String, Object> propertyValues = getPropertyValues(dataMap, relatedObjectId, relatedObjectsSpec, relatedObjectsSpec.hasValidCondition(), false);

        List<String> relatedObjectIds = new ArrayList<>();
        if (propertyValues.containsKey(relatedObjectId)) {
            Object value = propertyValues.get(relatedObjectId);
            if (value instanceof List) {
                for (Object obj : (List) value) {
                    relatedObjectIds.add(obj.toString());
                }
            } else {
                relatedObjectIds.add(value.toString());
            }

        }
        return relatedObjectIds;
    }

    private Map<String, Object> getPropertyValues(Map<String, Object> dataMap, ValueExtraction valueExtraction, boolean isExtractFirstMatch) {
        if (dataMap == null || dataMap.isEmpty() || valueExtraction == null || !valueExtraction.isValid())
            return new HashMap<>();

        String valuePath = PropertyUtil.removeDataPrefix(valueExtraction.getValuePath());
        return getPropertyValues(dataMap, valuePath, valueExtraction, valueExtraction.hasValidCondition(), isExtractFirstMatch);
    }

    private Map<String, Object> getPropertyValues(Map<String, Object> dataMap, String valuePath, RelatedCondition relatedCondition, boolean hasValidCondition, boolean isExtractFirstMatch) {
        Map<String, Object> propertyValues = new HashMap<>();
        if (valuePath.contains(ARRAY_SYMBOL)) { // Nested
            String conditionProperty = null;
            List<String> conditionMatches = null;
            if (hasValidCondition) {
                int idx = relatedCondition.getRelatedConditionProperty().lastIndexOf(PROPERTY_DELIMITER);
                conditionProperty = relatedCondition.getRelatedConditionProperty().substring(idx + 1);
                conditionMatches = relatedCondition.getRelatedConditionMatches();
            }

            List<Object> valueList = getPropertyValuesFromNestedObjects(dataMap, valuePath, conditionProperty, conditionMatches, hasValidCondition, isExtractFirstMatch);
            if (!valueList.isEmpty()) {
                if (isExtractFirstMatch) {
                    propertyValues.put(valuePath, valueList.get(0));
                } else {
                    propertyValues.put(valuePath, valueList);
                }
            }
        } else { // Flatten
            for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
                String key = entry.getKey();
                if (key.equals(valuePath) || key.startsWith(valuePath + PROPERTY_DELIMITER)) {
                    if (isExtractFirstMatch) {
                        propertyValues.put(key, entry.getValue());
                    } else {
                        List<Object> values = new ArrayList<>();
                        values.add(entry.getValue());
                        propertyValues.put(key, values);
                    }
                }
            }
        }

        return propertyValues;
    }

    private List<Object> getPropertyValuesFromNestedObjects(Map<String, Object> dataMap, String valuePath, String conditionProperty, List<String> conditionMatches, boolean hasCondition, boolean isExtractFirstMatch) {
        Set<Object> propertyValues = new HashSet<>();

        if (valuePath.contains(PROPERTY_DELIMITER)) {
            int idx = valuePath.indexOf(PROPERTY_DELIMITER);
            String prePath = valuePath.substring(0, idx);
            String postPath = valuePath.substring(idx + 1);
            try {
                if (prePath.endsWith(ARRAY_SYMBOL)) {
                    prePath = prePath.replace(ARRAY_SYMBOL, "");
                    if (dataMap.containsKey(prePath) && dataMap.get(prePath) != null) {
                        List<Map<String, Object>> nestedObjects = (List<Map<String, Object>>) dataMap.get(prePath);
                        for (Map<String, Object> nestedObject : nestedObjects) {
                            List<Object> valueList = getPropertyValuesFromNestedObjects(nestedObject, postPath, conditionProperty, conditionMatches, hasCondition, isExtractFirstMatch);
                            if (valueList != null && !valueList.isEmpty()) {
                                propertyValues.addAll(valueList);
                                if (isExtractFirstMatch)
                                    break;
                            }
                        }
                    }
                } else {
                    if (dataMap.containsKey(prePath) && dataMap.get(prePath) != null) {
                        Map<String, Object> nestedObject = (Map<String, Object>) dataMap.get(prePath);
                        List<Object> valueList = getPropertyValuesFromNestedObjects(nestedObject, postPath, conditionProperty, conditionMatches, hasCondition, isExtractFirstMatch);
                        if (valueList != null && !valueList.isEmpty()) {
                            propertyValues.addAll(valueList);
                        }
                    }
                }
            } catch (Exception ex) {
                //Ignore cast exception
            }
        } else if (dataMap.containsKey(valuePath) && dataMap.get(valuePath) != null) {
            Object extractPropertyValue = dataMap.get(valuePath);
            if (hasCondition) {
                if (dataMap.containsKey(conditionProperty) && dataMap.get(conditionProperty) != null) {
                    String conditionPropertyValue = dataMap.get(conditionProperty).toString();
                    if (conditionMatches.contains(conditionPropertyValue) && extractPropertyValue != null) {
                        propertyValues.add(extractPropertyValue);
                    }
                }
            } else {
                propertyValues.add(extractPropertyValue);
            }
        }

        List<Object> propertyValueList = new ArrayList<>(propertyValues);
        Collections.sort(propertyValueList, Comparator.comparing(Object::toString));
        return propertyValueList;
    }


    private void updateAssociatedParentRecords(String ancestors, String childKind, List<String> processedIds) {
        if (processedIds == null || processedIds.isEmpty())
            return;

        List<ParentChildRelatedObjectsSpec> specList = getParentChildRelatedObjectsSpecs(childKind);
        Set ancestorSet = new HashSet<>(Arrays.asList(ancestors.split(ANCESTRY_KINDS_DELIMITER)));
        for (ParentChildRelatedObjectsSpec spec : specList) {
            List<String> parentIds = getUniqueParentIds(childKind, processedIds, spec.getParentObjectId());
            if (parentIds.isEmpty())
                continue;

            final int limit = configurationProperties.getStorageRecordsByKindBatchSize();
            Map<String, List<String>> parentKindIds = resolveKindIds(spec.getParentKind(), parentIds);
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

    private void updateAssociatedChildrenRecords(String ancestors, List<String> processedIds) {
        if (processedIds == null || processedIds.isEmpty())
            return;

        String query = String.format("data.%s:(%s)", ASSOCIATED_IDENTITIES_PROPERTY, buildIdsFilter(processedIds));
        String kind = WILD_CARD_KIND;
        for (String ancestryKind : ancestors.split(ANCESTRY_KINDS_DELIMITER)) {
            if (!ancestryKind.trim().isEmpty()) {
                // Exclude the kinds in the ancestryKinds to prevent circular chasing
                kind += ",-" + ancestryKind.trim();
            }
        }

        final int limit = configurationProperties.getStorageRecordsByKindBatchSize();
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setKind(kind);
        searchRequest.setQuery(query);
        searchRequest.setReturnedFields(Arrays.asList("kind", "id"));
        List<RecordInfo> recordInfos = new ArrayList<>();
        for (SearchRecord record : getAllRecords(searchRequest)) {
            RecordInfo recordInfo = new RecordInfo();
            recordInfo.setKind(record.getKind());
            recordInfo.setId(record.getId());
            recordInfo.setOp(OperationType.update.getValue());
            recordInfos.add(recordInfo);

            if (recordInfos.size() >= limit) {
                createWorkerTask(ancestors, recordInfos);
                recordInfos = new ArrayList<>();
            }
        }
        if (!recordInfos.isEmpty()) {
            createWorkerTask(ancestors, recordInfos);
        }
    }

    private List<ParentChildRelatedObjectsSpec> getParentChildRelatedObjectsSpecs(String childKind) {
        String dataPartitionId = requestInfo.getHeaders().getPartitionId();
        final String kindWithMajor = PropertyUtil.getKindWithMajor(childKind);
        String key = dataPartitionId + " | " + kindWithMajor;

        List<ParentChildRelatedObjectsSpec> specsList = parentChildRelatedObjectsSpecsCache.get(key);
        if (specsList == null) {
            Set<ParentChildRelatedObjectsSpec> specs = new HashSet<>();
            List<PropertyConfigurations> configurationsList = searchParentKindConfigurations((kindWithMajor));
            for (PropertyConfigurations configurations : configurationsList) {
                for (PropertyConfiguration configuration : configurations.getConfigurations()) {
                    PropertyPath propertyPath = configuration.getPaths().stream().filter(p ->
                                    p.hasValidRelatedObjectsSpec() &&
                                            p.getRelatedObjectsSpec().isParentToChildren() &&
                                            p.getRelatedObjectsSpec().getRelatedObjectKind().contains(kindWithMajor))
                            .findFirst().orElse(null);
                    if (propertyPath != null) {
                        ParentChildRelatedObjectsSpec spec = new ParentChildRelatedObjectsSpec();
                        spec.setParentKind(configurations.getCode());
                        spec.setChildKind(kindWithMajor);
                        spec.setParentObjectId(propertyPath.getRelatedObjectsSpec().getRelatedObjectID());
                        specs.add(spec);
                    }
                }
            }

            specsList = new ArrayList<>(specs);
            parentChildRelatedObjectsSpecsCache.put(key, specsList);
        }

        return specsList;
    }

    private void createWorkerTask(String ancestors, List<RecordInfo> recordInfos) {
        if (recordInfos == null || recordInfos.isEmpty())
            return;

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

    private String searchConcreteKind(String kindWithMajor) {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setKind(kindWithMajor + "*");
        searchRequest.setReturnedFields(Arrays.asList("kind"));
        SearchRecord searchRecord = getFirstRecord(searchRequest);
        if (searchRecord != null) {
            searchRecord.getKind();
        }
        return null;
    }

    private PropertyConfigurations searchConfigurations(String kind) {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setKind(INDEX_PROPERTY_PATH_CONFIGURATION_KIND);
        String query = String.format("data.Code: \"%s\"", kind);
        searchRequest.setQuery(query);
        for (SearchRecord searchRecord : getAllRecords(searchRequest)) {
            try {
                String data = objectMapper.writeValueAsString(searchRecord.getData());
                PropertyConfigurations configurations = objectMapper.readValue(data, PropertyConfigurations.class);
                if (kind.equals(configurations.getCode())) {
                    return configurations;
                }
            } catch (JsonProcessingException e) {
                jaxRsDpsLog.error("failed to deserialize PropertyConfigurations object", e);
            }
        }
        return null;
    }

    private List<PropertyConfigurations> searchParentKindConfigurations(String childKind) {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setKind(INDEX_PROPERTY_PATH_CONFIGURATION_KIND);
        String query = String.format(PARENT_CHILDREN_CONFIGURATION_QUERY_FORMAT, childKind);
        searchRequest.setQuery(query);
        List<PropertyConfigurations> configurationsList = new ArrayList<>();
        for (SearchRecord searchRecord : getAllRecords(searchRequest)) {
            try {
                String data = objectMapper.writeValueAsString(searchRecord.getData());
                PropertyConfigurations configurations = objectMapper.readValue(data, PropertyConfigurations.class);
                configurationsList.add(configurations);
            } catch (JsonProcessingException e) {
                jaxRsDpsLog.error("failed to deserialize PropertyConfigurations object", e);
            }
        }
        return configurationsList;
    }

    private SearchRecord searchRelatedRecord(String relatedObjectKind, String relatedObjectId) {
        String kind = PropertyUtil.isConcreteKind(relatedObjectKind) ? relatedObjectKind : relatedObjectKind + "*";
        String id = PropertyUtil.removeIdPostfix(relatedObjectId);
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setKind(kind);
        String query = String.format("id: \"%s\"", id);
        searchRequest.setQuery(query);
        return getFirstRecord(searchRequest);
    }

    private Map<String, List<String>> resolveKindIds(String majorKind, List<String> ids) {
        Map<String, List<String>> kindIds = new HashMap<>();
        SearchRequest searchRequest = new SearchRequest();
        String kind = PropertyUtil.isConcreteKind(majorKind) ? majorKind : majorKind + "*";
        searchRequest.setKind(kind);
        String query = String.format("id: (%s)", buildIdsFilter(ids));
        searchRequest.setReturnedFields(Arrays.asList("kind", "id"));
        searchRequest.setQuery(query);
        for (SearchRecord record : getAllRecords(searchRequest)) {
            if (kindIds.containsKey(record.getKind())) {
                kindIds.get(record.getKind()).add(record.getId());
            } else {
                List<String> idList = new ArrayList<>();
                idList.add(record.getId());
                kindIds.put(record.getKind(), idList);
            }
        }
        return kindIds;
    }

    private List<String> getUniqueParentIds(String childKind, List<String> childRecordIds, String parentObjectId) {
        Set<String> parentIds = new HashSet<>();
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setKind(childKind);
        String query = String.format("id: (%s)", buildIdsFilter(childRecordIds));
        searchRequest.setReturnedFields(Arrays.asList(parentObjectId));
        searchRequest.setQuery(query);
        parentObjectId = PropertyUtil.removeDataPrefix(parentObjectId);
        for (SearchRecord record : getAllRecords(searchRequest)) {
            if (record.getData().containsKey(parentObjectId)) {
                Object id = record.getData().get(parentObjectId);
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
        return getAllRecords(searchRequest);
    }

    private String buildIdsFilter(List<String> ids) {
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

    /*
      It is assumed that the search request in this method won't return millions of records
     */
    private List<SearchRecord> getAllRecords(SearchRequest searchRequest) {
        searchRequest.setLimit(MAX_SEARCH_LIMIT);
        List<SearchRecord> allRecords = new ArrayList<>();
        boolean done = false;
        try {
            while (!done) {
                SearchResponse searchResponse = searchService.queryWithCursor(searchRequest);
                List<SearchRecord> results = searchResponse.getResults();
                if (results != null) {
                    allRecords.addAll(results);
                }
                if (searchResponse.getCursor() != null && results.size() == MAX_SEARCH_LIMIT) {
                    searchRequest.setCursor(searchResponse.getCursor());
                } else {
                    done = true;
                }
            }
        } catch (URISyntaxException e) {
            // TODO: log the error
        }
        return allRecords;
    }

    private SearchRecord getFirstRecord(SearchRequest searchRequest) {
        searchRequest.setLimit(1);
        try {
            SearchResponse searchResponse = searchService.query(searchRequest);
            List<SearchRecord> results = searchResponse.getResults();
            if (results != null && !results.isEmpty()) {
                return results.get(0);
            }
        } catch (URISyntaxException e) {
            // TODO: log the error
        }
        return null;
    }
}
