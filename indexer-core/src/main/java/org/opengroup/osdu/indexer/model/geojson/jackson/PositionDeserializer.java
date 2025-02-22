// Copyright © Schlumberger
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

package org.opengroup.osdu.indexer.model.geojson.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.opengroup.osdu.indexer.model.geojson.Position;

import java.io.IOException;

public class PositionDeserializer extends JsonDeserializer<Position> {

    @Override
    public Position deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException {
        if (jsonParser.isExpectedStartArrayToken()) {
            return deserializeArray(jsonParser, context);
        }
        throw JsonMappingException.from(context, "Cannot deserialize instance of " + Position.class.getName());
    }

    protected Position deserializeArray(JsonParser jsonParser, DeserializationContext context) throws IOException {
        Position node = new Position();
        node.setLongitude(extractDouble(jsonParser, context, false));
        node.setLatitude(extractDouble(jsonParser, context, false));
        node.setAltitude(extractDouble(jsonParser, context, true));
        return node;
    }

    private double extractDouble(JsonParser jsonParser, DeserializationContext context, boolean optional) throws IOException {
        JsonToken token = jsonParser.nextToken();
        if (token == null) {
            if (optional)
                return Double.NaN;
            else
                throw JsonMappingException.from(context, "Unexpected end-of-input when binding data into Position");
        } else {
            switch (token) {
                case END_ARRAY:
                    if (optional)
                        return Double.NaN;
                    else
                        throw JsonMappingException.from(context, "Unexpected end-of-input when binding data into Position");
                case VALUE_NUMBER_FLOAT:
                    return jsonParser.getDoubleValue();
                case VALUE_NUMBER_INT:
                    return jsonParser.getLongValue();
                default:
                    throw JsonMappingException.from(context, "Unexpected token (" + token.name() + ") when binding data into Position");
            }
        }
    }
}
