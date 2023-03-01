/*
 * Copyright © Schlumberger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.indexer.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengroup.osdu.indexer.model.indexproperty.RelatedObjectsSpec;
import org.opengroup.osdu.indexer.model.indexproperty.ValueExtraction;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RunWith(SpringRunner.class)
public class PropertyUtilTest {
    private final Gson gson = new Gson();

    @Test
    public void isPropertyPathMatched() {
        Assert.assertTrue(PropertyUtil.isPropertyPathMatched("data.FacilityName", "data.FacilityName"));
        Assert.assertTrue(PropertyUtil.isPropertyPathMatched("data.ProjectedBottomHoleLocation.Wgs84Coordinates", "data.ProjectedBottomHoleLocation"));

        Assert.assertFalse(PropertyUtil.isPropertyPathMatched("data.FacilityName", "data.FacilityNameAliase"));
        Assert.assertFalse(PropertyUtil.isPropertyPathMatched("data.ProjectedBottomHoleLocation.Wgs84Coordinates", "data.ProjectedBottomHole"));
        Assert.assertFalse(PropertyUtil.isPropertyPathMatched("", "data.ProjectedBottomHole"));
        Assert.assertFalse(PropertyUtil.isPropertyPathMatched(null, "data.ProjectedBottomHole"));
    }

    @Test
    public void removeDataPrefix() {
        Assert.assertEquals("FacilityName", PropertyUtil.removeDataPrefix("data.FacilityName"));
        Assert.assertEquals("FacilityName", PropertyUtil.removeDataPrefix("FacilityName"));
        Assert.assertEquals("ProjectedBottomHoleLocation", PropertyUtil.removeDataPrefix("data.ProjectedBottomHoleLocation"));
        Assert.assertEquals("ProjectedBottomHoleLocation", PropertyUtil.removeDataPrefix("ProjectedBottomHoleLocation"));
        Assert.assertEquals("", PropertyUtil.removeDataPrefix(""));
        Assert.assertNull(PropertyUtil.removeDataPrefix(null));
    }

//    @Test
//    public void getRelatedObjectIds_with_valid_condition() {
//        Map<String, Object> dataMap = getDataMap("well.json");
//        RelatedObjectsSpec spec = new RelatedObjectsSpec();
//        spec.setRelatedObjectKind("osdu:wks:master-data--GeoPoliticalEntity:1.");
//        spec.setRelatedObjectID("data.GeoContexts[].GeoPoliticalEntityID");
//        spec.setRelatedConditionProperty("data.GeoContexts[].GeoTypeID");
//        List<String> matches = Arrays.asList("opendes:reference-data--GeoPoliticalEntityType:Country:", "opendes:reference-data--GeoPoliticalEntityType:LicenseBlock:");
//        spec.setRelatedConditionMatches(matches);
//
//        List<String> relatedObjectIds = PropertyUtil.getRelatedObjectIds(dataMap, spec);
//        Assert.assertEquals(2, relatedObjectIds.size());
//    }
//
//    @Test
//    public void getValuePaths_with_valid_condition() {
//        Map<String, Object> dataMap = getDataMap("well.json");
//        ValueExtraction valueExtraction = new ValueExtraction();
//        valueExtraction.setValuePath("data.NameAliases[].AliasName");
//        valueExtraction.setRelatedConditionProperty("data.NameAliases[].AliasNameTypeID");
//        List<String> matches = Arrays.asList(
//                "opendes:reference-data--AliasNameType:UniqueIdentifier:",
//                "opendes:reference-data--AliasNameType:RegulatoryName:",
//                "opendes:reference-data--AliasNameType:PreferredName:",
//                "opendes:reference-data--AliasNameType:CommonName:",
//                "opendes:reference-data--AliasNameType:ShortName:");
//        valueExtraction.setRelatedConditionMatches(matches);
//
//        String valuePath = PropertyUtil.removeDataPrefix(valueExtraction.getValuePath());
//
//        Map<String, Object> propertyValues = PropertyUtil.getPropertyValues(dataMap, valueExtraction, false);
//        Assert.assertTrue(propertyValues.containsKey(valuePath));
//        List<Object> values = (List<Object>)propertyValues.get(valuePath);
//        Assert.assertEquals(2, values.size());
//        Assert.assertTrue(values.contains("100000113552"));
//        Assert.assertTrue(values.contains("Well1"));
//
//
//        propertyValues = PropertyUtil.getPropertyValues(dataMap, valueExtraction, true);
//        Assert.assertTrue(propertyValues.containsKey(valuePath));
//        values = (List<Object>)propertyValues.get(valuePath);
//        Assert.assertEquals(1, values.size());
//        Assert.assertTrue(values.contains("100000113552"));
//        Assert.assertFalse(values.contains("Well1"));
//    }

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
