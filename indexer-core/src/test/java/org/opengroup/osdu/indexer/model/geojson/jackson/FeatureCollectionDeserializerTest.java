package org.opengroup.osdu.indexer.model.geojson.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.google.gson.internal.LinkedTreeMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengroup.osdu.indexer.model.geojson.Feature;
import org.opengroup.osdu.indexer.model.geojson.FeatureCollection;
import org.opengroup.osdu.indexer.model.geojson.LineString;
import org.opengroup.osdu.indexer.model.geojson.Position;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.*;

@RunWith(SpringRunner.class)
public class FeatureCollectionDeserializerTest {

    private ObjectMapper mapper = new ObjectMapper();

    private FeatureCollection featureCollection = createFeatureCollection();

    @Test
    public void should_returnFeatureList_extractFeatureTest(){
        Map<String, Object> objectMap = createObjectMap();

        try {
            FeatureCollection collection = mapper.readValue(mapper.writeValueAsString(objectMap), FeatureCollection.class);
            assertEquals(featureCollection, collection);
        } catch (JsonProcessingException e) {
            fail("unable to parse FeatureCollection");
        }
    }

    @Test
    public void should_throwException_extractFeatureTest(){
        assertThrows(InvalidTypeIdException.class, () -> mapper.readValue(mapper.writeValueAsString(new LinkedTreeMap()), FeatureCollection.class));
    }

    @Test
    public void should_throwException_whenInvalidFeature_extractFeatureTest(){
        Map<String, Object> objectMap = createInvalidObjectMap();
        assertThrows(InvalidTypeIdException.class, () -> mapper.readValue(mapper.writeValueAsString(objectMap), FeatureCollection.class));
    }

    @Test
    public void should_returnFeatureList_withoutGeometry_extractFeatureTest() {
        FeatureCollection featureCollection = createFeatureCollectionWithoutGeometry();
        Map<String, Object> objectMap = createInvalidObjectMapWithoutGeometry();

        try {
            FeatureCollection collection = mapper.readValue(mapper.writeValueAsString(objectMap), FeatureCollection.class);
            assertEquals(featureCollection, collection);
        } catch (JsonProcessingException e) {
            fail("unable to parse FeatureCollection");
        }
    }

    private FeatureCollection createFeatureCollection(){
        Position position = new Position(90, 80);
        List<Position> positions = new ArrayList<>();
        positions.add(position);

        LineString lineString = new LineString();
        lineString.setCoordinates(positions);
        Feature feature = new Feature();
        feature.setGeometry(lineString);

        List<Feature> futures = new ArrayList<>();
        futures.add(feature);
        FeatureCollection featureCollection = new FeatureCollection();
        featureCollection.setFeatures(futures);

        return featureCollection;
    }

    private FeatureCollection createFeatureCollectionWithoutGeometry(){
        Feature feature = new Feature();

        List<Feature> futures = new ArrayList<>();
        futures.add(feature);
        FeatureCollection featureCollection = new FeatureCollection();
        featureCollection.setFeatures(futures);

        return featureCollection;
    }

    private Map<String, Object> createObjectMap(){
        Map<String, String> type = new LinkedTreeMap<>();
        type.put("type", "Feature");

        List<Double> objectList2 = new ArrayList<>();
        objectList2.add(90d);
        objectList2.add(80d);
        List<Object> objectList1 = new ArrayList<>();
        objectList1.add(objectList2);
        LinkedTreeMap<String, Object> geometry = new LinkedTreeMap<>();
        geometry.put("type", "LineString");
        geometry.put("coordinates", objectList1);

        LinkedTreeMap<String, Object> objectList11 = new LinkedTreeMap<>();
        objectList11.put("type", "Feature");
        objectList11.put("geometry", geometry);
        objectList11.put("property", new LinkedTreeMap<>());

        List<Object> objectList = new ArrayList<>();
        objectList.add(objectList11);

        Map<String, Object> objectMap = new LinkedTreeMap();
        objectMap.put("features", objectList);
        objectMap.put("type", "FeatureCollection");

        return objectMap;
    }

    private Map<String, Object> createInvalidObjectMap(){
        Map<String, String> type = new LinkedTreeMap<>();
        type.put("type", "Feature");

        List<Double> objectList2 = new ArrayList<>();
        objectList2.add(90d);
        objectList2.add(80d);
        List<Object> objectList1 = new ArrayList<>();
        objectList1.add(objectList2);
        LinkedTreeMap<String, Object> geometry = new LinkedTreeMap<>();
        geometry.put("type", "LineString");
        geometry.put("coordinates", objectList1);

        LinkedTreeMap<String, Object> objectList11 = new LinkedTreeMap<>();
        objectList11.put("type", "FeatuIre");
        objectList11.put("geometry", geometry);
        objectList11.put("property", new LinkedTreeMap<>());

        List<Object> objectList = new ArrayList<>();
        objectList.add(objectList11);

        Map<String, Object> objectMap = new LinkedTreeMap();
        objectMap.put("features", objectList);
        objectMap.put("type", "FeatureCollection");

        return objectMap;
    }

    private Map<String, Object> createInvalidObjectMapWithoutGeometry(){
        Map<String, String> type = new LinkedTreeMap<>();
        type.put("type", "Feature");

        LinkedTreeMap<String, Object> objectList11 = new LinkedTreeMap<>();
        objectList11.put("type", "Feature");
        objectList11.put("property", new LinkedTreeMap<>());

        List<Object> objectList = new ArrayList<>();
        objectList.add(objectList11);

        Map<String, Object> objectMap = new LinkedTreeMap();
        objectMap.put("features", objectList);
        objectMap.put("type", "FeatureCollection");

        return objectMap;
    }
}