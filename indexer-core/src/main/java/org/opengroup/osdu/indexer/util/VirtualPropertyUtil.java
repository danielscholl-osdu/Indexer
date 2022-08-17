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

    public static boolean isPropertyPathMatched(String path, String propertyPath) {
        // We should not just use name.startsWith(originalPropertyPath)
        // For example, if originalPropertyPath is "FacilityName" and name.startsWith(originalPropertyPath) is used,
        // it can match property "FacilityNameAlias". Same in method chooseOriginalProperty(...)
        return !Strings.isNullOrEmpty(path) && (path.startsWith(propertyPath + PROPERTY_DELIMITER) || path.equals(propertyPath));
    }

    public static String removeDataPrefix(String path) {
        if (!Strings.isNullOrEmpty(path) && path.startsWith(DATA_PREFIX))
            return path.substring(DATA_PREFIX.length());
        return path;
    }
}
