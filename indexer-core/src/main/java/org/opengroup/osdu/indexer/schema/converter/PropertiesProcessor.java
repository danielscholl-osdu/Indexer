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
import org.opengroup.osdu.indexer.schema.converter.config.SchemaConverterConfig;
import org.opengroup.osdu.indexer.schema.converter.config.SchemaConverterPropertiesConfig;
import org.opengroup.osdu.indexer.schema.converter.tags.AllOfItem;
import org.opengroup.osdu.indexer.schema.converter.tags.Definition;
import org.opengroup.osdu.indexer.schema.converter.tags.Definitions;
import org.opengroup.osdu.indexer.schema.converter.tags.TypeProperty;

import java.util.*;
import java.util.stream.Stream;

public class PropertiesProcessor {

    private JaxRsDpsLog log;
    private SchemaConverterConfig schemaConverterConfig;

    private static final String DEF_PREFIX = "#/definitions/";

    private final Definitions definitions;
    private final String pathPrefix;
    private final String pathPrefixWithDot;

    public PropertiesProcessor(Definitions definitions, JaxRsDpsLog log, SchemaConverterConfig schemaConverterConfig) {
        this(definitions, null, log, schemaConverterConfig);
    }

    public PropertiesProcessor(Definitions definitions, String pathPrefix, JaxRsDpsLog log, SchemaConverterConfig schemaConverterConfig) {
        this.log = log;
        this.definitions = definitions;
        this.pathPrefix = pathPrefix;
        this.pathPrefixWithDot = Objects.isNull(pathPrefix)  || pathPrefix.isEmpty() ? "" : pathPrefix + ".";
        this.schemaConverterConfig = schemaConverterConfig;
    }

    public Stream<Map<String, Object>> processItem(AllOfItem allOfItem) {
        Preconditions.checkNotNull(allOfItem, "allOfItem cannot be null");

        String ref = allOfItem.getRef();

        return Objects.isNull(ref) ?
            allOfItem.getProperties().entrySet().stream().flatMap(this::processPropertyEntry) : processRef(ref);
    }

    public Stream<Map<String, Object>> processRef(String ref) {
        Preconditions.checkNotNull(ref, "reference cannot be null");

        if (!ref.contains(DEF_PREFIX)) {
            log.warning("Unknown definition:" + ref);
            return Stream.empty();
        }

        String definitionSubRef = ref.substring(DEF_PREFIX.length());

        if (schemaConverterConfig.getSkippedDefinitions().contains(definitionSubRef)) {
            return Stream.empty();
        }

        if (Objects.nonNull(schemaConverterConfig.getSpecialDefinitionsMap().get(definitionSubRef))) {
            return storageSchemaEntry(schemaConverterConfig.getSpecialDefinitionsMap().get(definitionSubRef), pathPrefix);
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
            if (schemaConverterConfig.getSupportedArrayTypes().contains(entry.getValue().getItems().getType())) {
                return storageSchemaEntry("[]" + getTypeByDefinitionProperty(entry.getValue()), pathPrefixWithDot + entry.getKey());
            }

            return Stream.empty();
        }

        if (Objects.nonNull(entry.getValue().getProperties())) {
            PropertiesProcessor propertiesProcessor = new PropertiesProcessor(definitions, pathPrefixWithDot + entry.getKey()
                    , log, new SchemaConverterPropertiesConfig());
            return entry.getValue().getProperties().entrySet().stream().flatMap(propertiesProcessor::processPropertyEntry);
        }

        if (Objects.nonNull(entry.getValue().getRef())) {
            return new PropertiesProcessor(definitions, pathPrefixWithDot + entry.getKey(), log, new SchemaConverterPropertiesConfig())
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
        String itemsPattern = definitionProperty.getItems() != null ? definitionProperty.getItems().getPattern() : null;
        String format = definitionProperty.getFormat();
        String itemsType = definitionProperty.getItems() != null ? definitionProperty.getItems().getType() : null;
        String type = definitionProperty.getType();

        return Objects.nonNull(pattern) && pattern.startsWith("^srn") ? "link" :
                Objects.nonNull(itemsPattern) && itemsPattern.startsWith("^srn") ? "link" :
                Objects.nonNull(format)  ? schemaConverterConfig.getPrimitiveTypesMap().getOrDefault(format, format) :
                        Objects.nonNull(itemsType) ? schemaConverterConfig.getPrimitiveTypesMap().getOrDefault(itemsType, itemsType) :
                                schemaConverterConfig.getPrimitiveTypesMap().getOrDefault(type, type);
    }
}
