package org.opengroup.osdu.indexer.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.indexer.OperationType;
import org.opengroup.osdu.core.common.model.indexer.RecordInfo;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.core.common.model.storage.SchemaItem;
import org.opengroup.osdu.core.common.provider.interfaces.IRequestInfo;
import org.opengroup.osdu.indexer.cache.KindCache;
import org.opengroup.osdu.indexer.cache.PropertyConfigurationsCache;
import org.opengroup.osdu.indexer.cache.RelatedObjectCache;
import org.opengroup.osdu.indexer.config.IndexerConfigurationProperties;
import org.opengroup.osdu.indexer.model.Constants;
import org.opengroup.osdu.indexer.model.SearchRecord;
import org.opengroup.osdu.indexer.model.SearchRequest;
import org.opengroup.osdu.indexer.model.SearchResponse;
import org.opengroup.osdu.indexer.model.indexproperty.PropertyConfigurations;
import org.opengroup.osdu.indexer.service.SearchService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.net.URISyntaxException;
import java.util.*;

@Component
public class PropertyConfigurationsUtil {
    public static final String ASSOCIATED_IDENTITIES_PROPERTY = "AssociatedIdentities";
    private static final String ASSOCIATED_IDENTITIES_PROPERTY_STORAGE_FORMAT_TYPE = "[]string";
    private static final String WILD_CARD_KIND = "*:*:*:*";
    private static final String INDEX_PROPERTY_PATH_CONFIGURATION_KIND = "osdu:wks:reference-data--IndexPropertyPathConfiguration:*";
    private static final String ANCESTRY_KINDS_DELIMITER = ",";
    private static final String EMPTY_CODE = "__EMPTY_CODE__";
    private static final int DEFAULT_SEARCH_LIMIT = 100;

    private final Gson gson = new Gson();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PropertyConfigurations EMPTY_CONFIGURATIONS = new PropertyConfigurations() {{ setCode(EMPTY_CODE); }};


    @Inject
    private IndexerConfigurationProperties configurationProperties;
    @Inject
    private PropertyConfigurationsCache propertyConfigurationCache;
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

    public PropertyConfigurations getPropertyConfiguration(String kind) {
        if (Strings.isNullOrEmpty(kind))
            return null;

        String dataPartitionId = requestInfo.getHeaders().getPartitionId();
        List<String> kinds = Arrays.asList(kind, getKindWithMajor(kind)); // Specified version of kind first
        for(String kd: kinds) {
            String key = dataPartitionId + " | " + kd;

            PropertyConfigurations configuration = propertyConfigurationCache.get(key);
            if(configuration != null) {
                configuration = propertyConfigurationCache.get(key);
            }
            else {
                configuration = searchConfigurations(kd);
                if(configuration != null) {
                    propertyConfigurationCache.put(key, configuration);
                }
                else {
                    // It is common that a kind does not have extended property. So we need to cache an empty configuration
                    // to avoid unnecessary search
                    propertyConfigurationCache.put(key, EMPTY_CONFIGURATIONS);
                }
            }

            if(configuration != null && !isEmptyConfiguration(configuration)) {
                return configuration;
            }
        }

        return null;
    }

    public String resolveConcreteKind(String kind) {
        if(Strings.isNullOrEmpty(kind)) {
            return null;
        }

        if(isConcreteKind(kind)) {
            return kind;
        }

        if(kindCache.containsKey(kind)) {
            return kindCache.get(kind);
        }
        else {
            String concreteKind = searchConcreteKind(kind);
            if (!Strings.isNullOrEmpty(concreteKind)) {
                kindCache.put(kind, concreteKind);
            }
            return concreteKind;
        }
    }

    public Map<String, Object> getRelatedObject(String relatedObjectKind, String relatedObjectId) {
        if(Strings.isNullOrEmpty(relatedObjectKind) || Strings.isNullOrEmpty(relatedObjectId)) {
            return null;
        }

        String key = (relatedObjectId.endsWith(":"))
                    ? relatedObjectId.substring(0, relatedObjectId.length() -1)
                    : relatedObjectId;

        Map<String, Object> relatedObject = relatedObjectCache.get(key);
        if(relatedObject != null) {
            return relatedObject;
        }
        else {
            SearchRecord searchRecord = searchRelatedRecord(relatedObjectKind, relatedObjectId);
            if(searchRecord != null) {
                relatedObjectCache.put(key, searchRecord.getData());
            }
            return searchRecord.getData();
        }
    }

    public void setRelatedObject(String relatedObjectId, Map<String, Object> relatedObject) {
        if(Strings.isNullOrEmpty(relatedObjectId) || relatedObject == null) {
            return;
        }

        String key = (relatedObjectId.endsWith(":"))
                ? relatedObjectId.substring(0, relatedObjectId.length() -1)
                : relatedObjectId;

        relatedObjectCache.put(key, relatedObject);
    }

    public String removeColumnPostfix(String relatedObjectId) {
        if(relatedObjectId != null && relatedObjectId.endsWith(":")) {
            relatedObjectId = relatedObjectId.substring(0, relatedObjectId.length() - 1);
        }

        return relatedObjectId;
    }

    public SchemaItem createAssociatedIdentitiesSchemaItem() {
        SchemaItem extendedSchemaItem = new SchemaItem();
        extendedSchemaItem.setPath(ASSOCIATED_IDENTITIES_PROPERTY);
        extendedSchemaItem.setKind(ASSOCIATED_IDENTITIES_PROPERTY_STORAGE_FORMAT_TYPE);
        return extendedSchemaItem;
    }

    public void updateAssociatedRecords(RecordChangedMessages message, Map<String, List<String>> processedRecordIds) {
        if(processedRecordIds == null || processedRecordIds.isEmpty())
            return;

        Map<String, String> attributes = message.getAttributes();
        DpsHeaders headers = this.requestInfo.getHeadersWithDwdAuthZ();
        attributes.put(DpsHeaders.ACCOUNT_ID, headers.getAccountId());
        attributes.put(DpsHeaders.DATA_PARTITION_ID, headers.getPartitionIdWithFallbackToAccountId());
        attributes.put(DpsHeaders.CORRELATION_ID, headers.getCorrelationId());
        final String ancestors = attributes.containsKey(Constants.ANCESTRY_KINDS)? attributes.get(Constants.ANCESTRY_KINDS) : "";
        for (Map.Entry<String, List<String>> entry :  processedRecordIds.entrySet()) {
            String kind = entry.getKey();
            String updatedAncestors = ancestors.isEmpty()? kind : ancestors + ANCESTRY_KINDS_DELIMITER + kind;
            attributes.put(Constants.ANCESTRY_KINDS, updatedAncestors);
            updateAssociatedRecords(attributes, entry.getValue());
        }
    }

    private void updateAssociatedRecords(Map<String, String> attributes, List<String> associatedRecordIds) {
        if(associatedRecordIds == null || associatedRecordIds.isEmpty())
            return;

        StringBuilder stringBuilder = new StringBuilder();
        for(String id : associatedRecordIds) {
            if(stringBuilder.length() > 0) {
                stringBuilder.append(",");
            }
            stringBuilder.append("\"");
            stringBuilder.append(id);
            stringBuilder.append("\"");
        }
        String query = String.format("data.%s:(%s)", ASSOCIATED_IDENTITIES_PROPERTY, stringBuilder.toString());
        String kind = WILD_CARD_KIND;
        for(String ancestryKind: attributes.get(Constants.ANCESTRY_KINDS).split(ANCESTRY_KINDS_DELIMITER)) {
            if(!Strings.isNullOrEmpty(ancestryKind)) {
                // Exclude the kinds in the ancestryKinds to prevent circular chasing
                kind += ",-" + ancestryKind.trim();
            }
        }

        final int limit = configurationProperties.getStorageRecordsByKindBatchSize();
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setKind(kind);
        searchRequest.setQuery(query);
        searchRequest.setLimit(limit);
        searchRequest.setReturnedFields(Arrays.asList("kind", "id"));
        int offset = 0;
        try {
            while(true) {
                SearchResponse searchResponse = searchService.query(searchRequest);
                List<SearchRecord> results = searchResponse.getResults();
                if(results != null && !results.isEmpty()) {
                    createWorkerTask(attributes, results);
                }

                if(results == null || results.size() < limit)
                    break;

                offset += results.size();
                searchRequest.setOffset(offset);
            }
        } catch (URISyntaxException e) {
            // TODO: log the error
        }
    }

    private void createWorkerTask(Map<String, String> attributes, List<SearchRecord> results) {
        if(results == null || results.isEmpty())
            return;

        List<RecordInfo> data = new ArrayList<>();
        for (SearchRecord record: results) {
            RecordInfo recordInfo = new RecordInfo();
            recordInfo.setKind(record.getKind());
            recordInfo.setId(record.getId());
            recordInfo.setOp(OperationType.update.getValue());
            data.add(recordInfo);
        }

        RecordChangedMessages recordChangedMessages = RecordChangedMessages.builder().data(gson.toJson(data)).attributes(attributes).build();
        String recordChangedMessagePayload = gson.toJson(recordChangedMessages);
        this.indexerQueueTaskBuilder.createWorkerTask(recordChangedMessagePayload, 0L, this.requestInfo.getHeadersWithDwdAuthZ());
    }

    private boolean isEmptyConfiguration(PropertyConfigurations propertyConfigurations) {
        return propertyConfigurations != null && EMPTY_CODE.equals(propertyConfigurations.getCode());
    }

    private boolean isConcreteKind(String kind) {
        int index = kind.lastIndexOf(":");
        String version = kind.substring(index + 1);
        String[] subVersions = version.split("\\.");
        return (subVersions.length == 3);
    }

    private String getKindWithMajor(String kind) {
        int index = kind.lastIndexOf(":");
        String kindWithMajor = kind.substring(0, index) + ":";

        String version = kind.substring(index + 1);
        String[] subVersions = version.split("\\.");
        if(subVersions.length > 0) {
            kindWithMajor += subVersions[0] + ".";
        }

        return kindWithMajor;
    }

    private String searchConcreteKind(String kindWithMajor) {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setKind(kindWithMajor + "*");
        searchRequest.setLimit(DEFAULT_SEARCH_LIMIT);
        searchRequest.setReturnedFields(Arrays.asList("kind"));
        try {
            SearchResponse searchResponse = searchService.query(searchRequest);
            List<SearchRecord> results = searchResponse.getResults();
            if(results != null && !results.isEmpty())
            {
                //TODO: get the best match
                SearchRecord searchRecord = results.get(0);
                return searchRecord.getKind();
            }
        } catch (URISyntaxException e) {
            // TODO: log the error
        }
        return null;
    }

    private PropertyConfigurations searchConfigurations(String kind) {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setKind(INDEX_PROPERTY_PATH_CONFIGURATION_KIND);
        searchRequest.setLimit(DEFAULT_SEARCH_LIMIT);
        String query = String.format("data.Code: \"%s\"",kind);
        searchRequest.setQuery(query);
        try {
            SearchResponse searchResponse = searchService.query(searchRequest);
            List<SearchRecord> results = searchResponse.getResults();
            for(SearchRecord searchRecord : results) {
                String data = objectMapper.writeValueAsString(searchRecord.getData());
                PropertyConfigurations configurations = objectMapper.readValue(data, PropertyConfigurations.class);
                if(kind.equals(configurations.getCode())) {
                    return configurations;
                }
            }
        } catch (URISyntaxException e) {
            // TODO: log the error
        }
        catch (JsonProcessingException e) {
            // TODO: log the error
        }

        return null;
    }

    private SearchRecord searchRelatedRecord(String relatedObjectKind, String relatedObjectId) {
        String kind = isConcreteKind(relatedObjectKind)? relatedObjectKind : relatedObjectKind + "*";
        String id = removeColumnPostfix(relatedObjectId);
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setKind(kind);
        String query = String.format("id: \"%s\"",id);
        searchRequest.setQuery(query);
        searchRequest.setLimit(DEFAULT_SEARCH_LIMIT);
        try {
            SearchResponse searchResponse = searchService.query(searchRequest);
            List<SearchRecord> results = searchResponse.getResults();
            if(results != null && !results.isEmpty())
            {
                //TODO: get the best match
                return results.get(0);
            }
        } catch (URISyntaxException e) {
            // TODO: log the error
        }
        return null;
    }



}
