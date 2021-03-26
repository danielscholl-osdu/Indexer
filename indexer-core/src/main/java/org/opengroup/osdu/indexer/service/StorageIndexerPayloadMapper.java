package org.opengroup.osdu.indexer.service;

import static org.opengroup.osdu.indexer.service.IAttributeParsingService.DATA_GEOJSON_TAG;
import static org.opengroup.osdu.indexer.service.IAttributeParsingService.RECORD_GEOJSON_TAG;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import javax.inject.Inject;
import org.apache.commons.beanutils.NestedNullException;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.Constants;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.indexer.ElasticType;
import org.opengroup.osdu.core.common.model.indexer.IndexSchema;
import org.opengroup.osdu.core.common.model.indexer.IndexingStatus;
import org.opengroup.osdu.core.common.model.indexer.JobStatus;
import org.opengroup.osdu.indexer.schema.converter.config.SchemaConverterConfig;
import org.springframework.stereotype.Component;

@Component
public class StorageIndexerPayloadMapper {

	@Inject
	private JaxRsDpsLog log;
	@Inject
	private IAttributeParsingService attributeParsingService;
	@Inject
	private JobStatus jobStatus;
	@Inject
	private SchemaConverterConfig schemaConfig;

	public Map<String, Object> mapDataPayload(IndexSchema storageSchema, Map<String, Object> storageRecordData,
		String recordId) {

		Map<String, Object> dataMap = new HashMap<>();

		if (storageSchema.isDataSchemaMissing()) {
			return dataMap;
		}

		mapDataPayload(storageSchema.getDataSchema(), storageRecordData, recordId, dataMap);

		// add these once iterated over the list
		storageSchema.getDataSchema().put(DATA_GEOJSON_TAG, ElasticType.GEO_SHAPE.getValue());
		storageSchema.getDataSchema().remove(RECORD_GEOJSON_TAG);

		return dataMap;
	}

	private Map<String, Object> mapDataPayload(Map<String, Object> dataSchema, Map<String, Object> storageRecordData,
		String recordId, Map<String, Object> dataMap) {

		// get the key and get the corresponding object from the storageRecord object
		for (Map.Entry<String, Object> entry : dataSchema.entrySet()) {
			String name = entry.getKey();
			Object value = getPropertyValue(recordId, storageRecordData, name);
			ElasticType elasticType = defineElasticType(entry);

			if (Objects.isNull(elasticType)) {
				this.jobStatus
					.addOrUpdateRecordStatus(recordId, IndexingStatus.WARN, HttpStatus.SC_BAD_REQUEST, "e.getMessage()",
						String.format("record-id: %s | %s", recordId, "e.getMessage()"));
				continue;
			}

			if (schemaConfig.getProcessedArraysTypes().contains(elasticType.getValue().toLowerCase())) {
				processInnerProperties(recordId, dataMap, entry, name, (List<Map>) value);
			}

			if (value == null && !nullIndexedValueSupported(elasticType)) {
				continue;
			}

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
					// don't do anything , each nested property will be mapped separately
					break;
				case FLATTENED:
					// flattened type inner properties won't be mapped as they types not present in schema
					this.attributeParsingService.tryParseFlattened(recordId, name, value, dataMap);
					break;
				case OBJECT:
					// object type inner properties won't be mapped as they types not present in schema
					this.attributeParsingService.tryParseObject(recordId, name, value, dataMap);
					break;
				case UNDEFINED:
					// don't do anything for now
					break;
			}
		}

		return dataMap;
	}

	private void processInnerProperties(String recordId, Map<String, Object> dataMap, Entry<String, Object> entry,
		String name, List<Map> value) {
		Map map = (Map) entry.getValue();
		Map innerProperties = (Map) map.get(Constants.PROPERTIES);
		ArrayList<Map> maps = new ArrayList<>();
		value.forEach(recordData -> maps.add(mapDataPayload(innerProperties, recordData, recordId, new HashMap<>())));
		dataMap.put(name, maps);
	}

	private ElasticType defineElasticType(Map.Entry<String, Object> entry) {
		ElasticType elasticType = null;
		if (entry.getValue() instanceof String) {
			elasticType = ElasticType.forValue(entry.getValue().toString());
		} else if (entry.getValue() instanceof Map) {
			Map map = (Map) entry.getValue();
			elasticType = ElasticType.forValue(map.get(Constants.TYPE).toString());
		}
		return elasticType;
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