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
import joptsimple.internal.Strings;
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
public class GeoShapeParserTest {

    @InjectMocks
    private GeoShapeParser sut;

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void should_throwException_provided_emptyGeoJson() {
        String shapeJson = "{}";

        this.validateInput(this.sut::parseGeoJson, shapeJson, "shape type not included");
    }

    @Test
    public void should_throwException_parseInvalidPoint() {
        String shapeJson = "{\"type\":\"Point\",\"coordinates\":[-205.01621,39.57422]}";

        this.validateInput(this.sut::parseGeoJson, shapeJson, "Bad X value -205.01621 is not in boundary Rect(minX=-180.0,maxX=180.0,minY=-90.0,maxY=90.0)");
    }

    @Test
    public void should_throwException_parseInvalidPoint_NaN() {
        String shapeJson = "{\"type\":\"Point\",\"coordinates\":[-205.01621,NaN]}";

        this.validateInput(this.sut::parseGeoJson, shapeJson, "geo coordinates must be numbers");
    }

    @Test
    public void should_throwException_parseInvalidPoint_missingLatitude() {
        String shapeJson = "{\"type\":\"Point\",\"coordinates\":[-205.01621,\"\"]}";

        this.validateInput(this.sut::parseGeoJson, shapeJson, "geo coordinates must be numbers");
    }

    @Test
    public void should_throwException_missingMandatoryAttribute() {
        String shapeJson = "{\"mistype\":\"Point\",\"coordinates\":[-205.01621,39.57422]}";

        this.validateInput(this.sut::parseGeoJson, shapeJson, "shape type not included");
    }

    @Test
    public void should_throwException_parseInvalidShape() {
        String shapeJson = "{\"type\":\"InvalidShape\",\"coordinates\":[-105.01621,39.57422]}";

        this.validateInput(this.sut::parseGeoJson, shapeJson, "unknown geo_shape [InvalidShape]");
    }

    @Test
    public void should_parseValidPoint() {
        String shapeJson = "{\"type\":\"Point\",\"coordinates\":[-105.01621,39.57422]}";

        this.validateInput(this.sut::parseGeoJson, shapeJson, Strings.EMPTY);
    }

    @Test
    public void should_parseValidMultiPoint() {
        String shapeJson = "{\"type\":\"MultiPoint\",\"coordinates\":[[-105.01621,39.57422],[-80.666513,35.053994]]}";

        this.validateInput(this.sut::parseGeoJson, shapeJson, Strings.EMPTY);
    }

    @Test
    public void should_parseValidLineString() {
        String shapeJson = "{\"type\":\"LineString\",\"coordinates\":[[-101.744384,39.32155],[-101.552124,39.330048],[-101.403808,39.330048],[-101.332397,39.364032],[-101.041259,39.368279],[-100.975341,39.304549],[-100.914916,39.245016],[-100.843505,39.164141],[-100.805053,39.104488],[-100.491943,39.100226],[-100.437011,39.095962],[-100.338134,39.095962],[-100.195312,39.027718],[-100.008544,39.010647],[-99.865722,39.00211],[-99.684448,38.972221],[-99.51416,38.929502],[-99.382324,38.920955],[-99.321899,38.895308],[-99.113159,38.869651],[-99.0802,38.85682],[-98.822021,38.85682],[-98.448486,38.848264],[-98.206787,38.848264],[-98.020019,38.878204],[-97.635498,38.873928]]}";

        this.validateInput(this.sut::parseGeoJson, shapeJson, Strings.EMPTY);
    }

    @Test
    public void should_parseValidMultiLineString() {
        String shapeJson = "{\"type\":\"MultiLineString\",\"coordinates\":[[[-105.021443,39.578057],[-105.021507,39.577809],[-105.021572,39.577495],[-105.021572,39.577164],[-105.021572,39.577032],[-105.021529,39.576784]],[[-105.019898,39.574997],[-105.019598,39.574898],[-105.019061,39.574782]],[[-105.017173,39.574402],[-105.01698,39.574385],[-105.016636,39.574385],[-105.016508,39.574402],[-105.01595,39.57427]],[[-105.014276,39.573972],[-105.014126,39.574038],[-105.013825,39.57417],[-105.01331,39.574452]]]}";

        this.validateInput(this.sut::parseGeoJson, shapeJson, Strings.EMPTY);
    }

    @Test
    public void should_parseValidPolygon() {
        String shapeJson = "{\"type\":\"Polygon\",\"coordinates\":[[[100,0],[101,0],[101,1],[100,1],[100,0]]]}";

        this.validateInput(this.sut::parseGeoJson, shapeJson, Strings.EMPTY);
    }

    @Test
    public void should_throwException_parseInvalidPolygon_malformedLatitude() {
        String shapeJson = "{\"type\":\"Polygon\",\"coordinates\":[[[100,\"afgg\"],[101,0],[101,1],[100,1],[100,0]]]}";

        this.validateInput(this.sut::parseGeoJson, shapeJson, "geo coordinates must be numbers");
    }

    @Test
    public void should_parseValidMultiPolygon() {
        String shapeJson = "{\"type\":\"MultiPolygon\",\"coordinates\":[[[[107,7],[108,7],[108,8],[107,8],[107,7]]],[[[100,0],[101,0],[101,1],[100,1],[100,0]]]]}";

        this.validateInput(this.sut::parseGeoJson, shapeJson, Strings.EMPTY);
    }

    @Test
    public void should_parseValidGeometryCollection() {
        String shapeJson = "{\"type\":\"GeometryCollection\",\"geometries\":[{\"type\":\"Point\",\"coordinates\":[-80.660805,35.049392]},{\"type\":\"Polygon\",\"coordinates\":[[[-80.664582,35.044965],[-80.663874,35.04428],[-80.662586,35.04558],[-80.663444,35.046036],[-80.664582,35.044965]]]},{\"type\":\"LineString\",\"coordinates\":[[-80.662372,35.059509],[-80.662693,35.059263],[-80.662844,35.05893],[-80.66308,35.058332],[-80.663595,35.057753],[-80.663874,35.057401],[-80.66441,35.057033],[-80.664861,35.056787],[-80.665419,35.056506],[-80.665633,35.056312],[-80.666019,35.055891],[-80.666191,35.055452],[-80.666191,35.055171],[-80.666255,35.05489],[-80.666213,35.054222],[-80.666213,35.053924],[-80.665955,35.052905],[-80.665698,35.052044],[-80.665504,35.051482],[-80.665762,35.050481],[-80.66617,35.049725],[-80.666513,35.049286],[-80.666921,35.048531],[-80.667006,35.048215],[-80.667071,35.047775],[-80.667049,35.047389],[-80.666964,35.046985],[-80.666813,35.046353],[-80.666599,35.045966],[-80.666406,35.045615],[-80.665998,35.045193],[-80.665526,35.044877],[-80.664989,35.044543],[-80.664496,35.044174],[-80.663852,35.043876],[-80.663037,35.043717]]}]}";

        this.validateInput(this.sut::parseGeoJson, shapeJson, Strings.EMPTY);
    }

    @Test
    public void should_throwException_parseUnsupportedType_featureCollection() {
        String shapeJson = "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[1.9496737127045984,58.41415669686543],[1.8237672363511042,58.42946998193435],[1.8422735102001124,58.471472136376455],[1.9683241046247606,58.45614207250076],[1.9496737127045984,58.41415669686543]]]}}]}";

        this.validateInput(this.sut::parseGeoJson, shapeJson, "unknown geo_shape [FeatureCollection]");
    }

    @Test
    public void should_throwException_parseUnsupportedType_feature() {
        String shapeJson = "{\"type\":\"Feature\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[-80.724878,35.265454],[-80.722646,35.260338],[-80.720329,35.260618],[-80.718698,35.260267],[-80.715093,35.260548],[-80.71681,35.255361],[-80.710887,35.255361],[-80.703248,35.265033],[-80.704793,35.268397],[-80.70857,35.268257],[-80.712518,35.270359],[-80.715179,35.267696],[-80.721359,35.267276],[-80.724878,35.265454]]]},\"properties\":{\"name\":\"Plaza Road Park\"}}";

        this.validateInput(this.sut::parseGeoJson, shapeJson, "unknown geo_shape [Feature]");
    }

    private void validateInput(Function<Map<String, Object>, String> parser, String shapeJson, String errorMessage) {
        try {
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> shapeObj = new Gson().fromJson(shapeJson, type);

            String parsedShape = parser.apply(shapeObj);
            assertNotNull(parsedShape);
        } catch (IllegalArgumentException e) {
            assertThat(String.format("Incorrect error message for geo-json parsing [ %s ]", shapeJson),
                    e.getMessage(), containsString(errorMessage));
        }
    }
}
