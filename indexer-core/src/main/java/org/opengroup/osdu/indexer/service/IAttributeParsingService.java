package org.opengroup.osdu.indexer.service;

import java.util.Map;

public interface IAttributeParsingService {

    public static final String RECORD_GEOJSON_TAG = "GeoJSON.features.geometry";
    public static final String DATA_GEOJSON_TAG = "x-geojson";

    void tryParseValueArray(Class<?> attributeClass, String recordId, String attributeName, Object attributeVal, Map<String, Object> dataMap);

    void tryParseInteger(String recordId, String attributeName, Object attributeVal, Map<String, Object> dataMap);

    void tryParseLong(String recordId, String attributeName, Object attributeVal, Map<String, Object> dataMap);

    void tryParseFloat(String recordId, String attributeName, Object attributeVal, Map<String, Object> dataMap);

    void tryParseDouble(String recordId, String attributeName, Object attributeVal, Map<String, Object> dataMap);

    void tryParseBoolean(String recordId, String attributeName, Object attributeVal, Map<String, Object> dataMap);

    void tryParseString(String recordId, String attributeName, Object attributeVal, Map<String, Object> dataMap);

    void tryParseDate(String recordId, String attributeName, Object attributeVal, Map<String, Object> dataMap);

    void tryParseGeopoint(String recordId, String attributeName, Object attributeVal, Map<String, Object> dataMap);

    void tryParseGeojson(String recordId, String attributeName, Object attributeVal, Map<String, Object> dataMap);

    void tryParseNested(String recordId, String name, Object value, Map<String, Object> dataMap);

    void tryParseObject(String recordId, String name, Object value, Map<String, Object> dataMap);

    void tryParseFlattened(String recordId, String name, Object value, Map<String, Object> dataMap);
}
