package org.opengroup.osdu.indexer.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import org.opengroup.osdu.core.common.model.indexer.IndexSchema;
import org.opengroup.osdu.core.common.model.indexer.Records;
import org.opengroup.osdu.core.common.model.storage.Schema;
import org.opengroup.osdu.core.common.model.storage.SchemaItem;
import org.opengroup.osdu.indexer.model.IndexPropertyPath;
import org.opengroup.osdu.indexer.model.IndexPropertyPathConfiguration;
import org.opengroup.osdu.indexer.provider.interfaces.ISchemaCache;
import org.opengroup.osdu.indexer.schema.converter.tags.PropertyConfiguration;
import org.opengroup.osdu.indexer.service.IndexSchemaService;
import org.opengroup.osdu.indexer.service.SchemaService;
import org.opengroup.osdu.indexer.service.StorageIndexerPayloadMapper;
import org.opengroup.osdu.indexer.service.StorageService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ExtendedPropertyUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    private StorageService storageService;

    @Inject
    private ISchemaCache schemaCache;

    @Inject
    private SchemaService schemaProvider;

    @Inject
    private IndexSchemaService indexSchemaService;

    @Inject
    private StorageIndexerPayloadMapper storageIndexerPayloadMapper;

    public List<Map<String, Object>> getExtendedPropertyStorageSchema(PropertyConfiguration propertyConfiguration, String propertyRoot) {
        List<Map<String, Object>> extendedPropertySchema = new ArrayList();
        IndexPropertyPathConfiguration indexPropertyPathConfiguration = getIndexPropertyPathConfiguration((propertyConfiguration));
        if(indexPropertyPathConfiguration == null)
            return extendedPropertySchema;

        List<IndexPropertyPath> paths = indexPropertyPathConfiguration.getPaths().stream().filter(path -> hasRelatedObject(path)).collect(Collectors.toList());
        if(paths.isEmpty())
            return extendedPropertySchema;
        IndexPropertyPath indexPropertyPath = paths.get(0);

        try {
            String relatedObjectKind = indexPropertyPath.getRelatedObjectKind();
            String schema = (String) this.schemaCache.get(relatedObjectKind);
            if (Strings.isNullOrEmpty(schema)) {
                // get from storage
                schema = this.schemaProvider.getSchema(relatedObjectKind);
                if (Strings.isNullOrEmpty(schema))
                    return extendedPropertySchema;
            }
            Schema schemaObj = objectMapper.readValue(schema, Schema.class);
            String valuePath = removeDataDotPrefix(indexPropertyPath.getValuePath());
            for(SchemaItem schemaItem: schemaObj.getSchema()) {
                if(schemaItem.getPath().startsWith(valuePath)) {
                    String path = schemaItem.getPath();
                    path = path.replace(valuePath, propertyRoot);
                    Map<String, Object> map = new HashMap<>();
                    map.put("path", path);
                    map.put("kind", schemaItem.getKind());
                    extendedPropertySchema.add(map);
                }
            }
        } catch (UnsupportedEncodingException | URISyntaxException e) {
           //TODO
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return extendedPropertySchema;
    }

    public Map<String, Object> getExtendedPropertyValue(String propertyRootPath, PropertyConfiguration propertyConfiguration, Map<String, Object> dataCollectorMap) {
        IndexPropertyPathConfiguration indexPropertyPathConfiguration = getIndexPropertyPathConfiguration((propertyConfiguration));
        if(indexPropertyPathConfiguration == null)
            return new HashMap<>();

        List<IndexPropertyPath> paths = indexPropertyPathConfiguration.getPaths().stream().filter(path -> hasRelatedObject(path)).collect(Collectors.toList());
        if(indexPropertyPathConfiguration.isArray()) {

        }
        else {
            Map<String, Map<String, Object>> parentPayloads = new HashMap<>();
            for(IndexPropertyPath path: paths) {
                Map<String, Object> parentPayload = null;
                if(parentPayloads.containsKey(path.getRelatedObjectID())) {
                    parentPayload = parentPayloads.get(path.getRelatedObjectID());
                }
                else {
                    parentPayload = getParentEntityStoragePayload(path, dataCollectorMap);
                    parentPayloads.put(path.getRelatedObjectID(), parentPayload);
                }
                if(parentPayload == null || parentPayload.isEmpty())
                    continue;

                String valuePath = removeDataDotPrefix(path.getValuePath());
                Map<String, Object> value = retrieveValue(propertyRootPath, valuePath, parentPayload);
                if(!value.isEmpty())
                    return value;
            }
        }

        return new HashMap<>();
    }

    private IndexPropertyPathConfiguration getIndexPropertyPathConfiguration(PropertyConfiguration propertyConfiguration) {
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
            IndexPropertyPathConfiguration indexPropertyPathConfiguration = objectMapper.readValue(objectMapper.writeValueAsString(entity.getData()), IndexPropertyPathConfiguration.class);
            return indexPropertyPathConfiguration;
        } catch (URISyntaxException | JsonProcessingException e) {
            //TODO
            return null;
        }
    }

    private Map<String, Object> getParentEntityStoragePayload(IndexPropertyPath path, Map<String, Object> dataCollectorMap)  {

        try {
            Records.Entity parentEntity = getParentEntity(path, dataCollectorMap);
            if(parentEntity != null) {
                IndexSchema indexSchema = indexSchemaService.getIndexerInputSchema(parentEntity.getKind(), false);
                return storageIndexerPayloadMapper.mapDataPayload(indexSchema, parentEntity.getData(), parentEntity.getId());
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return new HashMap<>();
    }

    private Records.Entity getParentEntity(IndexPropertyPath path, Map<String, Object> dataCollectorMap)  {
        String relatedObjectIdPath = removeDataDotPrefix(path.getRelatedObjectID());
        if(!dataCollectorMap.containsKey(relatedObjectIdPath) || dataCollectorMap.get(relatedObjectIdPath) == null) {
            return null;
        }

        String relatedObjectId = (String)dataCollectorMap.get(relatedObjectIdPath);
        if(relatedObjectId.endsWith(":"))
            relatedObjectId = relatedObjectId.substring(0, relatedObjectId.length() - 1);
        Records records = null;
        try {
            records = storageService.getStorageRecords(Arrays.asList(relatedObjectId));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        if(records == null || records.getTotalRecordCount() < 1)
            return null;

        return records.getRecords().get(0);
    }

    private boolean hasRelatedObject(IndexPropertyPath path) {
        return (path != null && path.getRelatedObjectID() != null);
    }

    private Map<String, Object> retrieveValue(String propertyRootPath, String valuePath, Map<String, Object> parentPayload) {
        Map<String, Object> values = new HashMap<>();
        for (Map.Entry<String, Object> entry: parentPayload.entrySet()) {
            String key = entry.getKey();
            if(key.equals(valuePath) || key.startsWith(valuePath + ".")) {
                key = key.replace(valuePath, propertyRootPath);
                values.put(key, entry.getValue());
            }
        }
        return values;
    }

    private String removeDataDotPrefix(String propertyPath) {
        if(propertyPath != null && propertyPath.startsWith("data.")) {
            return propertyPath.substring("data.".length());
        }
        return propertyPath;
    }
}
