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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.opengroup.osdu.indexer.schema.converter.interfaces.SchemaToStorageFormat;
import org.opengroup.osdu.indexer.schema.converter.tags.*;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Converts schema from Schema Service format to Storage Service format
 */
@Component
@FieldDefaults(makeFinal=true, level= AccessLevel.PRIVATE)
public class SchemaToStorageFormatImpl implements SchemaToStorageFormat {

    ObjectMapper objectMapper;

    @Inject
    public SchemaToStorageFormatImpl(ObjectMapper objectMapper) {
        assert objectMapper!= null;

        this.objectMapper = objectMapper;
    }

    @Override
    public String convertToString(final String schemaServiceFormat, String kind) {
        assert schemaServiceFormat!= null;
        assert kind!= null;
        assert !kind.isEmpty();

        return saveJsonToString(convert(parserJsonString(schemaServiceFormat), kind));
    }

    public Map<String, Object> convertToMap(final String schemaServiceFormat, String kind) {
        assert schemaServiceFormat!= null;
        assert kind!= null;
        assert !kind.isEmpty();

        return convert(parserJsonString(schemaServiceFormat), kind);
    }

    protected SchemaRoot parserJsonString(final String schemaServiceFormat) {
        try {
            return objectMapper.readValue(schemaServiceFormat, SchemaRoot.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to load schema", e);
        }
    }

    protected String saveJsonToString(final Map<String, Object> schemaServiceFormat) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(schemaServiceFormat);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to save JSON file", e);
        }
    }

    public Map<String, Object> convert(SchemaRoot schemaServiceSchema, String kind) {
        assert schemaServiceSchema!= null;
        assert kind!= null;
        assert !kind.isEmpty();

        PropertiesProcessor propertiesProcessor = new PropertiesProcessor(schemaServiceSchema.getDefinitions());

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
        }

        final Map<String, Object> result = new LinkedHashMap<>();
        result.put("kind", kind);
        result.put("schema", storageSchemaItems);

        return result;
    }

}
