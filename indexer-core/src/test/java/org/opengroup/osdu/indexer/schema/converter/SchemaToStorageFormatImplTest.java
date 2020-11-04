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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@SpringBootTest
public class SchemaToStorageFormatImplTest {

    private ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    private SchemaToStorageFormatImpl schemaToStorageFormatImpl = new SchemaToStorageFormatImpl(objectMapper);

    @Test
    public void firstSchemaPassed() throws IOException, URISyntaxException {
        String json = getSchemaFromSchemaService("converter/first/schema.json");
        Map<String, Object> expected = getStorageSchema("converter/first/de-schema.json");

        Map<String, Object> converted = schemaToStorageFormatImpl.convertToMap(json, "osdu:osdu:Wellbore:1.0.0");

        compareSchemas(expected, converted);
    }

    @Test
    public void wkeSchemaPassed() throws IOException, URISyntaxException {
        String json = getSchemaFromSchemaService("converter/wks/slb_wke_wellbore.json");
        Map<String, Object> expected = getStorageSchema("converter/wks/de-slb_wke_wellbore.json");

        Map<String, Object> converted = schemaToStorageFormatImpl.convertToMap(json, "slb:wks:wellbore:1.0.6");

        compareSchemas(expected, converted);
    }

    private Map<String, Object> getStorageSchema(String s) throws IOException {
        TypeReference<Map<String, Object>> typeRef
                = new TypeReference<Map<String, Object>>() {
        };
        return objectMapper.readValue(ClassLoader.getSystemResource(s), typeRef);
    }

    private String getSchemaFromSchemaService(String s) throws IOException, URISyntaxException {
        return new String(Files.readAllBytes(
                Paths.get(ClassLoader.getSystemResource(s).toURI())), StandardCharsets.UTF_8);
    }

    private void compareSchemas(Map<String, Object> expected, Map<String, Object> converted) {
        assertEquals(expected.size(), converted.size());
        assertEquals(expected.get("kind"), converted.get("kind"));
        ArrayList<Map<String, String>> conv = (ArrayList<Map<String, String>>) converted.get("schema");
        ArrayList<Map<String, String>> exp = (ArrayList<Map<String, String>>) expected.get("schema");

        checkSchemaIteamsAreEqual(exp, conv);
    }

    private void checkSchemaIteamsAreEqual(ArrayList<Map<String, String>> exp, List<Map<String, String>> conv) {
        assertEquals(exp.size(), conv.size());
        conv.forEach((c) -> checkItemIn(c, exp));
    }

    private void checkItemIn(Map<String, String> item, List<Map<String, String>> exp) {
        String itemPath = item.get("path");
        assertEquals(itemPath + " is missed(or too many) see count", exp.stream().filter(e->itemPath.equals(e.get("path"))).count(), 1L);
        Map<String, String> found =  exp.stream().filter(e->item.get("path").equals(e.get("path"))).findAny().get();
        assertEquals("In " + itemPath, found.get("kind"), item.get("kind"));
    }
}