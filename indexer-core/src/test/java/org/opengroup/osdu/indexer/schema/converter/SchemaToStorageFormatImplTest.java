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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@SpringBootTest
public class SchemaToStorageFormatImplTest {

    private ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    private SchemaToStorageFormatImpl schemaToStorageFormatImpl = new SchemaToStorageFormatImpl(objectMapper);

    @Test
    public void firstSchemaPassed() {
        testSingleFile("/converter/first/schema.json", "osdu:osdu:Wellbore:1.0.0");
    }

    @Test
    public void integrationTestSchema1() {
        testSingleFile("/converter/integration-tests/index_records_1.schema", "KIND_VAL");
    }

    @Test
    public void integrationTestSchema2() {
        testSingleFile("/converter/integration-tests/index_records_2.schema", "KIND_VAL");
    }

    @Test
    public void integrationTestSchema3() {
        testSingleFile("/converter/integration-tests/index_records_3.schema", "KIND_VAL");
    }

    @Test
    public void wkeSchemaPassed() {
        testSingleFile("/converter/wks/slb_wke_wellbore.json", "slb:wks:wellbore:1.0.6");
    }

    @Test
    public void folderPassed() throws URISyntaxException, IOException {

        String folder = "/converter/R3-json-schema";
        Path path = Paths.get(this.getClass().getResource(folder).toURI());
        Files.walk(path)
                .filter(Files::isRegularFile)
                .filter(f -> f.toString().endsWith(".json"))
                .forEach( f -> testSingleFile(f.toString().replaceAll("\\\\", "/").substring(f.toString().replaceAll("\\\\", "/").indexOf(folder)), "osdu:osdu:Wellbore:1.0.0"));
    }

    private void testSingleFile(String filename, String kind) {
        String json = getSchemaFromSchemaService(filename);
        Map<String, Object> expected = getStorageSchema( filename + ".res");
        Map<String, Object> converted = schemaToStorageFormatImpl.convertToMap(json, kind);

        compareSchemas(expected, converted, filename);
    }

    private Map<String, Object> getStorageSchema(String s)  {

        TypeReference<Map<String, Object>> typeRef
                = new TypeReference<Map<String, Object>>() {
        };
        try {
            return objectMapper.readValue(this.getClass().getResource(s), typeRef);
        } catch (IOException | IllegalArgumentException e) {
            fail("Failed to load schema from file:" + s);
        }

        return null;
    }

    private String getSchemaFromSchemaService(String s) {
        try {
            return new String(Files.readAllBytes(
                    Paths.get(this.getClass().getResource(s).toURI())), StandardCharsets.UTF_8);
        } catch (Throwable e) {
            fail("Failed to read file:" + s);
        }
        return null;
    }

    private void compareSchemas(Map<String, Object> expected, Map<String, Object> converted, String filename) {
        assertEquals("File:" + filename, expected.size(), converted.size());
        assertEquals("File:" + filename, expected.get("kind"), converted.get("kind"));
        ArrayList<Map<String, String>> conv = (ArrayList<Map<String, String>>) converted.get("schema");
        ArrayList<Map<String, String>> exp = (ArrayList<Map<String, String>>) expected.get("schema");

        checkSchemaIteamsAreEqual(exp, conv, filename);
    }

    private void checkSchemaIteamsAreEqual(ArrayList<Map<String, String>> exp, List<Map<String, String>> conv, String filename) {
        assertEquals("File:" + filename, exp.size(), conv.size());
        conv.forEach((c) -> checkItemIn(c, exp, filename));
    }

    private void checkItemIn(Map<String, String> item, List<Map<String, String>> exp, String filename) {
        String itemPath = item.get("path");
        assertEquals("File:" + filename + ", " + itemPath + " is missed(or too many) see count", exp.stream().filter(e->itemPath.equals(e.get("path"))).count(), 1L);
        Map<String, String> found =  exp.stream().filter(e->item.get("path").equals(e.get("path"))).findAny().get();
        assertEquals("File:" + filename + ", in " + itemPath, found.get("kind"), item.get("kind"));
    }
}