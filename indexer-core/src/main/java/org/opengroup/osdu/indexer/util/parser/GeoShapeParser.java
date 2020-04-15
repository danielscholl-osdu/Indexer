// Copyright 2017-2019, Schlumberger
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

package org.opengroup.osdu.indexer.util.parser;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.geo.builders.ShapeBuilder;
import org.elasticsearch.common.geo.parsers.ShapeParser;
import org.elasticsearch.common.xcontent.DeprecationHandler;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.locationtech.spatial4j.exception.InvalidShapeException;
import org.locationtech.spatial4j.shape.Shape;
import org.opengroup.osdu.core.common.search.Preconditions;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.io.IOException;
import java.util.Map;

@Component
@RequestScope
public class GeoShapeParser {

    public String parseGeoJson(Map<String, Object> geoShapeObject) {

        Preconditions.checkNotNull(geoShapeObject, "geoShapeObject cannot be null");

        try {
            // use elasticsearch's ShapeParser to validate shape
            ShapeBuilder<?, ?> shapeBuilder = getShapeBuilderFromObject(geoShapeObject);
            Shape shape = shapeBuilder.buildS4J();
            if (shape == null) {
                throw new IllegalArgumentException("unable to parse shape");
            }

            return shapeBuilder.toString().replaceAll("\\r", "").replaceAll("\\n", "");
        } catch (ElasticsearchParseException | InvalidShapeException | IOException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    private ShapeBuilder<?, ?> getShapeBuilderFromObject(Map<String, Object> object) throws IOException {
        XContentBuilder contentBuilder = JsonXContent.contentBuilder().value(object);

        XContentParser parser = JsonXContent.jsonXContent.createParser(
                NamedXContentRegistry.EMPTY, DeprecationHandler.THROW_UNSUPPORTED_OPERATION,
                BytesReference.bytes(contentBuilder).streamInput()
        );

        parser.nextToken();
        return ShapeParser.parse(parser);
    }
}