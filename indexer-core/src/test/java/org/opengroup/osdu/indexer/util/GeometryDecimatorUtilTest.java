package org.opengroup.osdu.indexer.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.SneakyThrows;
import org.junit.Test;
import org.opengroup.osdu.indexer.model.geojson.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GeometryUtilTest {
    Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();

    GeometryDecimatorUtil util = new GeometryDecimatorUtil();

    @Test
    public void test1() {
        String geoShapeJson = getGeoShapeFromFile("multi_line_string_projection_meter.json");
        Type type = new TypeToken<Map<String, Object>>() {}.getType();
        Map<String, Object> shapeObj = gson.fromJson(geoShapeJson, type);
        try {
            boolean decimated = util.decimateShapeObj(shapeObj);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }


    }

    private void replaceCoordinates(Map<String, Object> featureCollection, Map<String, Object> geometryCollection) {
        ArrayList features =(ArrayList)featureCollection.get("features");
        for(Object feature: features) {

        }
    }

    private int getTotalSize(FeatureCollection featureCollection) {
        int size = 0;
        for (Feature feature: featureCollection) {
            GeoJsonObject geometry = feature.getGeometry();
            if(geometry instanceof MultiLineString) {
                MultiLineString multiLineString = ((MultiLineString) geometry);
                for(List<Position> coordinates : multiLineString.getCoordinates()) {
                    size += coordinates.size();
                }
            }
            else if(geometry instanceof LineString) {
                LineString lineString = ((LineString)geometry);
                size += lineString.getCoordinates().size();
            }
        }
        return size;
    }


    @SneakyThrows
    private String getGeoShapeFromFile(String file) {
        InputStream inStream = this.getClass().getResourceAsStream("/geometry-decimation/" + file);
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
