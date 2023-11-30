package org.opengroup.osdu.indexer.util.geo.extractor;

import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.indexer.model.geojson.jackson.GeoJsonConstants;
import org.springframework.stereotype.Component;
import org.opengroup.osdu.indexer.model.geojson.jackson.GeoJsonConstants;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;
import java.lang.ClassCastException;

@Component
public class PointExtractor {

    @Inject
    private JaxRsDpsLog log;

    public ArrayList<Double> extractFirstPointFromFeatureCollection(Map<String, Object> featureCollection) {

        if (featureCollection == null) {
            return new ArrayList<>();
        }

        if (!featureCollection.containsKey(GeoJsonConstants.FEATURES)) {
            return new ArrayList<>();
        }

        List<Map> features = (List<Map>) featureCollection.get(GeoJsonConstants.FEATURES);

        if (features.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String,Map> firstFeature = (Map<String,Map>) features.get(0);
        Map geometry = (Map) firstFeature.get("geometry");

        ArrayList<Double> point = extractFirstPointFromGeometry(geometry);

        if (point.size() < 2) {
          return new ArrayList<>();
        }

        return point;
    }

    private ArrayList<Double> extractFirstPointFromGeometry(Map<String, Object> geometry) {
        String type = (String) geometry.get(GeoJsonConstants.TYPE);
        type = type.replace("AnyCrs", "");

        ArrayList coordinates = (ArrayList<Object>) geometry.get(GeoJsonConstants.COORDINATES);

        switch (type) {
            case "Point":
                return getNestedArrayList(coordinates, 0);
            case "LineString":
            case "MultiPoint":
                return getNestedArrayList(coordinates, 1);
            case "Polygon":
            case "MultiLineString":
                return getNestedArrayList(coordinates, 2);
            case "MultiPolygon":
                return getNestedArrayList(coordinates, 3);
            case "GeometryCollection":
                List<Map> geometries = (List<Map>) geometry.get(GeoJsonConstants.GEOMETRIES);
                return extractFirstPointFromGeometry((Map) geometries.get(0));
            default:
                return new ArrayList<>();
        }
    }

    private ArrayList<Double> getNestedArrayList(ArrayList arr, int level) {
        // Initial assignment
        ArrayList temporaryNestedArray = arr;

        // Iteratively cast and retrieve nested ArrayList up to the specified level
        for (int i = 0; i < level; ++i) {
            ArrayList<Object> nestedObjArray = (ArrayList<Object>) temporaryNestedArray;
            temporaryNestedArray = (ArrayList<Object>) nestedObjArray.get(0);
        }

        try {
            // Explicit cast to ArrayList<Number>
            ArrayList<Number> numbers = (ArrayList<Number>) temporaryNestedArray;

            // Use stream to convert each Number to Double
            List<Double> doubleList = numbers.stream().map(Number::doubleValue).collect(Collectors.toList());

            // Create a new ArrayList<Double> from the List<Double>
            return new ArrayList<>(doubleList);
        } catch (ClassCastException e) {
            // Return an empty ArrayList<Double> in case of a ClassCastException
            this.log.warning(String.format("nestedArray: %s | error casting to numeric value | error: %s", temporaryNestedArray, e.getMessage()));
            return new ArrayList<>();
        }
    }

}
