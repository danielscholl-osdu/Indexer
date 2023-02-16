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

package org.opengroup.osdu.indexer.util;

import com.google.api.client.util.Strings;
import org.opengroup.osdu.indexer.model.indexproperty.RelatedCondition;
import org.opengroup.osdu.indexer.model.indexproperty.RelatedObjectsSpec;
import org.opengroup.osdu.indexer.model.indexproperty.ValueExtraction;

import java.util.*;

public class PropertyUtil {
    public static final String DATA_VIRTUAL_DEFAULT_LOCATION = "data.VirtualProperties.DefaultLocation";
    public static final String VIRTUAL_DEFAULT_LOCATION = "VirtualProperties.DefaultLocation";
    public static final String FIELD_WGS84_COORDINATES = ".Wgs84Coordinates";
    public static final String VIRTUAL_DEFAULT_LOCATION_WGS84_PATH = VIRTUAL_DEFAULT_LOCATION + FIELD_WGS84_COORDINATES;
    public static final String VIRTUAL_DEFAULT_LOCATION_IS_DECIMATED_PATH = VIRTUAL_DEFAULT_LOCATION + ".IsDecimated";


    private static final String PROPERTY_DELIMITER = ".";
    private static final String PROPERTY_SPLIT_DELIMITER = "\\.";
    private static final String DATA_PREFIX = "data" + PROPERTY_DELIMITER;
    private static final String ARRAY_SYMBOL = "[]";

    public static boolean isPropertyPathMatched(String propertyPath, String parentPropertyPath) {
        // We should not just use propertyPath.startsWith(parentPropertyPath)
        // For example, if parentPropertyPath is "data.FacilityName" and propertyPath.startsWith(parentPropertyPath) is used,
        // then the property "data.FacilityNameAlias" will be matched and unexpected result will be returned.
        return !Strings.isNullOrEmpty(propertyPath) && (propertyPath.startsWith(parentPropertyPath + PROPERTY_DELIMITER) || propertyPath.equals(parentPropertyPath));
    }

    public static String removeDataPrefix(String path) {
        if (!Strings.isNullOrEmpty(path) && path.startsWith(DATA_PREFIX))
            return path.substring(DATA_PREFIX.length());
        return path;
    }

    public static List<String> getRelatedObjectIds(Map<String, Object> dataMap, RelatedObjectsSpec relatedObjectsSpec) {
        if(dataMap == null ||  dataMap.isEmpty() || relatedObjectsSpec == null || !relatedObjectsSpec.isValid())
            return new ArrayList<>();

        String relatedObjectId = removeDataPrefix(relatedObjectsSpec.getRelatedObjectID());
        RelatedCondition relatedCondition = relatedObjectsSpec.hasValidCondition()? relatedObjectsSpec : null;
        Map<String, Object> propertyValues = getPropertyValues(dataMap, relatedObjectId, relatedCondition, false);

        List<String> relatedObjectIds = new ArrayList<>();
        if(propertyValues.containsKey(relatedObjectId)) {
            Object value = propertyValues.get(relatedObjectId);
            if(value instanceof List) {
                for(Object obj : (List)value) {
                    relatedObjectIds.add(obj.toString());
                }
            }
            else {
                relatedObjectIds.add(value.toString());
            }

        }
        return relatedObjectIds;
    }

    public static Map<String, Object> getPropertyValues(Map<String, Object> dataMap, ValueExtraction valueExtraction, boolean isExtractFirstMatch) {
        if(dataMap == null ||  dataMap.isEmpty() || valueExtraction == null || !valueExtraction.isValid())
            return new HashMap<>();

        String valuePath = removeDataPrefix(valueExtraction.getValuePath());
        RelatedCondition relatedCondition = valueExtraction.hasValidCondition()? valueExtraction : null;
        return getPropertyValues(dataMap, valuePath, relatedCondition, isExtractFirstMatch);
    }

    private static Map<String, Object> getPropertyValues(Map<String, Object> dataMap, String extractProperty, RelatedCondition relatedCondition, boolean isExtractFirstMatch) {
        Map<String, Object> propertyValues = new HashMap<>();
        if (extractProperty.contains(ARRAY_SYMBOL)) {
            // Nested
            List<Object> valueList = new ArrayList<>();
            if(relatedCondition == null) {
                valueList = getPropertyValuesFromNestedObjects(dataMap, extractProperty, isExtractFirstMatch);
            }
            else {
                Set<Object> valueSet = new HashSet<>();
                String delimiter = "\\[\\]\\.";
                String[] extractPropertyParts = extractProperty.split(delimiter);
                String[] relatedConditionPropertyParts = removeDataPrefix(relatedCondition.getRelatedConditionProperty()).split(delimiter);
                if(dataMap.containsKey(extractPropertyParts[0]) && dataMap.get(extractPropertyParts[0]) != null) {
                    List<Map<String, Object>> nestedObjects = (List<Map<String, Object>>)dataMap.get(extractPropertyParts[0]);
                    for (Map<String, Object> nestedObject: nestedObjects) {
                        if(nestedObject.get(extractPropertyParts[1]) != null && nestedObject.get(extractPropertyParts[1]) != null) {
                            Object extractPropertyValue = nestedObject.get(extractPropertyParts[1]);
                            String conditionPropertyValue = nestedObject.get(relatedConditionPropertyParts[1]).toString();
                            if(relatedCondition.getRelatedConditionMatches().contains(conditionPropertyValue)) {
                                valueSet.add(extractPropertyValue);

                                if(isExtractFirstMatch)
                                    break;
                            }
                        }
                    }
                }
                valueList.addAll(valueSet);
            }

            propertyValues.put(extractProperty, valueList);
        } else {
            // Flatten
            for (Map.Entry<String, Object> entry: dataMap.entrySet()) {
                String key = entry.getKey();
                if(key.equals(extractProperty) || key.startsWith(extractProperty + ".")) {
                    propertyValues.put(key, entry.getValue());
                }
            }
        }

        return propertyValues;
    }

    private static List<Object> getPropertyValuesFromNestedObjects(Map<String, Object> dataMap, String propertyPath, boolean isExtractFirstMatch) {
        Set<Object> propertyValues = new HashSet<>();

        if(propertyPath.contains(ARRAY_SYMBOL)) {
            String[] subPaths = propertyPath.split(PROPERTY_SPLIT_DELIMITER);
            for(int i = 0; i < subPaths.length; i++) {
                String subPath = subPaths[i];
                if(subPath.endsWith(ARRAY_SYMBOL)) {
                    String prePath = String.join(PROPERTY_DELIMITER,  Arrays.copyOfRange(subPaths, 0, i + 1)).replace(ARRAY_SYMBOL, "");
                    String postPath = (i < subPaths.length - 1)? String.join(PROPERTY_DELIMITER, Arrays.copyOfRange(subPaths, i + 1, subPaths.length)) : "";
                    try {
                        if (dataMap.containsKey(prePath)) {
                            for (Object obj : (List<Object>) dataMap.get(prePath)) {
                                if (Strings.isNullOrEmpty(postPath)) {
                                    propertyValues.add(obj);
                                } else {
                                    List<Object> values = getPropertyValuesFromNestedObjects((Map<String, Object>) obj, postPath, isExtractFirstMatch);
                                    propertyValues.addAll(values);
                                }

                                if(isExtractFirstMatch && !propertyValues.isEmpty()) {
                                    break;
                                }
                            }
                        }
                    }
                    catch(Exception ex) {
                        //Ignore cast exception
                    }
                }
            }
        }
        else if(dataMap.containsKey(propertyPath) && dataMap.get(propertyPath) != null) {
            propertyValues.add(dataMap.get(propertyPath));
        }
        return new ArrayList<>(propertyValues);
    }
}
