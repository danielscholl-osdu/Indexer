package org.opengroup.osdu.indexer.model.indexproperty.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengroup.osdu.indexer.model.indexproperty.PropertyConfiguration;
import org.opengroup.osdu.indexer.model.indexproperty.PropertyConfigurations;
import org.opengroup.osdu.indexer.model.indexproperty.PropertyPath;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

@RunWith(SpringRunner.class)
public class PropertyPathDeserializerTest {
    @Test
    public void deserialize_configurations_test() throws JsonProcessingException {
        String jsonText = getJsonFromFile("configurations.json");
        ObjectMapper objectMapper = new ObjectMapper();
        PropertyConfigurations configurations = objectMapper.readValue(jsonText, PropertyConfigurations.class);
        Assert.assertNotNull(configurations);
        Assert.assertEquals(2, configurations.getConfigurations().size());
        PropertyConfiguration countryNameConfiguration = configurations.getConfigurations().get(0);
        PropertyConfiguration wellUWIConfiguration = configurations.getConfigurations().get(1);

        Assert.assertEquals("CountryNames", countryNameConfiguration.getName());
        Assert.assertEquals(1, countryNameConfiguration.getPaths().size());
        PropertyPath path1 = countryNameConfiguration.getPaths().get(0);
        Assert.assertTrue(path1.hasValidRelatedObjectsSpec());
        Assert.assertTrue(path1.getRelatedObjectsSpec().hasValidCondition());
        Assert.assertEquals(1, path1.getRelatedObjectsSpec().getRelatedConditionMatches().size());
        Assert.assertTrue(path1.hasValidValueExtraction());
        Assert.assertFalse(path1.getValueExtraction().hasValidCondition());

        Assert.assertEquals("WellUWI", wellUWIConfiguration.getName());
        Assert.assertEquals(1, wellUWIConfiguration.getPaths().size());
        PropertyPath path2 = wellUWIConfiguration.getPaths().get(0);
        Assert.assertFalse(path2.hasValidRelatedObjectsSpec());
        Assert.assertTrue(path2.hasValidValueExtraction());
        Assert.assertTrue(path2.getValueExtraction().hasValidCondition());
        Assert.assertEquals(5, path2.getValueExtraction().getRelatedConditionMatches().size());

    }

    @SneakyThrows
    private String getJsonFromFile(String file) {
        InputStream inStream = this.getClass().getResourceAsStream("/indexproperty/" + file);
        BufferedReader br = new BufferedReader(new InputStreamReader(inStream));
        StringBuilder stringBuilder = new StringBuilder();
        String sCurrentLine;
        while ((sCurrentLine = br.readLine()) != null)
        {
            stringBuilder.append(sCurrentLine).append("\n");
        }
        return stringBuilder.toString();
    }

}
