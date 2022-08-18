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

public class VirtualPropertyUtil {
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
}
