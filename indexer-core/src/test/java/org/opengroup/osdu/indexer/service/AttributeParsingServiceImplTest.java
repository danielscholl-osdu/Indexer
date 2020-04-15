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

package org.opengroup.osdu.indexer.service;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.indexer.JobStatus;
import org.opengroup.osdu.indexer.util.parser.DateTimeParser;
import org.opengroup.osdu.indexer.util.parser.GeoShapeParser;
import org.opengroup.osdu.indexer.util.parser.NumberParser;
import org.springframework.test.context.junit4.SpringRunner;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
public class AttributeParsingServiceImplTest {

    @Mock
    private GeometryConversionService geometryConversionService;
    @Mock
    private NumberParser numberParser;
    @Mock
    private DateTimeParser dateTimeParser;
    @Mock
    private GeoShapeParser geoShapeParser;
    @Mock
    private JaxRsDpsLog log;
    @Mock
    private JobStatus jobStatus;
    @InjectMocks
    private AttributeParsingServiceImpl sut;

    @Test
    public void should_parseValidInteger() {
        Map<String, Object> dataMap = new HashMap<>();

        when(this.numberParser.parseInteger(any(), any())).thenThrow(new IllegalArgumentException("number parsing error, integer out of range: attribute: lat | value: 101959.E1019594E"));
        this.sut.tryParseInteger("common:welldb:wellbore-OGY4ZWQ5", "lat", "101959.E1019594E", dataMap);
        assertEquals(dataMap.size(), 0);
        assertFalse(dataMap.containsKey("lat"));
    }

    @Test
    public void should_parseValidLong() {
        Map<String, Object> dataMap = new HashMap<>();

        when(this.numberParser.parseLong(any(), any())).thenReturn(0L);
        this.sut.tryParseLong("common:welldb:wellbore-OGY4ZWQ5", "reference", "", dataMap);
        assertEquals(dataMap.size(), 1);
        assertEquals(dataMap.get("reference"), 0L);
    }

    @Test
    public void should_parseValidFloat() {
        Map<String, Object> dataMap = new HashMap<>();

        when(this.numberParser.parseFloat(any(), any())).thenReturn(0f);
        this.sut.tryParseFloat("common:welldb:wellbore-MjVhND", "lon", null, dataMap);
        assertEquals(dataMap.size(), 1);
        assertEquals(dataMap.get("lon"), 0.0f);
    }

    @Test
    public void should_parseValidDouble() {
        Map<String, Object> dataMap = new HashMap<>();

        when(this.numberParser.parseDouble(any(), any())).thenReturn(20.0);
        this.sut.tryParseDouble("common:welldb:wellbore-zMWQtMm", "location", 20.0, dataMap);
        assertEquals(dataMap.size(), 1);
        assertEquals(dataMap.get("location"), 20.0);
    }

    @Test
    public void should_parseBoolean() {
        Map<String, Object> dataMap = new HashMap<>();

        this.sut.tryParseBoolean("common:welldb:wellbore-OGY4ZWQ5", "dry", "", dataMap);
        assertEquals(dataMap.size(), 1);
        assertEquals(dataMap.get("dry"), false);

        this.sut.tryParseBoolean("common:welldb:wellbore-OGY4ZWQ5", "active", null, dataMap);
        assertEquals(dataMap.size(), 2);
        assertEquals(dataMap.get("active"), false);

        this.sut.tryParseBoolean("common:welldb:wellbore-OGY4ZWQ5", "notation", "E.2131", dataMap);
        assertEquals(dataMap.size(), 3);
        assertEquals(dataMap.get("notation"), false);

        this.sut.tryParseBoolean("common:welldb:wellbore-OGY4ZWQ5", "aw", false, dataMap);
        assertEquals(dataMap.size(), 4);
        assertEquals(dataMap.get("aw"), false);

        this.sut.tryParseBoolean("common:welldb:wellbore-OGY4ZWQ5", "side", "true", dataMap);
        assertEquals(dataMap.size(), 5);
        assertEquals(dataMap.get("side"), true);
    }

    @Test
    public void should_parseDate_tryParseDate() {
        Map<String, Object> dataMap = new HashMap<>();

        this.sut.tryParseDate("common:welldb:wellbore-OGY4ZWQ5", "createTime", "", dataMap);
        assertEquals(dataMap.size(), 0);
        assertFalse(dataMap.containsKey("createTime"));

        this.sut.tryParseDate("common:welldb:wellbore-OGY4ZWQ5", "activatedOn", null, dataMap);
        assertEquals(dataMap.size(), 0);
        assertFalse(dataMap.containsKey("activatedOn"));

        this.sut.tryParseDate("common:welldb:wellbore-OGY4ZWQ5", "activatedOn", "E.2131", dataMap);
        assertEquals(dataMap.size(), 0);
        assertFalse(dataMap.containsKey("activatedOn"));

        when(this.dateTimeParser.convertDateObjectToUtc("2018-11-06T19:37:11.128Z")).thenReturn("2018-11-06T19:37:11+0000");
        this.sut.tryParseDate("common:welldb:wellbore-OGY4ZWQ5", "disabledOn", "2018-11-06T19:37:11.128Z", dataMap);
        assertEquals(dataMap.size(), 1);
        assertTrue(dataMap.containsKey("disabledOn"));
        assertEquals(dataMap.get("disabledOn"), "2018-11-06T19:37:11+0000");
    }

    @Test
    public void should_notReturnLatLong_given_oneOfTheNullAttribute_tryGetGeopointTest() {
        LinkedTreeMap<String, Object> positionTreeMap = new LinkedTreeMap<>();
        positionTreeMap.put("longitude", null);
        positionTreeMap.put("latitude", 20.0);

        Map<String, Object> storageData = new HashMap<>();
        storageData.put("location", positionTreeMap);

        Map<String, Object> dataMap = new HashMap<>();

        this.sut.tryParseGeopoint("common:welldb:wellbore-NjdhZTZ", "location", storageData, dataMap);

        assertTrue(dataMap.isEmpty());
    }

    @Test
    public void should_notReturnLatLong_given_oneOfTheEmptyAttribute_tryGetGeopointTest() {
        LinkedTreeMap<String, Object> positionTreeMap = new LinkedTreeMap<>();
        positionTreeMap.put("longitude", 23.46);
        positionTreeMap.put("latitude", "");

        Map<String, Object> storageData = new HashMap<>();
        storageData.put("location", positionTreeMap);

        Map<String, Object> dataMap = new HashMap<>();

        this.sut.tryParseGeopoint("common:welldb:wellbore-NjdhZTZ", "location", storageData, dataMap);

        assertTrue(dataMap.isEmpty());
    }

    @Test
    public void should_notReturnLatLong_given_invalidTreeMap_tryGetGeopointTest() {
        LinkedTreeMap<String, Object> positionTreeMap = new LinkedTreeMap<>();
        positionTreeMap.put("longitude", "hello");
        positionTreeMap.put("latitude", 20.0);

        Map<String, Object> storageData = new HashMap<>();
        storageData.put("location", positionTreeMap);

        Map<String, Object> dataMap = new HashMap<>();

        this.sut.tryParseGeopoint("", "location", storageData, dataMap);

        assertTrue(dataMap.isEmpty());
    }

    @Test
    public void should_notReturnLatLong_given_outOfRange_tryGetGeopointTest() {
        LinkedTreeMap<String, Object> positionTreeMap = new LinkedTreeMap<>();
        positionTreeMap.put("longitude", -189);
        positionTreeMap.put("latitude", 20.0);

        Map<String, Object> storageData = new HashMap<>();
        storageData.put("location", positionTreeMap);

        Map<String, Object> dataMap = new HashMap<>();

        this.sut.tryParseGeopoint("", "location", storageData, dataMap);

        assertTrue(dataMap.isEmpty());
    }

    @Test
    public void should_returnLatLong_given_validTreeMap_tryGetGeopointTest() {
        Map<String, Double> positionMap = new HashMap<>();
        positionMap.put("longitude", 10.45);
        positionMap.put("latitude", 90.0);

        Map<String, Object> storageData = new HashMap<>();
        storageData.put("location", positionMap);

        when(this.geometryConversionService.tryGetGeopoint(positionMap)).thenReturn(positionMap);

        Map<String, Object> dataMap = new HashMap<>();

        this.sut.tryParseGeopoint("", "location", storageData, dataMap);

        assertFalse(dataMap.isEmpty());
    }

    @Test
    public void should_returnGeoShape_given_validTreeMap_tryGetGeoShapeTest() {
        final String shapeJson = "{\"type\":\"Polygon\",\"coordinates\":[[[100,0],[101,0],[101,1],[100,1],[100,0]]]}";
        Map<String, Object> storageData = new HashMap<>();
        storageData.put("location", parseJson(shapeJson));

        when(this.geoShapeParser.parseGeoJson(storageData)).thenReturn("");

        Map<String, Object> dataMap = new HashMap<>();

        this.sut.tryParseGeojson("", "location", storageData, dataMap);

        assertFalse(dataMap.isEmpty());
    }

    @Test
    public void should_throwException_given_geoShapeParingFailed() {
        final String shapeJson = "{\"type\":\"Polygon\",\"coordinates\":[[[100,NaN],[101,0],[101,1],[100,1],[100,0]]]}";
        Map<String, Object> storageData = new HashMap<>();
        storageData.put("location", parseJson(shapeJson));

        when(this.geoShapeParser.parseGeoJson(any())).thenThrow(new IllegalArgumentException("geo coordinates must be numbers"));

        Map<String, Object> dataMap = new HashMap<>();

        this.sut.tryParseGeojson("", "location", storageData, dataMap);

        assertTrue(dataMap.isEmpty());
    }

    private Map<String, Object> parseJson(String json) {
        Type type = new TypeToken<Map<String, Object>>() {}.getType();
        return new Gson().fromJson(json, type);
    }
}