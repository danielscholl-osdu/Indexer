package org.opengroup.osdu.indexer.model.geojson.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengroup.osdu.indexer.model.geojson.*;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

@RunWith(SpringRunner.class)
public class GeometryCollectionSerializerTest {
    private TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {};
    private static ObjectMapper objectMapper;

    @BeforeClass
    public static void setup() {
        objectMapper = new ObjectMapper();
        objectMapper.registerSubtypes(new NamedType(GeometryCollection.class, "geometrycollection"));
        objectMapper.registerSubtypes(new NamedType(Polygon.class, "polygon"));
        objectMapper.registerSubtypes(new NamedType(MultiPolygon.class, "multipolygon"));
        objectMapper.registerSubtypes(new NamedType(LineString.class, "linestring"));
        objectMapper.registerSubtypes(new NamedType(MultiLineString.class, "multilinestring"));
        objectMapper.registerSubtypes(new NamedType(Point.class, "point"));
        objectMapper.registerSubtypes(new NamedType(MultiPoint.class, "multipoint"));
    }

    @Test
    public void deserialize_polyline() throws JsonProcessingException {
        String shapeJson = getGeoShapeFromFile("geometrycollection_linestring.json");
        GeometryCollection geometryCollection = objectMapper.readValue(shapeJson, GeometryCollection.class);
        Assert.assertNotNull(geometryCollection);
        Assert.assertNotNull(geometryCollection.getGeometries());
        Assert.assertTrue(geometryCollection.getGeometries().size() > 0);
        Assert.assertTrue(geometryCollection.getGeometries().get(0) instanceof LineString);
    }

    @Test
    public void deserialize_multipolyline() throws JsonProcessingException {
        String shapeJson = getGeoShapeFromFile("geometrycollection_multilinestring.json");
        GeometryCollection geometryCollection = objectMapper.readValue(shapeJson, GeometryCollection.class);
        Assert.assertNotNull(geometryCollection);
        Assert.assertNotNull(geometryCollection.getGeometries());
        Assert.assertTrue(geometryCollection.getGeometries().size() > 0);
        Assert.assertTrue(geometryCollection.getGeometries().get(0) instanceof MultiLineString);
    }

    @Test
    public void deserialize_polygon() throws JsonProcessingException {
        String shapeJson = getGeoShapeFromFile("geometrycollection_polygon.json");
        GeometryCollection geometryCollection = objectMapper.readValue(shapeJson, GeometryCollection.class);
        Assert.assertNotNull(geometryCollection);
        Assert.assertNotNull(geometryCollection.getGeometries());
        Assert.assertTrue(geometryCollection.getGeometries().size() > 0);
        Assert.assertTrue(geometryCollection.getGeometries().get(0) instanceof Polygon);
    }

    @Test
    public void deserialize_multipolygon() throws JsonProcessingException {
        String shapeJson = getGeoShapeFromFile("geometrycollection_multipolygon.json");
        GeometryCollection geometryCollection = objectMapper.readValue(shapeJson, GeometryCollection.class);
        Assert.assertNotNull(geometryCollection);
        Assert.assertNotNull(geometryCollection.getGeometries());
        Assert.assertTrue(geometryCollection.getGeometries().size() > 0);
        Assert.assertTrue(geometryCollection.getGeometries().get(0) instanceof MultiPolygon);
    }

    @Test
    public void deserialize_point() throws JsonProcessingException {
        String shapeJson = getGeoShapeFromFile("geometrycollection_point.json");
        GeometryCollection geometryCollection = objectMapper.readValue(shapeJson, GeometryCollection.class);
        Assert.assertNotNull(geometryCollection);
        Assert.assertNotNull(geometryCollection.getGeometries());
        Assert.assertTrue(geometryCollection.getGeometries().size() > 0);
        Assert.assertTrue(geometryCollection.getGeometries().get(0) instanceof Point);
    }

    @Test
    public void deserialize_multipoint() throws JsonProcessingException {
        String shapeJson = getGeoShapeFromFile("geometrycollection_multipoint.json");
        GeometryCollection geometryCollection = objectMapper.readValue(shapeJson, GeometryCollection.class);
        Assert.assertNotNull(geometryCollection);
        Assert.assertNotNull(geometryCollection.getGeometries());
        Assert.assertTrue(geometryCollection.getGeometries().size() > 0);
        Assert.assertTrue(geometryCollection.getGeometries().get(0) instanceof MultiPoint);
    }

    @SneakyThrows
    private String getGeoShapeFromFile(String file) {
        InputStream inStream = this.getClass().getResourceAsStream("/geo/decimator/" + file);
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
