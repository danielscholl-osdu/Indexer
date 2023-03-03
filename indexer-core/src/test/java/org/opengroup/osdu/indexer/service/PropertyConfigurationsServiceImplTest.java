package org.opengroup.osdu.indexer.service;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.opengroup.osdu.indexer.model.indexproperty.RelatedObjectsSpec;
import org.opengroup.osdu.indexer.model.indexproperty.ValueExtraction;
import org.opengroup.osdu.indexer.util.PropertyUtil;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RunWith(SpringRunner.class)
public class PropertyConfigurationsServiceImplTest {
    private final Gson gson = new Gson();

    @InjectMocks
    private PropertyConfigurationsServiceImpl sut;

    @Test
    public void getRelatedObjectIds_with_valid_condition() {
        Map<String, Object> dataMap = getDataMap("well.json");
        RelatedObjectsSpec spec = new RelatedObjectsSpec();
        spec.setRelationshipDirection("ChildToParent");
        spec.setRelatedObjectKind("osdu:wks:master-data--GeoPoliticalEntity:1.");
        spec.setRelatedObjectID("data.GeoContexts[].GeoPoliticalEntityID");
        spec.setRelatedConditionProperty("data.GeoContexts[].GeoTypeID");
        List<String> matches = Arrays.asList("opendes:reference-data--GeoPoliticalEntityType:Country:", "opendes:reference-data--GeoPoliticalEntityType:LicenseBlock:");
        spec.setRelatedConditionMatches(matches);

        List<String> relatedObjectIds = sut.getRelatedObjectIds(dataMap, spec);
        Assert.assertEquals(2, relatedObjectIds.size());
    }

    @Test
    public void getValuePaths_with_valid_condition() {
        Map<String, Object> dataMap = getDataMap("well.json");
        ValueExtraction valueExtraction = new ValueExtraction();
        valueExtraction.setValuePath("data.NameAliases[].AliasName");
        valueExtraction.setRelatedConditionProperty("data.NameAliases[].AliasNameTypeID");
        List<String> matches = Arrays.asList(
                "opendes:reference-data--AliasNameType:UniqueIdentifier:",
                "opendes:reference-data--AliasNameType:RegulatoryName:",
                "opendes:reference-data--AliasNameType:PreferredName:",
                "opendes:reference-data--AliasNameType:CommonName:",
                "opendes:reference-data--AliasNameType:ShortName:");
        valueExtraction.setRelatedConditionMatches(matches);

        String valuePath = PropertyUtil.removeDataPrefix(valueExtraction.getValuePath());

        Map<String, Object> propertyValues = sut.getPropertyValues(dataMap, valueExtraction, false);
        Assert.assertTrue(propertyValues.containsKey(valuePath));
        Assert.assertTrue(propertyValues.get(valuePath) instanceof List);
        List<Object> values = (List<Object>)propertyValues.get(valuePath);
        Assert.assertEquals(2, values.size());
        Assert.assertTrue(values.contains("100000113552"));
        Assert.assertTrue(values.contains("Well1"));


        propertyValues = sut.getPropertyValues(dataMap, valueExtraction, true);
        Assert.assertTrue(propertyValues.containsKey(valuePath));
        Assert.assertTrue(propertyValues.get(valuePath) instanceof String);
        String value = (String)propertyValues.get(valuePath);
        Assert.assertEquals("100000113552", value);
    }

    private Map<String, Object> getDataMap(String file) {
        String jsonText = getJsonFromFile(file);
        Type type = new TypeToken<Map<String, Object>>() {}.getType();
        return  gson.fromJson(jsonText, type);
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
