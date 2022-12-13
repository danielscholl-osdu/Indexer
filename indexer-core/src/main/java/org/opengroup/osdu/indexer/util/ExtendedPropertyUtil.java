package org.opengroup.osdu.indexer.util;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.opengroup.osdu.core.common.model.indexer.Records;
import org.opengroup.osdu.indexer.model.IndexPropertyPath;
import org.opengroup.osdu.indexer.model.IndexPropertyPathConfiguration;
import org.opengroup.osdu.indexer.schema.converter.tags.PropertyConfiguration;
import org.opengroup.osdu.indexer.service.StorageService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class ExtendedPropertyUtil {
    private static final Gson gson = new Gson();

    @Inject
    private StorageService storageService;

    public Object getExtendedPropertyValue(PropertyConfiguration propertyConfiguration, Map<String, Object> dataCollectorMap) {
        if(propertyConfiguration == null || Strings.isNullOrEmpty(propertyConfiguration.getConfiguration()))
            return null;

        String configurationId = propertyConfiguration.getConfiguration().trim();
        if(configurationId.endsWith(":"))
            configurationId = configurationId.substring(0, configurationId.length() -1);

        try {
            Records records = storageService.getStorageRecords(Arrays.asList(configurationId));
            if(records == null || records.getTotalRecordCount() < 1)
                return null;
            Records.Entity entity = records.getRecords().get(0);
            String jsonStr = gson.toJson(entity.getData());
            IndexPropertyPathConfiguration indexPropertyPathConfiguration = gson.fromJson(jsonStr, IndexPropertyPathConfiguration.class);

            if(indexPropertyPathConfiguration.isArray()) {
                List<Object> values = new ArrayList<>();
                for(IndexPropertyPath path: indexPropertyPathConfiguration.getPaths()) {
                    Object value = getValueFromRelatedObject(path, dataCollectorMap);
                    if(value != null)
                        values.add(value);
                }
                return values;
            }
            else {
                  for(IndexPropertyPath path: indexPropertyPathConfiguration.getPaths()) {
                      if(path.getRelatedObjectKind() != null) {
                          Object value = getValueFromRelatedObject(path, dataCollectorMap);
                          if(value != null)
                              return value;
                      }
                  }
            }

        } catch (URISyntaxException e) {
            // TBD
        }

        return null;
    }

    private Object getValueFromRelatedObject(IndexPropertyPath path, Map<String, Object> dataCollectorMap) {
        String relatedObjectIdPath = path.getRelatedObjectID();
        if(relatedObjectIdPath.startsWith("data."))
            relatedObjectIdPath = relatedObjectIdPath.substring("data.".length());
        if(!dataCollectorMap.containsKey(relatedObjectIdPath) || dataCollectorMap.get(relatedObjectIdPath) == null) {
            return null;
        }

        try {
            String relatedObjectId = (String)dataCollectorMap.get(relatedObjectIdPath);
            if(relatedObjectId.endsWith(":"))
                relatedObjectId = relatedObjectId.substring(0, relatedObjectId.length() - 1);
            Records records = storageService.getStorageRecords(Arrays.asList(relatedObjectId));
            if(records == null || records.getTotalRecordCount() < 1)
                return null;

            String valuePath = path.getValuePath();
            if(valuePath.startsWith("data."))
                valuePath = valuePath.substring("data.".length());
            Records.Entity entity = records.getRecords().get(0);
            if(entity.getKind().startsWith(path.getRelatedObjectKind())) {
                return retrieveValue(valuePath, entity.getData());
            }
        } catch (URISyntaxException e) {
            // TBD
        }

        return null;
    }

    private Object retrieveValue(String valuePath, Map<String, Object> valuesMap) {
        if(valuePath.contains(".")) {
            int index = valuePath.indexOf(".");
            String prefix = valuePath.substring(0, index);

            if(valuesMap.containsKey(prefix) && valuesMap.get(prefix) instanceof Map) {
                valuePath = valuePath.substring(index + 1);
                valuesMap = (Map<String, Object>)valuesMap.get(prefix);
                return retrieveValue(valuePath, valuesMap);
            }
            else
                return null;
        }
        else if(valuesMap.containsKey(valuePath)) {
            return valuesMap.get(valuePath);
        }
        return null;
    }
}
