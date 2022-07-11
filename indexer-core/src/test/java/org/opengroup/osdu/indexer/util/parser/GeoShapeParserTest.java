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

package org.opengroup.osdu.indexer.util.parser;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import joptsimple.internal.Strings;
import lombok.SneakyThrows;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit4.SpringRunner;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.function.Function;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@RunWith(SpringRunner.class)
public class GeoShapeParserTest {

    @InjectMocks
    private GeoShapeParser sut;

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void should_throwException_provided_emptyGeoJson() {
        String shapeJson = "{}";

        this.validateInput(this.sut::parseGeoJson, shapeJson, Strings.EMPTY, "shape not included");
    }

    @Test
    public void should_throwException_parseInvalidPoint() {
        String shapeJson = getGeoShapeFromFile("input/invalid_point.json");
        this.validateInput(this.sut::parseGeoJson, shapeJson, Strings.EMPTY, "unable to parse FeatureCollection");
    }

    @Test
    public void should_throwException_parseInvalidPoint_NaN() {
        String shapeJson = getGeoShapeFromFile("input/invalid_point_nan.json");
        this.validateInput(this.sut::parseGeoJson, shapeJson, Strings.EMPTY, "unable to parse FeatureCollection");
    }

    @Test
    public void should_throwException_parseInvalidPoint_missingLatitude() {
        String shapeJson = getGeoShapeFromFile("input/invalid_point_missing_latitude.json");
        this.validateInput(this.sut::parseGeoJson, shapeJson, Strings.EMPTY, "unable to parse FeatureCollection");
    }

    @Test
    public void should_throwException_missingMandatoryAttribute() {
        String shapeJson = getGeoShapeFromFile("input/missing_mandatory_attribute.json");
        this.validateInput(this.sut::parseGeoJson, shapeJson, Strings.EMPTY, "must be a valid FeatureCollection");
    }

    @Test
    public void should_throwException_parseInvalidShape() {
        String shapeJson = getGeoShapeFromFile("input/invalid_shape.json");
        this.validateInput(this.sut::parseGeoJson, shapeJson, Strings.EMPTY, "must be a valid FeatureCollection");
    }

    @Test
    public void should_parseValidPoint() {
        String shapeJson = getGeoShapeFromFile("input/valid_point.json");
        String expectedParsedShape = getGeoShapeFromFile("expected/valid_point.json");
        this.validateInput(this.sut::parseGeoJson, shapeJson, expectedParsedShape, Strings.EMPTY);
    }

    @Test
    public void should_parseValidMultiPoint() {
        String shapeJson = getGeoShapeFromFile("input/valid_multi_point.json");
        String expectedParsedShape =  getGeoShapeFromFile("expected/valid_multi_point.json");
        this.validateInput(this.sut::parseGeoJson, shapeJson, expectedParsedShape, Strings.EMPTY);
    }

    @Test
    public void should_parseValidLineString() {
        String shapeJson = getGeoShapeFromFile("input/valid_line_string.json");
        String expectedParsedShape = getGeoShapeFromFile("expected/valid_line_string.json");
        this.validateInput(this.sut::parseGeoJson, shapeJson, expectedParsedShape, Strings.EMPTY);
    }

    @Test
    public void should_parseValidMultiLineString() {
        String shapeJson = getGeoShapeFromFile("input/valid_multi_line_string.json");
        String expectedParsedShape = getGeoShapeFromFile("expected/valid_multi_line_string.json");
        this.validateInput(this.sut::parseGeoJson, shapeJson, expectedParsedShape, Strings.EMPTY);
    }

    @Test
    public void should_parseValidPolygon() {
        String shapeJson = getGeoShapeFromFile("input/valid_polygon.json");
        String expectedParsedShape = getGeoShapeFromFile("expected/valid_polygon.json");
        this.validateInput(this.sut::parseGeoJson, shapeJson, expectedParsedShape, Strings.EMPTY);
    }

    @Test
    public void should_throwException_parseInvalidPolygon_malformedLatitude() {
        String shapeJson = getGeoShapeFromFile("input/invalid_polygon_malformed_latitude.json");
        this.validateInput(this.sut::parseGeoJson, shapeJson, Strings.EMPTY, "unable to parse FeatureCollection");
    }

    @Test
    public void should_parseValidMultiPolygon() {
        String shapeJson = getGeoShapeFromFile("input/valid_multi_polygon.json");
        String expectedParsedShape = getGeoShapeFromFile("expected/valid_multi_polygon.json");
        this.validateInput(this.sut::parseGeoJson, shapeJson, expectedParsedShape, Strings.EMPTY);
    }

    @Test
    public void should_parseValidGeometryCollection() {
        String shapeJson = getGeoShapeFromFile("input/valid_geometry_collection.json");
        String expectedParsedShape = getGeoShapeFromFile("expected/valid_geometry_collection.json");
        this.validateInput(this.sut::parseGeoJson, shapeJson, expectedParsedShape, Strings.EMPTY);
    }

    @Test
    public void should_parseValidFeatureCollection() {
        String shapeJson = getGeoShapeFromFile("input/valid_feature_collection.json");
        String expectedParsedShape = getGeoShapeFromFile("expected/valid_feature_collection.json");
        this.validateInput(this.sut::parseGeoJson, shapeJson, expectedParsedShape, Strings.EMPTY);
    }

    @Test
    public void should_parseValidFeatureCollection_withZCoordinate() {
        String shapeJson = getGeoShapeFromFile("input/valid_feature_collection_with_z_coordinate.json");
        String expectedParsedShape = getGeoShapeFromFile("expected/valid_feature_collection_with_z_coordinate.json");
        this.validateInput(this.sut::parseGeoJson, shapeJson, expectedParsedShape, Strings.EMPTY);
    }

    @Test
    public void should_throwException_parseUnsupportedType_feature() {
        String shapeJson = getGeoShapeFromFile("input/valid_feature_collection_with_z_coordinate.json");
        String expectedParsedShape = getGeoShapeFromFile("expected/valid_feature_collection_with_z_coordinate.json");
        this.validateInput(this.sut::parseGeoJson, shapeJson, expectedParsedShape, Strings.EMPTY);
    }

    @Test
    public void shouldParseValidMultiPolygonWithZCoordinates(){
        String shapeJson = getGeoShapeFromFile("input/multi_polygon_with_z_coordinates.json");
        String expectedParsedShape = getGeoShapeFromFile("expected/multi_polygon_with_z_coordinates.json");
        this.validateInput(this.sut::parseGeoJson, shapeJson, expectedParsedShape, Strings.EMPTY);
    }

    @Test
    public void shouldParseValidLineStringWithZCoordinates(){
        String shapeJson = getGeoShapeFromFile("input/line_string_with_z_coordinate.json");
        String expectedParsedShape = getGeoShapeFromFile("expected/line_string_with_z_coordinate.json");
        this.validateInput(this.sut::parseGeoJson, shapeJson, expectedParsedShape, Strings.EMPTY);
    }

    @Test
    public void shouldParseValidMultiLineStringWithZCoordinates(){
        String shapeJson = getGeoShapeFromFile("input/valid_milti_line_string_with_z_coordinate.json");
        String expectedParsedShape = getGeoShapeFromFile("expected/valid_multi_line_string_with_z_coordinate.json");
        this.validateInput(this.sut::parseGeoJson, shapeJson, expectedParsedShape, Strings.EMPTY);
    }

    @Test
    public void should_throwException_parseInvalidFeatureCollection() {
        String shapeJson = getGeoShapeFromFile("input/invalid_feature_collection.json");

        Type type = new TypeToken<Map<String, Object>>() {}.getType();
        Map<String, Object> shapeObj = new Gson().fromJson(shapeJson, type);

        assertThrows(IllegalArgumentException.class, () -> this.sut.parseGeoJson(shapeObj));
    }

    private void validateInput(Function<Map<String, Object>, Map<String, Object>> parser, String shapeJson, String expectedParsedShape, String errorMessage) {
        try {
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> shapeObj = new Gson().fromJson(shapeJson, type);
            Map<String, Object> expectedShape = new Gson().fromJson(expectedParsedShape, type);

            Map<String, Object> parsedShape = parser.apply(shapeObj);
            assertNotNull(parsedShape);
            assertTrue(Strings.isNullOrEmpty(errorMessage));
            assertTrue(parsedShape.equals(expectedShape));
        } catch (IllegalArgumentException e) {
            if (Strings.isNullOrEmpty(errorMessage)) {
                fail(String.format("error parsing valid geo-json %s", shapeJson));
            } else {
                assertThat(String.format("Incorrect error message for geo-json parsing [ %s ]", shapeJson),
                        e.getMessage(), containsString(errorMessage));
            }
        }
    }

    @SneakyThrows
    private String getGeoShapeFromFile(String file) {
        InputStream inStream = this.getClass().getResourceAsStream("/geojson/parsing/" + file);
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
