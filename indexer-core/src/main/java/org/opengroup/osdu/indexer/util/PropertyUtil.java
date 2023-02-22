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

    public static Map<String, Object> combineObjectMap(Map<String, Object> to, Map<String, Object> from) {
        for (Map.Entry<String, Object> entry: from.entrySet()) {
            if(to.containsKey(entry.getKey())) {
                Set<Object> objectSet = new HashSet<>();

                Object toObject = to.get(entry.getKey());
                if(toObject instanceof List) {
                    objectSet.addAll((List)toObject);
                }
                else {
                    objectSet.add(toObject);
                }

                Object fromObject = entry.getValue();
                if(fromObject instanceof  List) {
                    objectSet.addAll((List)fromObject);
                }
                else {
                    objectSet.add(fromObject);
                }

                to.put(entry.getKey(), new ArrayList<>(objectSet));
            }
            else {
                to.put(entry.getKey(), entry.getValue());
            }
        }

        return to;
    }

    public static Map<String, Object> replacePropertyPaths(String propertyRootPath, String valuePath, Map<String, Object> objectMap) {
        propertyRootPath = removeDataPrefix(propertyRootPath);
        valuePath = removeDataPrefix(valuePath);

        Map<String, Object> values = new HashMap<>();
        for (Map.Entry<String, Object> entry: objectMap.entrySet()) {
            String key = entry.getKey();
            if(key.equals(valuePath) || key.startsWith(valuePath + PROPERTY_DELIMITER)) {
                key = key.replace(valuePath, propertyRootPath);
                values.put(key, entry.getValue());
            }
        }
        return values;
    }

    public static List<String> getRelatedObjectIds(Map<String, Object> dataMap, RelatedObjectsSpec relatedObjectsSpec) {
        if(dataMap == null ||  dataMap.isEmpty() || relatedObjectsSpec == null || !relatedObjectsSpec.isValid())
            return new ArrayList<>();

        String relatedObjectId = removeDataPrefix(relatedObjectsSpec.getRelatedObjectID());
        Map<String, Object> propertyValues = getPropertyValues(dataMap, relatedObjectId, relatedObjectsSpec, relatedObjectsSpec.hasValidCondition(), false);

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
        return getPropertyValues(dataMap, valuePath, valueExtraction, valueExtraction.hasValidCondition(), isExtractFirstMatch);
    }

    private static Map<String, Object> getPropertyValues(Map<String, Object> dataMap, String valuePath, RelatedCondition relatedCondition, boolean hasValidCondition, boolean isExtractFirstMatch) {
        Map<String, Object> propertyValues = new HashMap<>();
        if (valuePath.contains(ARRAY_SYMBOL)) { // Nested
            String conditionProperty = null;
            List<String> conditionMatches = null;
            if(hasValidCondition) {
                int idx = relatedCondition.getRelatedConditionProperty().lastIndexOf(PROPERTY_DELIMITER);
                conditionProperty = relatedCondition.getRelatedConditionProperty().substring(idx + 1);
                conditionMatches = relatedCondition.getRelatedConditionMatches();
            }

            List<Object> valueList = getPropertyValuesFromNestedObjects(dataMap, valuePath, conditionProperty, conditionMatches, hasValidCondition, isExtractFirstMatch);
            if(!valueList.isEmpty()) {
                if(isExtractFirstMatch) {
                    propertyValues.put(valuePath, valueList.get(0));
                }
                else {
                    propertyValues.put(valuePath, valueList);
                }
            }
        } else { // Flatten
            for (Map.Entry<String, Object> entry: dataMap.entrySet()) {
                String key = entry.getKey();
                if(key.equals(valuePath) || key.startsWith(valuePath + PROPERTY_DELIMITER)) {
                    if(isExtractFirstMatch) {
                        propertyValues.put(key, entry.getValue());
                    }
                    else {
                        List<Object> values = new ArrayList<>();
                        values.add(entry.getValue());
                        propertyValues.put(key, values);
                    }
                }
            }
        }

        return propertyValues;
    }

    private static List<Object> getPropertyValuesFromNestedObjects(Map<String, Object> dataMap, String valuePath, String conditionProperty, List<String> conditionMatches, boolean hasCondition, boolean isExtractFirstMatch) {
        Set<Object> propertyValues = new HashSet<>();

        if(valuePath.contains(PROPERTY_DELIMITER)) {
            int idx = valuePath.indexOf(PROPERTY_DELIMITER);
            String prePath = valuePath.substring(0, idx);
            String postPath = valuePath.substring(idx + 1);
            try {
                if(prePath.endsWith(ARRAY_SYMBOL)) {
                    prePath = prePath.replace(ARRAY_SYMBOL, "");
                    if (dataMap.containsKey(prePath) && dataMap.get(prePath) != null) {
                        List<Map<String, Object>> nestedObjects = (List<Map<String, Object>>)dataMap.get(prePath);
                        for (Map<String, Object> nestedObject: nestedObjects) {
                            List<Object> valueList = getPropertyValuesFromNestedObjects(nestedObject, postPath, conditionProperty, conditionMatches, hasCondition, isExtractFirstMatch);
                            if(valueList != null && !valueList.isEmpty()) {
                                propertyValues.addAll(valueList);
                                if(isExtractFirstMatch)
                                    break;
                            }
                        }
                    }
                }
                else {
                    if (dataMap.containsKey(prePath) && dataMap.get(prePath) != null) {
                        Map<String, Object> nestedObject = (Map<String, Object>)dataMap.get(prePath);
                        List<Object> valueList = getPropertyValuesFromNestedObjects(nestedObject, postPath, conditionProperty, conditionMatches, hasCondition, isExtractFirstMatch);
                        if(valueList != null && !valueList.isEmpty()) {
                            propertyValues.addAll(valueList);
                        }
                    }
                }
            }
            catch(Exception ex) {
                //Ignore cast exception
            }
        }
        else if(dataMap.containsKey(valuePath) && dataMap.get(valuePath) != null) {
            Object extractPropertyValue = dataMap.get(valuePath);
            if(hasCondition) {
                if(dataMap.containsKey(conditionProperty) && dataMap.get(conditionProperty) != null) {
                    String conditionPropertyValue = dataMap.get(conditionProperty).toString();
                    if(conditionMatches.contains(conditionPropertyValue) && extractPropertyValue != null) {
                        propertyValues.add(extractPropertyValue);
                    }
                }
            }
            else {
                propertyValues.add(extractPropertyValue);
            }
        }
        return new ArrayList<>(propertyValues);
    }
}
