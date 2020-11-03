package org.opengroup.osdu.indexer.schema.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SchemaToStorageFormatImplTest {

    @Inject
    private ObjectMapper objectMapper;

    @Inject
    SchemaToStorageFormatImpl schemaToStorageFormatImpl;

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
                Paths.get(ClassLoader.getSystemResource(s).toURI()))
                , StandardCharsets.UTF_8);
    }

    private void compareSchemas(Map<String, Object> expected, Map<String, Object> converted) {
        assertEquals(converted.size(), expected.size());
        assertEquals(converted.get("kind"), expected.get("kind"));
        ArrayList<Map<String, String>> conv = (ArrayList<Map<String, String>>) converted.get("schema");
        ArrayList<Map<String, String>> exp = (ArrayList<Map<String, String>>) expected.get("schema");

        checkSchemaIteamsAreEqual(conv, exp);
    }

    private void checkSchemaIteamsAreEqual(ArrayList<Map<String, String>> conv, List<Map<String, String>> exp) {
        assertEquals(conv.size(), exp.size());
        conv.forEach((c) -> checkItemIn(c, exp));
    }

    private void checkItemIn(Map<String, String> item, List<Map<String, String>> exp) {
        String itemPath = item.get("path");
        assertEquals(exp.stream().filter(e->itemPath.equals(e.get("path"))).count(), 1L);
        Map<String, String> found =  exp.stream().filter(e->item.get("path").equals(e.get("path"))).findAny().get();
        assertEquals(found.get("kind"), item.get("kind"), "In " + itemPath);
    }
}