package org.opengroup.osdu.indexer.schema.converter.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.*;

@Configuration
@ConfigurationProperties(prefix = "schema.converter")
@Getter
@Setter
public class SchemaConverterPropertiesConfig implements SchemaConverterConfig {

    private Set<String> skippedDefinitions = getDefaultSkippedDefinitions();
    private Set<String> supportedArrayTypes = getDefaultSupportedArrayTypes();
    private Map<String, String> specialDefinitionsMap = getDefaultSpecialDefinitionsMap();
    private Map<String, String> primitiveTypesMap = getDefaultPrimitiveTypesMap();

    private Set<String> getDefaultSkippedDefinitions() {
        return new HashSet<>(Arrays.asList("AbstractAnyCrsFeatureCollection.1.0.0",
                "anyCrsGeoJsonFeatureCollection"));
    }

    private Set<String> getDefaultSupportedArrayTypes() {
        return new HashSet<>(Arrays.asList("boolean", "integer", "number", "string", "object"));
    }

    private Map<String, String> getDefaultSpecialDefinitionsMap() {
        Map<String, String> defaultSpecialDefinitions = new HashMap<>();

        defaultSpecialDefinitions.put("AbstractFeatureCollection.1.0.0", "core:dl:geoshape:1.0.0");
        defaultSpecialDefinitions.put("core_dl_geopoint", "core:dl:geopoint:1.0.0");
        defaultSpecialDefinitions.put("geoJsonFeatureCollection", "core:dl:geoshape:1.0.0");

        return defaultSpecialDefinitions;
    }

    private Map<String, String> getDefaultPrimitiveTypesMap() {
        Map<String, String> defaultPrimitiveTypesMap = new HashMap<>();

        defaultPrimitiveTypesMap.put("boolean", "bool");
        defaultPrimitiveTypesMap.put("number", "double");
        defaultPrimitiveTypesMap.put("date-time", "datetime");
        defaultPrimitiveTypesMap.put("date", "datetime");
        defaultPrimitiveTypesMap.put("time", "datetime");
        defaultPrimitiveTypesMap.put("int32", "int");
        defaultPrimitiveTypesMap.put("integer", "int");
        defaultPrimitiveTypesMap.put("int64", "long");

        return defaultPrimitiveTypesMap;
    }
}
