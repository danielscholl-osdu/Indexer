package org.opengroup.osdu.indexer.model.geojson.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import org.opengroup.osdu.indexer.model.geojson.*;

import java.io.IOException;

public class GeometryCollectionSerializer extends JsonSerializer<GeometryCollection> {
    @Override
    public void serialize(GeometryCollection value, JsonGenerator jsonGenerator, SerializerProvider provider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("type", "geometrycollection");

        jsonGenerator.writeArrayFieldStart("geometries");
        for (GeoJsonObject shape : value.getGeometries()) {
            serializeGeoShape(shape, jsonGenerator);
        }
        jsonGenerator.writeEndArray();
        jsonGenerator.writeEndObject();
    }

    @Override
    public void serializeWithType(GeometryCollection value, JsonGenerator jsonGenerator, SerializerProvider provider, TypeSerializer typeSerializer)
            throws IOException, JsonProcessingException {

        serialize(value, jsonGenerator, provider);
    }

    private void serializeGeoShape(GeoJsonObject geoJsonObject, JsonGenerator jsonGenerator) throws IOException {
        if (geoJsonObject instanceof Point) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("type", "point");
            jsonGenerator.writeObjectField("coordinates", ((Point) geoJsonObject).getCoordinates());
            jsonGenerator.writeEndObject();
        } else if (geoJsonObject instanceof LineString) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("type", "linestring");
            jsonGenerator.writeObjectField("coordinates", ((LineString) geoJsonObject).getCoordinates());
            jsonGenerator.writeEndObject();
        } else if (geoJsonObject instanceof Polygon) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("type", "polygon");
            jsonGenerator.writeObjectField("coordinates", ((Polygon) geoJsonObject).getCoordinates());
            jsonGenerator.writeEndObject();
        } else if (geoJsonObject instanceof MultiPoint) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("type", "multipoint");
            jsonGenerator.writeObjectField("coordinates", ((MultiPoint) geoJsonObject).getCoordinates());
            jsonGenerator.writeEndObject();
        } else if (geoJsonObject instanceof MultiLineString) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("type", "multilinestring");
            jsonGenerator.writeObjectField("coordinates", ((MultiLineString) geoJsonObject).getCoordinates());
            jsonGenerator.writeEndObject();
        } else if (geoJsonObject instanceof MultiPolygon) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("type", "multipolygon");
            jsonGenerator.writeObjectField("coordinates", ((MultiPolygon) geoJsonObject).getCoordinates());
            jsonGenerator.writeEndObject();
        }
    }
}
