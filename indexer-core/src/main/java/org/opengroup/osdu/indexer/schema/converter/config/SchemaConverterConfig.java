package org.opengroup.osdu.indexer.schema.converter.config;

import java.util.Map;
import java.util.Set;

/*
Provides configuration for the schema converter
 */
public interface SchemaConverterConfig {
    Set<String> getSkippedDefinitions();
    Set<String> getSupportedArrayTypes();
    Map<String, String> getSpecialDefinitionsMap();
    Map<String, String> getPrimitiveTypesMap();
}
