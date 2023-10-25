package org.opengroup.osdu.indexer.util.geo.extractor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.lang.ClassCastException;

@Component
public class PointExtractor {

    public ArrayList<Double> extractFirstPointFromFeatureCollection(Map<String, Object> featureCollection) {

        if (featureCollection == null) {
            return new ArrayList<>();
        }

        if (!featureCollection.containsKey("features")) {
            return new ArrayList<>();
        }

        List features = (List) featureCollection.get("features");

        if (features.isEmpty()) {
            return new ArrayList<>();
        }

        Map firstFeature = (Map) features.get(0);
        Map geometry = (Map) firstFeature.get("geometry");

        ArrayList<Double> point = extractFirstPointFromGeometry(geometry);

        if (point.size() < 2) {
          return new ArrayList<>();
        }

        return point;
    }

    private ArrayList<Double> extractFirstPointFromGeometry(Map<String, Object> geometry) {

        String type = (String) geometry.get("type");
        type = type.replace("AnyCrs", "");

        ArrayList coordinates = (ArrayList<Object>) geometry.get("coordinates");

        switch (type) {
            case "Point":
                return getNestedArrayList(coordinates, 0);
            case "LineString":
                return getNestedArrayList(coordinates, 1);
            case "Polygon":
                return getNestedArrayList(coordinates, 2);
            case "MultiPoint":
                return getNestedArrayList(coordinates, 1);
            case "MultiLineString":
                return getNestedArrayList(coordinates, 2);
            case "MultiPolygon":
                return getNestedArrayList(coordinates, 3);
            case "GeometryCollection":
                List geometries = (List) geometry.get("geometries");
                return extractFirstPointFromGeometry((Map) geometries.get(0));
            default:
                return new ArrayList<>();
        }
    }

    private ArrayList<Double> getNestedArrayList(ArrayList arr, int level) {
        ArrayList tmp = arr;
        for (int i = 0; i < level; ++i) {
            tmp = (ArrayList<Object>)((ArrayList<Object>) tmp).get(0);
        }
        try {
          return new ArrayList<>(((ArrayList<Number>) tmp).stream().map(Number::doubleValue).collect(Collectors.toList()));
        } catch (ClassCastException e) {
          return new ArrayList<>();
        }
    }

}
