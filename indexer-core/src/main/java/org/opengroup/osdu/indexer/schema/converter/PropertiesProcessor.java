// Copyright 2017-2019, Schlumberger
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.indexer.schema.converter;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.opengroup.osdu.indexer.schema.converter.tags.AllOfItem;
import org.opengroup.osdu.indexer.schema.converter.tags.Definition;
import org.opengroup.osdu.indexer.schema.converter.tags.Definitions;
import org.opengroup.osdu.indexer.schema.converter.tags.TypeProperty;

import java.util.*;
import java.util.stream.Stream;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
class PropertiesProcessor {
    static String DEF_PREFIX = "#/definitions/";

    static Set<String> SKIP_DEFINITIONS = new HashSet<>(Collections.singletonList("AbstractAnyCrsFeatureCollection.1.0.0"));

    static Set<String> DONT_EXPAND_DEFINITIONS = new HashSet<>(
            Arrays.asList("AbstractFeatureCollection.1.0.0", "AbstractAnyCrsFeatureCollection.1.0.0",
                    "geoJsonFeatureCollection", "core_dl_geopoint", "AbstractAnyCrsFeatureCollection.1.0.0"));

    static Set<String> ARRAY_SUPPORTED_SIMPLE_TYPES = new HashSet<>(
            Arrays.asList("number", "string", "integer", "boolean"));

    static Map<String, String> SPEC_DEFINITION_TYPES = new HashMap<String, String>() {{
        put("AbstractFeatureCollection.1.0.0", "core:dl:geoshape:1.0.0");
        put("geoJsonFeatureCollection", "core:dl:geoshape:1.0.0");
        put("core_dl_geopoint", "core:dl:geopoint:1.0.0");
    }};

    static Map<String, String> PRIMITIVE_TYPES_MAP = new HashMap<String, String>() {{
        put("date-time", "datetime");
        put("date", "datetime");
        put("int64", "long");
        put("number", "double");
        put("boolean", "bool");
        put("integer", "int");
    }};

    Definitions definitions;
    String prefix;
    String formattedPrefix;

    public PropertiesProcessor(Definitions definitions) {
        this(definitions, null);
    }

    public PropertiesProcessor(Definitions definitions, String prefix) {
        this.definitions = definitions;
        this.prefix = prefix;
        this.formattedPrefix = prefix == null || prefix.isEmpty() ? "" : prefix + ".";
    }

    protected Stream<Map<String, Object>> processItem(AllOfItem allOfItem) {
        String ref = allOfItem.getRef();

        if (ref != null) {
            return processRef(ref);
        }

        return allOfItem.getProperties().entrySet().stream().flatMap(this::processPropertyEntry);
    }

    public Stream<Map<String, Object>> processRef(String ref) {
        String definitionSubRef = ref.substring(DEF_PREFIX.length());
        Definition definition = definitions.getDefinition(
                Optional.of(definitionSubRef)
                        .orElseThrow(() -> new RuntimeException("Unknown reference")));
        Optional.ofNullable(definition).orElseThrow(() -> new RuntimeException("Failed to find definition"));

        if (SKIP_DEFINITIONS.contains(definitionSubRef)) {
            return Stream.empty();
        }

        if (DONT_EXPAND_DEFINITIONS.contains(definitionSubRef)) {
            return storageSchemaEntry(SPEC_DEFINITION_TYPES.get(definitionSubRef), prefix);
        }

        return definition.getProperties().entrySet().stream().flatMap(this::processPropertyEntry);
    }

    protected Stream<Map<String, Object>> processPropertyEntry(Map.Entry<String, TypeProperty> entry) {
        if ("object".equals(entry.getValue().getType())
                && entry.getValue().getItems() == null
                && entry.getValue().getRef() == null) {
            return Stream.empty();
        }

        if ("array".equals(entry.getValue().getType())) {
            if (ARRAY_SUPPORTED_SIMPLE_TYPES.contains(entry.getValue().getItems().getType())) {
                return storageSchemaEntry("[]" + getTypeByDefinitionProperty(entry.getValue()), formattedPrefix + entry.getKey());
            }

            return Stream.empty();
        }

        if (entry.getValue().getRef() != null) {
            PropertiesProcessor propertiesProcessor = new PropertiesProcessor(definitions, formattedPrefix + entry.getKey());
            return propertiesProcessor.processRef(entry.getValue().getRef());
        }

        return storageSchemaEntry(getTypeByDefinitionProperty(entry.getValue()), formattedPrefix + entry.getKey());
    }

    protected Stream<Map<String, Object>> storageSchemaEntry(String kind, String path) {
        Map<String, Object> map = new HashMap<>();
        map.put("kind", kind);
        map.put("path", path);
        return Stream.of(map);
    }

    protected String getTypeByDefinitionProperty(TypeProperty definitionProperty) {
        String pattern = definitionProperty.getPattern();
        String format = definitionProperty.getFormat();
        String type = definitionProperty.getType();
        String itemsType = definitionProperty.getItems() != null ? definitionProperty.getItems().getType() : null;

        return pattern != null && pattern.startsWith("^srn") ? "link" :
                format != null ? PRIMITIVE_TYPES_MAP.getOrDefault(format, format) :
                        itemsType != null ? PRIMITIVE_TYPES_MAP.getOrDefault(itemsType, itemsType) :
                                PRIMITIVE_TYPES_MAP.getOrDefault(type, type);
    }
}
