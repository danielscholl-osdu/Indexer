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
        if(!Strings.isNullOrEmpty(path) && path.startsWith(DATA_PREFIX))
            return path.substring(DATA_PREFIX.length());
        return path;
    }
}
