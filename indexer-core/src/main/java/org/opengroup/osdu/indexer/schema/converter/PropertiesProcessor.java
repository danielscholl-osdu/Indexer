// Copyright 2017-2020, Schlumberger
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

import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.search.Preconditions;
import org.opengroup.osdu.indexer.schema.converter.tags.AllOfItem;
import org.opengroup.osdu.indexer.schema.converter.tags.Definition;
import org.opengroup.osdu.indexer.schema.converter.tags.Definitions;
import org.opengroup.osdu.indexer.schema.converter.tags.TypeProperty;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Stream;


public class PropertiesProcessor {

    private JaxRsDpsLog log;

    private static final String DEF_PREFIX = "#/definitions/";

    private static final Set<String> SKIP_DEFINITIONS = new HashSet<>(
            Arrays.asList("AbstractAnyCrsFeatureCollection.1.0.0",
                          "anyCrsGeoJsonFeatureCollection"));

    private static final Set<String> ARRAY_SUPPORTED_SIMPLE_TYPES = new HashSet<>(
            Arrays.asList("boolean", "integer", "number", "string"));

    private static final Map<String, String> SPEC_DEFINITION_TYPES = new HashMap<String, String>() {{
        put("AbstractFeatureCollection.1.0.0", "core:dl:geoshape:1.0.0");
        put("core_dl_geopoint", "core:dl:geopoint:1.0.0");
        put("geoJsonFeatureCollection", "core:dl:geoshape:1.0.0");
    }};

    private static final Map<String, String> PRIMITIVE_TYPES_MAP = new HashMap<String, String>() {{
        put("boolean", "bool");
        put("number", "double");
        put("date-time", "datetime");
        put("date", "datetime");
        put("time", "datetime");
        put("int32", "int");
        put("integer", "int");
        put("int64", "long");
    }};

    private final Definitions definitions;
    private final String pathPrefix;
    private final String pathPrefixWithDot;


    public PropertiesProcessor(Definitions definitions, JaxRsDpsLog log) {
        this(definitions, null, log);
    }

    public PropertiesProcessor(Definitions definitions, String pathPrefix, JaxRsDpsLog log) {
        this.log = log;
        this.definitions = definitions;
        this.pathPrefix = pathPrefix;
        this.pathPrefixWithDot = Objects.isNull(pathPrefix)  || pathPrefix.isEmpty() ? "" : pathPrefix + ".";
    }

    protected Stream<Map<String, Object>> processItem(AllOfItem allOfItem) {
        Preconditions.checkNotNull(allOfItem, "ref cannot be null");

        String ref = allOfItem.getRef();

        return Objects.isNull(ref) ?
            allOfItem.getProperties().entrySet().stream().flatMap(this::processPropertyEntry) : processRef(ref);
    }

    public Stream<Map<String, Object>> processRef(String ref) {
        Preconditions.checkNotNull(ref, "allOfItem cannot be null");

        if (!ref.contains(DEF_PREFIX)) {
            log.warning("Unknown definition:" + ref);
            return Stream.empty();
        }

        String definitionSubRef = ref.substring(DEF_PREFIX.length());

        if (SKIP_DEFINITIONS.contains(definitionSubRef)) {
            return Stream.empty();
        }

        if (!Objects.isNull(SPEC_DEFINITION_TYPES.get(definitionSubRef))) {
            return storageSchemaEntry(SPEC_DEFINITION_TYPES.get(definitionSubRef), pathPrefix);
        }

        Definition definition = definitions.getDefinition(definitionSubRef);
        Optional.ofNullable(definition).orElseThrow(() ->
         new AppException(HttpStatus.SC_NOT_FOUND, "Failed to find definition:" + definitionSubRef,
                 "Unknown definition:" + definitionSubRef));

        return definition.getProperties().entrySet().stream().flatMap(this::processPropertyEntry);
    }

    protected Stream<Map<String, Object>> processPropertyEntry(Map.Entry<String, TypeProperty> entry) {
        Preconditions.checkNotNull(entry, "entry cannot be null");

        if ("object".equals(entry.getValue().getType())
                && Objects.isNull(entry.getValue().getItems())
                && Objects.isNull(entry.getValue().getRef())
                && Objects.isNull(entry.getValue().getProperties())) {
            return Stream.empty();
        }

        if ("array".equals(entry.getValue().getType())) {
            if (ARRAY_SUPPORTED_SIMPLE_TYPES.contains(entry.getValue().getItems().getType())) {
                return storageSchemaEntry("[]" + getTypeByDefinitionProperty(entry.getValue()), pathPrefixWithDot + entry.getKey());
            }

            return Stream.empty();
        }

        if (!Objects.isNull(entry.getValue().getProperties())) {
            PropertiesProcessor propertiesProcessor = new PropertiesProcessor(definitions, pathPrefixWithDot + entry.getKey(), log);
            return entry.getValue().getProperties().entrySet().stream().flatMap(propertiesProcessor::processPropertyEntry);
        }

        if (!Objects.isNull(entry.getValue().getRef())) {
            return new PropertiesProcessor(definitions, pathPrefixWithDot + entry.getKey(), log)
                    .processRef(entry.getValue().getRef());
        }

        return storageSchemaEntry(getTypeByDefinitionProperty(entry.getValue()), pathPrefixWithDot + entry.getKey());
    }

    protected Stream<Map<String, Object>> storageSchemaEntry(String kind, String path) {
        Preconditions.checkNotNullOrEmpty(kind, "kind cannot be null or empty");
        Preconditions.checkNotNullOrEmpty(path, "path cannot be null or empty");

        Map<String, Object> map = new HashMap<>();
        map.put("kind", kind);
        map.put("path", path);
        return Stream.of(map);
    }

    protected String getTypeByDefinitionProperty(TypeProperty definitionProperty) {
        Preconditions.checkNotNull(definitionProperty, "definitionProperty cannot be null");

        String pattern = definitionProperty.getPattern();
        String format = definitionProperty.getFormat();
        String type = definitionProperty.getType();
        String itemsType = definitionProperty.getItems() != null ? definitionProperty.getItems().getType() : null;
        String itemsPattern = definitionProperty.getItems() != null ? definitionProperty.getItems().getPattern() : null;

        return !Objects.isNull(pattern) && pattern.startsWith("^srn") ? "link" :
                !Objects.isNull(itemsPattern) && itemsPattern.startsWith("^srn") ? "link" :
                !Objects.isNull(format)  ? PRIMITIVE_TYPES_MAP.getOrDefault(format, format) :
                        !Objects.isNull(itemsType) ? PRIMITIVE_TYPES_MAP.getOrDefault(itemsType, itemsType) :
                                PRIMITIVE_TYPES_MAP.getOrDefault(type, type);
    }
}
