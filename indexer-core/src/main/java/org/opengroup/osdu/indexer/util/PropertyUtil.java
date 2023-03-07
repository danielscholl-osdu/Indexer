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
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;

import java.util.*;

public class PropertyUtil {
    public static final String DATA_VIRTUAL_DEFAULT_LOCATION = "data.VirtualProperties.DefaultLocation";
    public static final String VIRTUAL_DEFAULT_LOCATION = "VirtualProperties.DefaultLocation";
    public static final String FIELD_WGS84_COORDINATES = ".Wgs84Coordinates";
    public static final String VIRTUAL_DEFAULT_LOCATION_WGS84_PATH = VIRTUAL_DEFAULT_LOCATION + FIELD_WGS84_COORDINATES;
    public static final String VIRTUAL_DEFAULT_LOCATION_IS_DECIMATED_PATH = VIRTUAL_DEFAULT_LOCATION + ".IsDecimated";


    private static final String PROPERTY_DELIMITER = ".";
    private static final String NESTED_OBJECT_DELIMITER = "[].";
    private static final String ARRAY_SYMBOL = "[]";
    private static final String DATA_PREFIX = "data" + PROPERTY_DELIMITER;

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
        if((to == null || to.isEmpty()) && (from == null || from.isEmpty())) {
            return new HashMap<>();
        }
        else if(to == null || to.isEmpty()) {
            return from;
        }
        else if(from == null || from.isEmpty()) {
            return to;
        }

        for (Map.Entry<String, Object> entry : from.entrySet()) {
            if (to.containsKey(entry.getKey())) {
                Set<Object> objectSet = new HashSet<>();

                Object toObject = to.get(entry.getKey());
                if (toObject instanceof List) {
                    objectSet.addAll((List) toObject);
                } else {
                    objectSet.add(toObject);
                }

                Object fromObject = entry.getValue();
                if (fromObject instanceof List) {
                    objectSet.addAll((List) fromObject);
                } else {
                    objectSet.add(fromObject);
                }

                List<Object> propertyValueList = new ArrayList<>(objectSet);
                Collections.sort(propertyValueList, Comparator.comparing(Object::toString));
                to.put(entry.getKey(), propertyValueList);
            } else {
                to.put(entry.getKey(), entry.getValue());
            }
        }

        return to;
    }

    public static Map<String, Object> replacePropertyPaths(String propertyRootPath, String valuePath, Map<String, Object> objectMap) {
        if(Strings.isNullOrEmpty(propertyRootPath) || Strings.isNullOrEmpty(propertyRootPath) || objectMap == null || objectMap.isEmpty()) {
            return new HashMap<>();
        }

        propertyRootPath = removeDataPrefix(propertyRootPath);
        valuePath = removeDataPrefix(valuePath);

        Map<String, Object> values = new HashMap<>();
        for (Map.Entry<String, Object> entry : objectMap.entrySet()) {
            String key = entry.getKey();
            if (key.equals(valuePath) || key.startsWith(valuePath + PROPERTY_DELIMITER)) {
                key = key.replace(valuePath, propertyRootPath);
                values.put(key, entry.getValue());
            }
        }
        return values;
    }

    public static boolean isConcreteKind(String kind) {
        if(Strings.isNullOrEmpty(kind)) {
            return false;
        }

        int index = kind.lastIndexOf(":");
        String version = kind.substring(index + 1);
        String[] subVersions = version.split("\\.");
        return (subVersions.length == 3);
    }

    public static String getKindWithMajor(String kind) {
        if(Strings.isNullOrEmpty(kind)) {
            return kind;
        }

        int index = kind.lastIndexOf(":");
        String kindWithMajor = kind.substring(0, index) + ":";
        String version = kind.substring(index + 1);
        String[] subVersions = version.split("\\.");
        if (subVersions.length > 0) {
            kindWithMajor += subVersions[0] + ".";
        }
        return kindWithMajor;
    }

    public static String removeIdPostfix(String objectId) {
        if (objectId != null && objectId.endsWith(":")) {
            objectId = objectId.substring(0, objectId.length() - 1);
        }
        return objectId;
    }

    public static List<String> getChangedProperties(Map<String, Object> leftMap, Map<String, Object> rightMap) {
        if(leftMap == null && rightMap == null) {
            return new ArrayList<>();
        }

        if(leftMap == null) {
            leftMap = new HashMap<>();
        }
        if(rightMap == null) {
            rightMap = new HashMap<>();
        }
        MapDifference<String, Object> difference = Maps.difference(leftMap, rightMap);
        if(difference.areEqual()) {
            return new ArrayList<>();
        }

        Set<String> changedProperties = new HashSet<>();
        if (difference.entriesOnlyOnLeft().size() > 0) {
            difference.entriesOnlyOnLeft().forEach((key, value) -> changedProperties.add(key));
        }
        if (difference.entriesOnlyOnRight().size() > 0) {
            difference.entriesOnlyOnRight().forEach((key, value) -> changedProperties.add(key));
        }
        if (difference.entriesDiffering().size() > 0) {
            for(Map.Entry<String, MapDifference.ValueDifference<Object>> entry : difference.entriesDiffering().entrySet()) {
                try {
                    MapDifference.ValueDifference<Object> valueDifference = entry.getValue();
                    Object left = valueDifference.leftValue();
                    Object right = valueDifference.rightValue();
                    if(left == null && right == null) {
                        continue;
                    }
                    else if(left == null || right == null) {
                        changedProperties.add(entry.getKey());
                    }
                    else if(left instanceof Map) {
                        Map<String, Object> innerLeftMap = (Map<String, Object>)left;
                        Map<String, Object> innerRightMap = (Map<String, Object>)right;
                        List<String> nestedChangedProperties = getChangedProperties(innerLeftMap, innerRightMap);
                        for (String nestedProperty: nestedChangedProperties) {
                            String p = entry.getKey() + PROPERTY_DELIMITER + nestedProperty;
                            changedProperties.add(p);
                        }
                    }
                    else if(left instanceof List) {
                        List<Object> innerLeftList = (List<Object>)left;
                        List<Object> innerRightList = (List<Object>)right;
                        if(innerLeftList.size() != innerRightList.size()) {
                            String p = entry.getKey() + ARRAY_SYMBOL;
                            changedProperties.add(p);
                        }
                        else {
                            for(int i = 0; i < innerLeftList.size(); i++) {
                                Map<String, Object> innerLeftMap = (Map<String, Object>)innerLeftList.get(i);
                                Map<String, Object> innerRightMap = (Map<String, Object>)innerRightList.get(i);
                                List<String> nestedChangedProperties = getChangedProperties(innerLeftMap, innerRightMap);
                                for (String nestedProperty: nestedChangedProperties) {
                                    String p = entry.getKey() + NESTED_OBJECT_DELIMITER + nestedProperty;
                                    changedProperties.add(p);
                                }
                            }
                        }
                    }
                    else {
                        changedProperties.add(entry.getKey());
                    }
                }
                catch (Exception ex) {
                    // assume there is difference in this case
                    changedProperties.add(entry.getKey());
                }
            }
        }

        return new ArrayList<>(changedProperties);
    }
}
