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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.search.Preconditions;
import org.opengroup.osdu.indexer.schema.converter.config.SchemaConverterConfig;
import org.opengroup.osdu.indexer.schema.converter.interfaces.SchemaToStorageFormat;
import org.opengroup.osdu.indexer.schema.converter.tags.PropertiesData;
import org.opengroup.osdu.indexer.schema.converter.tags.SchemaRoot;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Converts schema from Schema Service format to Storage Service format
 */
@Component
public class SchemaToStorageFormatImpl implements SchemaToStorageFormat {

    private ObjectMapper objectMapper;
    private JaxRsDpsLog log;
    private SchemaConverterConfig schemaConverterConfig;

    @Inject
    public SchemaToStorageFormatImpl(ObjectMapper objectMapper, JaxRsDpsLog log, SchemaConverterConfig schemaConverterConfig) {
        Preconditions.checkNotNull(objectMapper, "objectMapper cannot be null");

        this.objectMapper = objectMapper;
        this.log = log;
        this.schemaConverterConfig = schemaConverterConfig;
    }

    @Override
    public String convertToString(final String schemaServiceFormat, String kind) {
        Preconditions.checkNotNullOrEmpty(schemaServiceFormat, "schemaServiceFormat cannot be null or empty");
        Preconditions.checkNotNullOrEmpty(kind, "kind cannot be null or empty");

        return saveJsonToString(convert(parserJsonString(schemaServiceFormat), kind));
    }

    public Map<String, Object> convertToMap(final String schemaServiceFormat, String kind) {
        Preconditions.checkNotNullOrEmpty(schemaServiceFormat, "schemaServiceFormat cannot be null or empty");
        Preconditions.checkNotNullOrEmpty(kind, "kind cannot be null or empty");

        return convert(parserJsonString(schemaServiceFormat), kind);
    }

    protected SchemaRoot parserJsonString(final String schemaServiceFormat) {
        try {
            return objectMapper.readValue(schemaServiceFormat, SchemaRoot.class);
        } catch (JsonProcessingException e) {
            throw new AppException(HttpStatus.SC_BAD_REQUEST, "Loading shchem error", "Failed to load schema", e);
        }
    }

    protected String saveJsonToString(final Map<String, Object> schemaServiceFormat) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(schemaServiceFormat);
        } catch (JsonProcessingException e) {
            throw new AppException(HttpStatus.SC_UNPROCESSABLE_ENTITY, "Saving JSON error", "Failed to save a JSON file", e);
        }
    }

    public Map<String, Object> convert(SchemaRoot schemaServiceSchema, String kind) {
        Preconditions.checkNotNull(objectMapper, "schemaServiceSchema cannot be null");
        Preconditions.checkNotNullOrEmpty(kind, "kind cannot be null or empty");

        PropertiesProcessor propertiesProcessor = new PropertiesProcessor(schemaServiceSchema.getDefinitions(), log, schemaConverterConfig);

        final List<Map<String, Object>> storageSchemaItems = new ArrayList<>();
        if (schemaServiceSchema.getProperties() != null) {
            PropertiesData schemaData = schemaServiceSchema.getProperties().getData();
            if (!Objects.isNull(schemaData)) {

                if (schemaData.getAllOf() != null) {
                    storageSchemaItems.addAll(schemaServiceSchema.getProperties().getData().getAllOf().stream()
                            .flatMap(propertiesProcessor::processItem)
                            .collect(Collectors.toList()));
                }

                if (schemaData.getRef() != null) {
                    storageSchemaItems.addAll(propertiesProcessor.processRef(schemaData.getRef())
                            .collect(Collectors.toList()));
                }
            }
        } else {
            log.warning("Schema doesn't have properties, kind:" + kind);
        }

        final Map<String, Object> result = new LinkedHashMap<>();
        result.put("kind", kind);
        result.put("schema", storageSchemaItems);

        return result;
    }

}
