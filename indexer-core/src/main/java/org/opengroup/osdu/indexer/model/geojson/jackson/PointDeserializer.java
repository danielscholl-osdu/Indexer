package org.opengroup.osdu.indexer.model.geojson.jackson;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.opengroup.osdu.indexer.model.geojson.Point;
import org.opengroup.osdu.indexer.model.geojson.Position;

import java.io.IOException;

public class PointDeserializer extends JsonDeserializer<Point> {
    @Override
    public Point deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
        ObjectCodec codec = jsonParser.getCodec();
        JsonNode coordinatesNode = codec.readTree(jsonParser);
        JsonNode positionNode = coordinatesNode.get("coordinates");
        if(positionNode == null) {
            throw new JsonParseException(jsonParser, "Missing coordinates field in the point");
        }

        Position position = codec.treeToValue(positionNode, Position.class);
        if(position.hasAltitude())
            return  new Point(position.getLongitude(), position.getLatitude(), position.getAltitude());
        else
            return  new Point(position.getLongitude(), position.getLatitude());
    }
}
