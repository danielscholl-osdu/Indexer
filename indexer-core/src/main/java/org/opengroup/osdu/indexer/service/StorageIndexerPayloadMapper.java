package org.opengroup.osdu.indexer.service;

import org.apache.commons.beanutils.NestedNullException;
import org.apache.commons.beanutils.PropertyUtils;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.indexer.ElasticType;
import org.opengroup.osdu.core.common.model.indexer.IndexSchema;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.opengroup.osdu.indexer.service.IAttributeParsingService.DATA_GEOJSON_TAG;
import static org.opengroup.osdu.indexer.service.IAttributeParsingService.RECORD_GEOJSON_TAG;

@Component
public class StorageIndexerPayloadMapper {

    @Inject
    private JaxRsDpsLog log;
    @Inject
    private IAttributeParsingService attributeParsingService;

    public Map<String, Object> mapDataPayload(IndexSchema storageSchema, Map<String, Object> storageRecordData, String recordId) {

        Map<String, Object> dataMap = new HashMap<>();

        if (storageSchema.isDataSchemaMissing()) return dataMap;

        // get the key and get the corresponding object from the storageRecord object
        for (Map.Entry<String, String> entry : storageSchema.getDataSchema().entrySet()) {

            String name = entry.getKey();

            Object value = getPropertyValue(recordId, storageRecordData, name);

            ElasticType elasticType = ElasticType.forValue(entry.getValue());

            if (value == null && !nullIndexedValueSupported(elasticType)) continue;

            switch (elasticType) {
                case KEYWORD:
                case KEYWORD_ARRAY:
                case TEXT:
                case TEXT_ARRAY:
                    dataMap.put(name, value);
                    break;
                case INTEGER_ARRAY:
                    this.attributeParsingService.tryParseValueArray(Integer.class, recordId, name, value, dataMap);
                    break;
                case INTEGER:
                    this.attributeParsingService.tryParseInteger(recordId, name, value, dataMap);
                    break;
                case LONG_ARRAY:
                    this.attributeParsingService.tryParseValueArray(Long.class, recordId, name, value, dataMap);
                    break;
                case LONG:
                    this.attributeParsingService.tryParseLong(recordId, name, value, dataMap);
                    break;
                case FLOAT_ARRAY:
                    this.attributeParsingService.tryParseValueArray(Float.class, recordId, name, value, dataMap);
                    break;
                case FLOAT:
                    this.attributeParsingService.tryParseFloat(recordId, name, value, dataMap);
                    break;
                case DOUBLE_ARRAY:
                    this.attributeParsingService.tryParseValueArray(Double.class, recordId, name, value, dataMap);
                    break;
                case DOUBLE:
                    this.attributeParsingService.tryParseDouble(recordId, name, value, dataMap);
                    break;
                case BOOLEAN_ARRAY:
                    this.attributeParsingService.tryParseValueArray(Boolean.class, recordId, name, value, dataMap);
                    break;
                case BOOLEAN:
                    this.attributeParsingService.tryParseBoolean(recordId, name, value, dataMap);
                    break;
                case DATE_ARRAY:
                    this.attributeParsingService.tryParseValueArray(Date.class, recordId, name, value, dataMap);
                    break;
                case DATE:
                    this.attributeParsingService.tryParseDate(recordId, name, value, dataMap);
                    break;
                case GEO_POINT:
                    this.attributeParsingService.tryParseGeopoint(recordId, name, storageRecordData, dataMap);
                    break;
                case GEO_SHAPE:
                    this.attributeParsingService.tryParseGeojson(recordId, name, value, dataMap);
                    break;
                case NESTED:
                case OBJECT:
                case UNDEFINED:
                    // don't do anything for now
                    break;
            }
        }

        // add these once iterated over the list
        storageSchema.getDataSchema().put(DATA_GEOJSON_TAG, ElasticType.GEO_SHAPE.getValue());
        storageSchema.getDataSchema().remove(RECORD_GEOJSON_TAG);

        return dataMap;
    }

    private Object getPropertyValue(String recordId, Map<String, Object> storageRecordData, String propertyKey) {

        try {
            // try getting first level property using optimized collection
            Object propertyVal = storageRecordData.get(propertyKey);
            if (propertyVal != null) return propertyVal;

            // use apache utils to get nested property
            return PropertyUtils.getProperty(storageRecordData, propertyKey);
        } catch (NestedNullException ignored) {
            // property not found in record
        } catch (NoSuchMethodException e) {
            this.log.warning(String.format("record-id: %s | error fetching property: %s | error: %s", recordId, propertyKey, e.getMessage()));
        } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
            this.log.warning(String.format("record-id: %s | error fetching property: %s | error: %s", recordId, propertyKey, e.getMessage()), e);
        }
        return null;
    }

    private boolean nullIndexedValueSupported(ElasticType type) {
        return type == ElasticType.TEXT;
    }
}