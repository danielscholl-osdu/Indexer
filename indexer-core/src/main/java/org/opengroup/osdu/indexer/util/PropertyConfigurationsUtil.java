package org.opengroup.osdu.indexer.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import org.opengroup.osdu.indexer.cache.KindCache;
import org.opengroup.osdu.indexer.cache.PropertyConfigurationsCache;
import org.opengroup.osdu.indexer.cache.SearchRecordCache;
import org.opengroup.osdu.indexer.model.SearchRecord;
import org.opengroup.osdu.indexer.model.SearchRequest;
import org.opengroup.osdu.indexer.model.SearchResponse;
import org.opengroup.osdu.indexer.model.indexproperty.PropertyConfigurations;
import org.opengroup.osdu.indexer.service.SearchService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

@Component
public class PropertyConfigurationsUtil {
    private static final String INDEX_PROPERTY_PATH_CONFIGURATION_KIND = "osdu:wks:reference-data--IndexPropertyPathConfiguration:*";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PropertyConfigurations emptyConfigurations = new PropertyConfigurations();

    @Inject
    private PropertyConfigurationsCache propertyConfigurationCache;
    @Inject
    private KindCache kindCache;
    @Inject
    private SearchRecordCache searchRecordCache;

    @Inject
    private SearchService searchService;

    public PropertyConfigurations getPropertyConfiguration(String kind) {
        if (Strings.isNullOrEmpty(kind))
            return null;

        List<String> kinds = Arrays.asList(kind, getKindWithMajor(kind)); // Specified version of kind first
        for(String kd: kinds) {
            PropertyConfigurations configuration = null;
            if(propertyConfigurationCache.containsKey(kd)) {
                if(!isEmptyConfiguration(propertyConfigurationCache.get(kd)))
                    configuration = propertyConfigurationCache.get(kd);
            }
            else {
                configuration = searchConfigurations(kd);
                if(configuration != null) {
                    propertyConfigurationCache.put(kd, configuration);
                }
                else {
                    // It is common that a kind does not have extended property. So we need to cache an empty configuration
                    // to avoid unnecessary search
                    propertyConfigurationCache.put(kd, emptyConfigurations);
                }
            }

            if(configuration != null) {
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

    public SearchRecord getRelatedRecord(String relatedObjectKind, String relatedObjectId) {
        if(Strings.isNullOrEmpty(relatedObjectKind) || Strings.isNullOrEmpty(relatedObjectId)) {
            return null;
        }

        String key =  relatedObjectKind + ":" + relatedObjectId;
        if(searchRecordCache.containsKey(key)) {
            return searchRecordCache.get(key);
        }
        else {
            SearchRecord searchRecord = searchRelatedRecord(relatedObjectKind, relatedObjectId);
            if(searchRecord != null) {
                searchRecordCache.put(key, searchRecord);
            }
            return searchRecord;
        }
    }

    private boolean isEmptyConfiguration(PropertyConfigurations propertyConfigurations) {
        return propertyConfigurations != null && Strings.isNullOrEmpty(propertyConfigurations.getCode());
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
        searchRequest.setLimit(10);
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
        searchRequest.setLimit(10);
        String query = String.format("data.Code: \"%s\"",kind);
        searchRequest.setQuery(query);
        PropertyConfigurations configurations = null;
        try {
            SearchResponse searchResponse = searchService.query(searchRequest);
            List<SearchRecord> results = searchResponse.getResults();
            if(results != null && !results.isEmpty())
            {
                //TODO: get the best match
                SearchRecord searchRecord = results.get(0);
                String data = objectMapper.writeValueAsString(searchRecord.getData());
                configurations = objectMapper.readValue(data, PropertyConfigurations.class);
            }
        } catch (URISyntaxException e) {
            // TODO: log the error
        }
        catch (JsonProcessingException e) {
            // TODO: log the error
        }

        return configurations;
    }

    private SearchRecord searchRelatedRecord(String relatedObjectKind, String relatedObjectId) {
        String kind = isConcreteKind(relatedObjectKind)? relatedObjectKind : relatedObjectKind + "*";
        String id = relatedObjectId.endsWith(":")? relatedObjectId.substring(0, relatedObjectId.length() - 1) : relatedObjectId;
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setKind(kind);
        String query = String.format("id: \"%s\"",id);
        searchRequest.setQuery(query);
        searchRequest.setLimit(10);
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
