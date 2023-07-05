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
import org.springframework.test.context.junit4.SpringRunner;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.*;

@RunWith(SpringRunner.class)
public class PropertyUtilTest {
    private static final Gson gson = new Gson();

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
    public void hasSameMajorVersion() {
        Assert.assertTrue(PropertyUtil.hasSameMajorKind("osdu:wks:master-data--Well:1.0.0", "osdu:wks:master-data--Well:1.0.0"));
        Assert.assertTrue(PropertyUtil.hasSameMajorKind("osdu:wks:master-data--Well:1.1.0", "osdu:wks:master-data--Well:1.0.0"));
        Assert.assertTrue(PropertyUtil.hasSameMajorKind("osdu:wks:master-data--Well:1.0.2", "osdu:wks:master-data--Well:1.0.0"));
        Assert.assertFalse(PropertyUtil.hasSameMajorKind("osdu:wks:master-data--Well:2.0.0", "osdu:wks:master-data--Well:1.0.0"));
        Assert.assertFalse(PropertyUtil.hasSameMajorKind("osdu:wks:master-data--Well:2.0.0", null));
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

    @Test
    public void removeIdPostfix() {
        Assert.assertEquals("data-partition:entity:12345", PropertyUtil.removeIdPostfix("data-partition:entity:12345"));
        Assert.assertEquals("data-partition:entity:12345", PropertyUtil.removeIdPostfix("data-partition:entity:12345:"));
    }

    @Test
    public void combineObjectMap_listObject() {
        Map<String, Object> leftObjectMap = new HashMap<>();
        List<Object> leftList = new ArrayList<>(Arrays.asList("v1", "v3"));
        leftObjectMap.put("key", leftList);
        Map<String, Object> rightObjectMap = new HashMap<>();
        List<Object> rightList = new ArrayList<>(Arrays.asList("v1", "v2", "v4"));
        rightObjectMap.put("key", rightList);

        Map<String, Object> combinedObjectMap = PropertyUtil.combineObjectMap(leftObjectMap, new HashMap<>());
        Assert.assertEquals(1, combinedObjectMap.size());
        Assert.assertEquals(2, ((List)combinedObjectMap.get("key")).size());

        combinedObjectMap = PropertyUtil.combineObjectMap(new HashMap<>(), rightObjectMap);
        Assert.assertEquals(1, combinedObjectMap.size());
        Assert.assertEquals(3, ((List)combinedObjectMap.get("key")).size());

        combinedObjectMap = PropertyUtil.combineObjectMap(leftObjectMap, rightObjectMap);
        Assert.assertEquals(1, combinedObjectMap.size());
        Assert.assertEquals(4, ((List)combinedObjectMap.get("key")).size());
    }

    @Test
    public void combineObjectMap_mapObject() {
        Map<String, Object> leftObjectMap = new HashMap<>();
        Map<String, Object> leftInnerMap = new HashMap<>();
        leftInnerMap.put("key2_1", "value1");
        leftObjectMap.put("key", leftInnerMap);

        Map<String, Object> rightObjectMap = new HashMap<>();
        Map<String, Object> rightInnerMap = new HashMap<>();
        rightInnerMap.put("key2_2", "value2");
        rightObjectMap.put("key", rightInnerMap);

        Map<String, Object> combinedObjectMap = PropertyUtil.combineObjectMap(leftObjectMap, new HashMap<>());
        Assert.assertEquals(1, combinedObjectMap.size());
        Assert.assertEquals(1, ((Map)combinedObjectMap.get("key")).size());

        combinedObjectMap = PropertyUtil.combineObjectMap(new HashMap<>(), rightObjectMap);
        Assert.assertEquals(1, combinedObjectMap.size());
        Assert.assertEquals(1, ((Map)combinedObjectMap.get("key")).size());

        combinedObjectMap = PropertyUtil.combineObjectMap(leftObjectMap, rightObjectMap);
        Assert.assertEquals(1, combinedObjectMap.size());
        Assert.assertEquals(2, ((Map)combinedObjectMap.get("key")).size());
    }

    @Test
    public void combineObjectMap_sameStringValue() {
        Map<String, Object> leftObjectMap = new HashMap<>();
        leftObjectMap.put("key", "value");

        Map<String, Object> rightObjectMap = new HashMap<>();
        rightObjectMap.put("key", "value");

        Map<String, Object> combinedObjectMap = PropertyUtil.combineObjectMap(leftObjectMap, new HashMap<>());
        Assert.assertEquals(1, combinedObjectMap.size());
        Assert.assertEquals("value", combinedObjectMap.get("key"));

        combinedObjectMap = PropertyUtil.combineObjectMap(new HashMap<>(), rightObjectMap);
        Assert.assertEquals(1, combinedObjectMap.size());
        Assert.assertEquals("value", combinedObjectMap.get("key"));

        combinedObjectMap = PropertyUtil.combineObjectMap(leftObjectMap, rightObjectMap);
        Assert.assertEquals(1, combinedObjectMap.size());
        Assert.assertEquals("value", combinedObjectMap.get("key"));

    }

    @Test
    public void combineObjectMap_differentStringValue() {
        Map<String, Object> leftObjectMap = new HashMap<>();
        leftObjectMap.put("key", "value1");

        Map<String, Object> rightObjectMap = new HashMap<>();
        rightObjectMap.put("key", "value2");

        Map<String, Object> combinedObjectMap = PropertyUtil.combineObjectMap(leftObjectMap, new HashMap<>());
        Assert.assertEquals(1, combinedObjectMap.size());
        Assert.assertEquals("value1", combinedObjectMap.get("key"));

        combinedObjectMap = PropertyUtil.combineObjectMap(new HashMap<>(), rightObjectMap);
        Assert.assertEquals(1, combinedObjectMap.size());
        Assert.assertEquals("value2", combinedObjectMap.get("key"));

        combinedObjectMap = PropertyUtil.combineObjectMap(leftObjectMap, rightObjectMap);
        Assert.assertEquals(1, combinedObjectMap.size());
        Assert.assertEquals(2, ((List)combinedObjectMap.get("key")).size());
    }

    @Test
    public void combineObjectMap_ObjectList() {
        Map<String, Object> leftObjectMap = new HashMap<>();
        leftObjectMap.put("key", "value1");

        Map<String, Object> rightObjectMap = new HashMap<>();
        List<Object> rightList = new ArrayList<>(Arrays.asList("v1", "v2", "v4"));
        rightObjectMap.put("key", rightList);

        Map<String, Object> combinedObjectMap = PropertyUtil.combineObjectMap(leftObjectMap, rightObjectMap);
        Assert.assertEquals(1, combinedObjectMap.size());
        Assert.assertEquals(4, ((List)combinedObjectMap.get("key")).size());

        leftObjectMap = new HashMap<>();
        leftObjectMap.put("key", "value1");
        rightObjectMap = new HashMap<>();
        rightObjectMap.put("key", new ArrayList<>());
        combinedObjectMap = PropertyUtil.combineObjectMap(leftObjectMap, rightObjectMap);
        Assert.assertEquals(1, combinedObjectMap.size());
        Assert.assertEquals("value1", combinedObjectMap.get("key"));
    }

    @Test
    public void combineObjectMap_misMatchTypeValue() {
        Map<String, Object> leftObjectMap = new HashMap<>();
        leftObjectMap.put("key", "value1");

        Map<String, Object> rightObjectMap = new HashMap<>();
        rightObjectMap.put("key", new HashMap<>());

        // The object values in rightObjectMap will be ignored in this case
        Map<String, Object> combinedObjectMap = PropertyUtil.combineObjectMap(leftObjectMap, rightObjectMap);
        Assert.assertEquals(1, combinedObjectMap.size());
        Assert.assertTrue(combinedObjectMap.get("key") instanceof String);
        Assert.assertEquals("value1", combinedObjectMap.get("key"));

        combinedObjectMap = PropertyUtil.combineObjectMap(rightObjectMap, leftObjectMap);
        Assert.assertEquals(1, combinedObjectMap.size());
        Assert.assertTrue(combinedObjectMap.get("key") instanceof Map);
    }

    @Test
    public void replacePropertyPaths() {
        Map<String, Object> map = new HashMap<>();
        map.put("aaa", "value1");
        map.put("bbb.ccc", "value2");
        map.put("bbb.ddd", "value3");

        Map<String, Object> newMap;
        newMap = PropertyUtil.replacePropertyPaths("111", "aaa", map);
        Assert.assertEquals("value1", newMap.get("111"));
        newMap = PropertyUtil.replacePropertyPaths("111", "data.aaa", map);
        Assert.assertEquals("value1", newMap.get("111"));
        newMap = PropertyUtil.replacePropertyPaths("data.111", "aaa", map);
        Assert.assertEquals("value1", newMap.get("111"));

        newMap = PropertyUtil.replacePropertyPaths("222", "bbb", map);
        Assert.assertEquals("value2", newMap.get("222.ccc"));
        Assert.assertEquals("value3", newMap.get("222.ddd"));
        newMap = PropertyUtil.replacePropertyPaths("222", "data.bbb", map);
        Assert.assertEquals("value2", newMap.get("222.ccc"));
        Assert.assertEquals("value3", newMap.get("222.ddd"));
        newMap = PropertyUtil.replacePropertyPaths("data.222", "bbb", map);
        Assert.assertEquals("value2", newMap.get("222.ccc"));
        Assert.assertEquals("value3", newMap.get("222.ddd"));
    }

    @Test
    public void replacePropertyPaths_emptyValues() {
        Map<String, Object> map = new HashMap<>();
        map.put("abc", "value1");
        Assert.assertTrue(PropertyUtil.replacePropertyPaths(null, "abc", map).isEmpty());
        Assert.assertTrue(PropertyUtil.replacePropertyPaths("abc", null, map).isEmpty());
        Assert.assertTrue(PropertyUtil.replacePropertyPaths("a12", "abc", new HashMap<>()).isEmpty());
        Assert.assertTrue(PropertyUtil.replacePropertyPaths("a12", "abc", null).isEmpty());
    }

    @Test
    public void isConcreteKind() {
        Assert.assertTrue(PropertyUtil.isConcreteKind("osdu:wks:master-data--Well:1.0.0"));
        Assert.assertFalse(PropertyUtil.isConcreteKind("osdu:wks:master-data--Well:1.0."));
        Assert.assertFalse(PropertyUtil.isConcreteKind("osdu:master-data--Well:1.0.0"));
        Assert.assertFalse(PropertyUtil.isConcreteKind(null));
        Assert.assertFalse(PropertyUtil.isConcreteKind(""));
    }

    @Test
    public void getKindWithMajor() {
        Assert.assertEquals("osdu:wks:master-data--Well:1.", PropertyUtil.getKindWithMajor("osdu:wks:master-data--Well:1.0.0"));
        Assert.assertEquals("osdu:wks:master-data--Well:1.", PropertyUtil.getKindWithMajor("osdu:wks:master-data--Well:1."));
        Assert.assertEquals("osdu:wks:master-data--Well:1.", PropertyUtil.getKindWithMajor("osdu:wks:master-data--Well:1"));

        Assert.assertEquals("", PropertyUtil.getKindWithMajor("osdu:wks:master-data--Well"));
        Assert.assertEquals("", PropertyUtil.getKindWithMajor(""));
        Assert.assertNull(null, PropertyUtil.getKindWithMajor(null));
    }

    @Test
    public void getChangedProperties() {
        Map<String, Object> dataMapLeft = getDataMap("well.json");
        Map<String, Object> dataMapRight = getDataMap("well2.json");
        List<String> changedProperties = PropertyUtil.getChangedProperties(dataMapLeft, dataMapRight);
        Assert.assertEquals(3, changedProperties.size());
        List<String> expectedChangedWellProperties = Arrays.asList("VirtualProperties.DefaultName", "VerticalMeasurements[].VerticalMeasurementID", "FacilityName");
        changedProperties.forEach(p -> Assert.assertTrue(expectedChangedWellProperties.contains(p)));

        dataMapLeft = getDataMap("wellLog.json");
        dataMapRight = getDataMap("wellLog2.json");
        changedProperties = PropertyUtil.getChangedProperties(dataMapLeft, dataMapRight);
        List<String> expectedChangedWellLogProperties = Arrays.asList("Curves[].CurveID");
        changedProperties.forEach(p -> Assert.assertTrue(expectedChangedWellLogProperties.contains(p)));
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
