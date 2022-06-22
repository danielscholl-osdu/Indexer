package org.opengroup.osdu.indexer.model.geojson.jackson;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import joptsimple.internal.Strings;
import lombok.SneakyThrows;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.opengroup.osdu.indexer.model.geojson.Feature;
import org.opengroup.osdu.indexer.model.geojson.FeatureCollection;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
public class FeatureCollectionDeserializerTest {

    @InjectMocks
    private FeatureCollectionDeserializer featureCollectionDeserializer;

    private final ObjectMapper mapper = new ObjectMapper();
    private final JsonFactory factory = mapper.getFactory();

    @Test
    public void should_throwException_provided_emptyGeoJson() {
        String shapeJson = "{}";

        this.validateInput(shapeJson, Strings.EMPTY, "shape not included");
    }

    @Test
    public void should_throwException_parseInvalidPoint() {
        String shapeJson = getFeatureFromFile("input/invalid_point.json");
        this.validateInput(shapeJson, Strings.EMPTY, "unable to parse FeatureCollection");
    }

    @Test
    public void should_throwException_parseInvalidPoint_NaN() {
        String shapeJson = getFeatureFromFile("input/invalid_point_nan.json");
        this.validateInput(shapeJson, Strings.EMPTY, "unable to parse FeatureCollection");
    }

    @Test
    public void should_throwException_parseInvalidPoint_missingLatitude() {
        String shapeJson = getFeatureFromFile("input/invalid_point_missing_latitude.json");
        this.validateInput(shapeJson, Strings.EMPTY, "unable to parse FeatureCollection");
    }

    @Test
    public void should_throwException_missingMandatoryAttribute() {
        String shapeJson = getFeatureFromFile("input/missing_mandatory_attribute.json");
        this.validateInput(shapeJson, Strings.EMPTY, "must be a valid FeatureCollection");
    }

    @Test
    public void should_throwException_parseInvalidShape() {
        String shapeJson = getFeatureFromFile("input/invalid_shape.json");
        this.validateInput(shapeJson, Strings.EMPTY, "must be a valid FeatureCollection");
    }

    @Test
    public void should_parseValidPoint() {
        String shapeJson = getFeatureFromFile("input/valid_point.json");
        this.validateInput(shapeJson, shapeJson, Strings.EMPTY);
    }

    @Test
    public void should_parseValidMultiPoint() {
        String shapeJson = getFeatureFromFile("input/valid_multi_point.json");
        this.validateInput(shapeJson, shapeJson, Strings.EMPTY);
    }

    @Test
    public void should_parseValidLineString() {
        String shapeJson = getFeatureFromFile("input/valid_line_string.json");
        this.validateInput(shapeJson, shapeJson, Strings.EMPTY);
    }

    @Test
    public void should_parseValidMultiLineString() {
        String shapeJson = getFeatureFromFile("input/valid_multi_line_string.json");
        this.validateInput(shapeJson, shapeJson, Strings.EMPTY);
    }

    @Test
    public void should_parseValidPolygon() {
        String shapeJson = getFeatureFromFile("input/valid_polygon.json");
        this.validateInput(shapeJson, shapeJson, Strings.EMPTY);
    }

    @Test
    public void should_throwException_parseInvalidPolygon_malformedLatitude() {
        String shapeJson = getFeatureFromFile("input/invalid_polygon_malformed_latitude.json");
        this.validateInput(shapeJson, Strings.EMPTY, "unable to parse FeatureCollection");
    }

    @Test
    public void should_parseValidMultiPolygon() {
        String shapeJson = getFeatureFromFile("input/valid_multi_polygon.json");
        this.validateInput(shapeJson, shapeJson, Strings.EMPTY);
    }

    @Test
    public void should_parseValidGeometryCollection() {
        String shapeJson = getFeatureFromFile("input/valid_geometry_collection.json");
        this.validateInput(shapeJson, shapeJson, Strings.EMPTY);
    }

    @Test
    public void should_parseValidFeatureCollection() {
        String shapeJson = getFeatureFromFile("input/valid_feature_collection.json");
        this.validateInput(shapeJson, shapeJson, Strings.EMPTY);
    }

    @Test
    public void should_parseValidFeatureCollection_withZCoordinate() {
        String shapeJson = getFeatureFromFile("input/valid_feature_collection_with_z_coordinate.json");
        this.validateInput(shapeJson, shapeJson, Strings.EMPTY);
    }

    @Test
    public void should_throwException_parseUnsupportedType_feature() {
        String shapeJson = getFeatureFromFile("input/valid_feature_collection_with_z_coordinate.json");
        this.validateInput(shapeJson, shapeJson, Strings.EMPTY);
    }

    @Test
    public void shouldParseValidMultiPolygonWithZCoordinates() {
        String shapeJson = getFeatureFromFile("input/multi_polygon_with_z_coordinates.json");
        this.validateInput(shapeJson, shapeJson, Strings.EMPTY);
    }

    @Test
    public void shouldParseValidLineStringWithZCoordinates() {
        String shapeJson = getFeatureFromFile("input/line_string_with_z_coordinate.json");
        this.validateInput(shapeJson, shapeJson, Strings.EMPTY);
    }

    @Test
    public void shouldParseValidMultiLineStringWithZCoordinates() {
        String shapeJson = getFeatureFromFile("input/valid_milti_line_string_with_z_coordinate.json");
        this.validateInput(shapeJson, shapeJson, Strings.EMPTY);
    }


    private void validateInput(String shapeJson, String expectedParsedShape, String errorMessage) {
        try {
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> expectedShape = new Gson().fromJson(expectedParsedShape, type);
            FeatureCollection featureCollection = mapper.convertValue(expectedShape, FeatureCollection.class);

            JsonParser parser = factory.createParser(shapeJson);
            List<Feature> result = (List<Feature>) callPrivateMethod_ExtractFeature(parser);

            assertNotNull(result);
            assertTrue(Strings.isNullOrEmpty(errorMessage));
            assertEquals(featureCollection.getFeatures(), result);
        } catch (IllegalArgumentException e) {
            if (Strings.isNullOrEmpty(errorMessage)) {
                fail(String.format("error parsing valid feature-json %s", shapeJson));
            } else {
                assertThat(String.format("Incorrect error message for feature-json parsing [ %s ]", shapeJson), e.getMessage(), containsString(errorMessage));
            }
        } catch (IOException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @SneakyThrows
    private String getFeatureFromFile(String file) {
        InputStream inStream = this.getClass().getResourceAsStream("/geojson/parsing/" + file);
        assert inStream != null;
        BufferedReader br = new BufferedReader(new InputStreamReader(inStream));
        StringBuilder stringBuilder = new StringBuilder();
        String sCurrentLine;
        while ((sCurrentLine = br.readLine()) != null) {
            stringBuilder.append(sCurrentLine).append("\n");
        }
        return stringBuilder.toString();
    }

    public Object callPrivateMethod_ExtractFeature(JsonParser parser) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
        Method method = featureCollectionDeserializer.getClass().getDeclaredMethod("extractFeature", JsonParser.class);
        method.setAccessible(true);
        return method.invoke(featureCollectionDeserializer, parser);
    }
}
