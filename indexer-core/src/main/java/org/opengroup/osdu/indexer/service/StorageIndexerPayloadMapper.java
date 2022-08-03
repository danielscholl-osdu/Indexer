package org.opengroup.osdu.indexer.service;

import static org.opengroup.osdu.indexer.service.IAttributeParsingService.DATA_GEOJSON_TAG;
import static org.opengroup.osdu.indexer.service.IAttributeParsingService.RECORD_GEOJSON_TAG;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.inject.Inject;

import com.google.api.client.util.Strings;
import org.apache.commons.beanutils.NestedNullException;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.Constants;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.indexer.ElasticType;
import org.opengroup.osdu.core.common.model.indexer.IndexSchema;
import org.opengroup.osdu.core.common.model.indexer.IndexingStatus;
import org.opengroup.osdu.core.common.model.indexer.JobStatus;
import org.opengroup.osdu.core.common.search.Preconditions;
import org.opengroup.osdu.indexer.schema.converter.config.SchemaConverterConfig;
import org.opengroup.osdu.indexer.schema.converter.interfaces.IVirtualPropertiesSchemaCache;
import org.opengroup.osdu.indexer.schema.converter.tags.Priority;
import org.opengroup.osdu.indexer.schema.converter.tags.VirtualProperties;
import org.opengroup.osdu.indexer.schema.converter.tags.VirtualProperty;
import org.springframework.stereotype.Component;

@Component
public class StorageIndexerPayloadMapper {
	private static final String PROPERTY_DELIMITER = ".";
	private static final String DATA_PREFIX = "data" + PROPERTY_DELIMITER;

	@Inject
	private JaxRsDpsLog log;
	@Inject
	private IAttributeParsingService attributeParsingService;
	@Inject
	private JobStatus jobStatus;
	@Inject
	private SchemaConverterConfig schemaConfig;

	@Inject
	private IVirtualPropertiesSchemaCache virtualPropertiesSchemaCache;

	public Map<String, Object> mapDataPayload(IndexSchema storageSchema, Map<String, Object> storageRecordData,
		String recordId) {

		Map<String, Object> dataCollectorMap = new HashMap<>();

		if (storageSchema.isDataSchemaMissing()) {
			this.log.warning(String.format("record-id: %s | schema mismatching: %s ", recordId, storageSchema.getKind()));
			return dataCollectorMap;
		}

		mapDataPayload(storageSchema.getDataSchema(), storageRecordData, recordId, dataCollectorMap);
		mapVirtualPropertiesPayload(storageSchema, dataCollectorMap);

		// add these once iterated over the list
		storageSchema.getDataSchema().put(DATA_GEOJSON_TAG, ElasticType.GEO_SHAPE.getValue());
		storageSchema.getDataSchema().remove(RECORD_GEOJSON_TAG);

		return dataCollectorMap;
	}

	private Map<String, Object> mapDataPayload(Map<String, Object> dataSchema, Map<String, Object> storageRecordData,
		String recordId, Map<String, Object> dataCollectorMap) {

		// get the key and get the corresponding object from the storageRecord object
		for (Map.Entry<String, Object> entry : dataSchema.entrySet()) {
			String schemaPropertyName = entry.getKey();
			Object storageRecordValue = getPropertyValue(recordId, storageRecordData, schemaPropertyName);
			ElasticType elasticType = defineElasticType(entry.getValue());

			if (Objects.isNull(elasticType)) {
				this.jobStatus
					.addOrUpdateRecordStatus(recordId, IndexingStatus.WARN, HttpStatus.SC_BAD_REQUEST,
						String.format("record-id: %s | %s for entry %s", recordId, "Not resolvable elastic type", schemaPropertyName));
				continue;
			}

			if (schemaConfig.getProcessedArraysTypes().contains(elasticType.getValue().toLowerCase()) && Objects.nonNull(storageRecordValue)) {
				processInnerProperties(recordId, dataCollectorMap, entry.getValue(), schemaPropertyName, (List<Map>) storageRecordValue);
			}

			if (storageRecordValue == null && !nullIndexedValueSupported(elasticType)) {
				continue;
			}

			switch (elasticType) {
				case KEYWORD:
				case KEYWORD_ARRAY:
				case TEXT:
				case TEXT_ARRAY:
					dataCollectorMap.put(schemaPropertyName, storageRecordValue);
					break;
				case INTEGER_ARRAY:
					this.attributeParsingService.tryParseValueArray(Integer.class, recordId, schemaPropertyName, storageRecordValue, dataCollectorMap);
					break;
				case INTEGER:
					this.attributeParsingService.tryParseInteger(recordId, schemaPropertyName, storageRecordValue, dataCollectorMap);
					break;
				case LONG_ARRAY:
					this.attributeParsingService.tryParseValueArray(Long.class, recordId, schemaPropertyName, storageRecordValue, dataCollectorMap);
					break;
				case LONG:
					this.attributeParsingService.tryParseLong(recordId, schemaPropertyName, storageRecordValue, dataCollectorMap);
					break;
				case FLOAT_ARRAY:
					this.attributeParsingService.tryParseValueArray(Float.class, recordId, schemaPropertyName, storageRecordValue, dataCollectorMap);
					break;
				case FLOAT:
					this.attributeParsingService.tryParseFloat(recordId, schemaPropertyName, storageRecordValue, dataCollectorMap);
					break;
				case DOUBLE_ARRAY:
					this.attributeParsingService.tryParseValueArray(Double.class, recordId, schemaPropertyName, storageRecordValue, dataCollectorMap);
					break;
				case DOUBLE:
					this.attributeParsingService.tryParseDouble(recordId, schemaPropertyName, storageRecordValue, dataCollectorMap);
					break;
				case BOOLEAN_ARRAY:
					this.attributeParsingService.tryParseValueArray(Boolean.class, recordId, schemaPropertyName, storageRecordValue, dataCollectorMap);
					break;
				case BOOLEAN:
					this.attributeParsingService.tryParseBoolean(recordId, schemaPropertyName, storageRecordValue, dataCollectorMap);
					break;
				case DATE_ARRAY:
					this.attributeParsingService.tryParseValueArray(Date.class, recordId, schemaPropertyName, storageRecordValue, dataCollectorMap);
					break;
				case DATE:
					this.attributeParsingService.tryParseDate(recordId, schemaPropertyName, storageRecordValue, dataCollectorMap);
					break;
				case GEO_POINT:
					this.attributeParsingService.tryParseGeopoint(recordId, schemaPropertyName, storageRecordValue, dataCollectorMap);
					break;
				case GEO_SHAPE:
					this.attributeParsingService.tryParseGeojson(recordId, schemaPropertyName, storageRecordValue, dataCollectorMap);
					break;
				case FLATTENED:
					// flattened type inner properties will be added "as is" without parsing as they types not present in schema
					this.attributeParsingService.tryParseFlattened(recordId, schemaPropertyName, storageRecordValue, dataCollectorMap);
					break;
				case OBJECT:
					// object type inner properties will be added "as is" without parsing as they types not present in schema
					this.attributeParsingService.tryParseObject(recordId, schemaPropertyName, storageRecordValue, dataCollectorMap);
					break;
				case UNDEFINED:
					// don't do anything for now
					break;
			}
		}

		return dataCollectorMap;
	}

	private void processInnerProperties(String recordId, Map<String, Object> dataCollectorMap, Object schemaPropertyWithInnerProperties,
		String name, List<Map> storageRecordValue) {
		Map schemaPropertyMap = (Map) schemaPropertyWithInnerProperties;
		Map innerProperties = (Map) schemaPropertyMap.get(Constants.PROPERTIES);
		ArrayList<Map> innerPropertiesMappingCollector = new ArrayList<>();
		storageRecordValue.forEach(recordData -> innerPropertiesMappingCollector.add(mapDataPayload(innerProperties, recordData, recordId, new HashMap<>())));
		dataCollectorMap.put(name, innerPropertiesMappingCollector);
	}

	private ElasticType defineElasticType(Object entryValue) {
		ElasticType elasticType = null;
		if (entryValue instanceof String) {
			elasticType = ElasticType.forValue(entryValue.toString());
		} else if (entryValue instanceof Map) {
			Map map = (Map) entryValue;
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

	private void mapVirtualPropertiesPayload(IndexSchema storageSchema, Map<String, Object> dataCollectorMap) {
		if(dataCollectorMap.isEmpty() || this.virtualPropertiesSchemaCache.get(storageSchema.getKind()) == null) {
			return;
		}

		VirtualProperties virtualProperties = (VirtualProperties) this.virtualPropertiesSchemaCache.get(storageSchema.getKind());
		for (Map.Entry<String, VirtualProperty> entry :virtualProperties.getProperties().entrySet()) {
			if(entry.getValue().getPriorities() == null || entry.getValue().getPriorities().size() == 0) {
				continue;
			}
			Priority priority = chooseOriginalProperty(entry.getValue().getPriorities(), dataCollectorMap);
			String virtualPropertyPath = entry.getKey().startsWith(DATA_PREFIX)
										? entry.getKey().substring(DATA_PREFIX.length())
										: entry.getKey();
			String originalPropertyPath = priority.getPath().startsWith(DATA_PREFIX)
										? priority.getPath().substring(DATA_PREFIX.length())
										: priority.getPath();

			List<String> originalPropertyNames = dataCollectorMap.keySet().stream()
					.filter(path -> isPropertyPathMatched(path, originalPropertyPath))
					.collect(Collectors.toList());

			// Populate the virtual property values from the chosen original property
			for(String originalPropertyName: originalPropertyNames) {
				if(dataCollectorMap.containsKey(originalPropertyName)) {
					String virtualPropertyName = virtualPropertyPath + originalPropertyName.substring(originalPropertyPath.length());
					dataCollectorMap.put(virtualPropertyName, dataCollectorMap.get(originalPropertyName));
				}
			}
		}
	}

	private boolean isPropertyPathMatched(String path, String propertyPath) {
		// We should not just use name.startsWith(originalPropertyPath)
		// For example, if originalPropertyPath is "FacilityName" and name.startsWith(originalPropertyPath) is used,
		// it can match property "FacilityNameAlias". Same in method chooseOriginalProperty(...)
		return !Strings.isNullOrEmpty(path) && (path.startsWith(propertyPath + PROPERTY_DELIMITER) || path.equals(propertyPath));
	}

	private Priority chooseOriginalProperty(List<Priority> priorities, Map<String, Object> dataCollectorMap) {
		for(Priority priority: priorities) {
			String originalPropertyPath = priority.getPath().startsWith(DATA_PREFIX)
					? priority.getPath().substring(DATA_PREFIX.length())
					: priority.getPath();
			List<String> originalPropertyNames = dataCollectorMap.keySet().stream().filter(name ->
					name.startsWith(originalPropertyPath + PROPERTY_DELIMITER) || name.equals(originalPropertyPath))
					.collect(Collectors.toList());
			for(String originalPropertyName: originalPropertyNames) {
				// TBD: Should we specially handle DefaultLocation -- originalPropertyName + ".Wgs84Coordinates"
				if(dataCollectorMap.containsKey(originalPropertyName) && dataCollectorMap.get(originalPropertyName) != null)
					return priority;
			}
		}

		// None of the original properties has value, return the default one
		return priorities.get(0);
	}
}
