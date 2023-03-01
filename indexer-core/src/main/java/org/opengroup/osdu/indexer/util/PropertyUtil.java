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
import org.opengroup.osdu.core.common.model.storage.Schema;
import org.opengroup.osdu.core.common.model.storage.SchemaItem;
import org.opengroup.osdu.indexer.model.indexproperty.*;

import java.util.*;

public class PropertyUtil {
    public static final String DATA_VIRTUAL_DEFAULT_LOCATION = "data.VirtualProperties.DefaultLocation";
    public static final String VIRTUAL_DEFAULT_LOCATION = "VirtualProperties.DefaultLocation";
    public static final String FIELD_WGS84_COORDINATES = ".Wgs84Coordinates";
    public static final String VIRTUAL_DEFAULT_LOCATION_WGS84_PATH = VIRTUAL_DEFAULT_LOCATION + FIELD_WGS84_COORDINATES;
    public static final String VIRTUAL_DEFAULT_LOCATION_IS_DECIMATED_PATH = VIRTUAL_DEFAULT_LOCATION + ".IsDecimated";


    private static final String PROPERTY_DELIMITER = ".";
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
}
