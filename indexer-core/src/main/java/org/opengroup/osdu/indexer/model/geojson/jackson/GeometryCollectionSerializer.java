/*
 * Copyright © Schlumberger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.indexer.model.geojson.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
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
            throws IOException {

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
