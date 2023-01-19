package org.opengroup.osdu.indexer.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import org.opengroup.osdu.indexer.model.SearchRecord;
import org.opengroup.osdu.indexer.model.SearchRequest;
import org.opengroup.osdu.indexer.model.SearchResponse;
import org.opengroup.osdu.indexer.model.indexproperty.PropertyConfigurations;
import org.opengroup.osdu.indexer.schema.converter.interfaces.IPropertyConfigurationsCache;
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

    @Inject
    private IPropertyConfigurationsCache propertyConfigurationCache;
    @Inject
    private SearchService searchService;

    public PropertyConfigurations getPropertyConfiguration(String kind) {
        if (Strings.isNullOrEmpty(kind))
            return null;

        List<String> kinds = Arrays.asList(kind, getKindWithMajor(kind)); // Specified version of kind first
        PropertyConfigurations configuration = null;
        for(String kd: kinds) {
            configuration =(PropertyConfigurations) propertyConfigurationCache.get(kd);
            if(configuration != null) {
                if(Strings.isNullOrEmpty(configuration.getCode())) {
                    configuration = null; //reset
                }
            }
            else {
                configuration = searchConfigurations(kd);
                if(configuration != null) {
                    propertyConfigurationCache.put(kd, configuration);
                }
                else {
                    propertyConfigurationCache.put(kd, new PropertyConfigurations());
                }
            }

            if(configuration != null)
                break;
        }

        return configuration;
    }

    public String resolveConcreteKind(String kind) {
        if(isConcreteKind(kind))
            return kind;

        //TODO: cache the mapping
        String concreteKind = searchConcreteKind(kind);
        return concreteKind;
    }

    public SearchRecord getRelatedRecord(String relatedObjectKind, String relatedObjectId) {
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
                SearchRecord searchRecord = results.get(0);

                // Cache it
                return searchRecord;
            }
        } catch (URISyntaxException e) {
            // TODO: log the error
        }
        return null;
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
}
