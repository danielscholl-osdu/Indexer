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

package org.opengroup.osdu.indexer.util.geo.decimator;

import org.opengroup.osdu.indexer.model.geojson.*;
import org.springframework.stereotype.Component;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@Component
public class GeometryDecimator {
    private static final double NORMAL_SHAPE_DECIMATION_EPSILON = 10; // meters
    private static final double DEGREES_TO_METERS = 100000; // approximate using 100km per degree
    private static final int MAX_SHAPE_POINT_COUNT_FOR_LINE_DECIMATION = 300000;
    private static final double TOLERANCE_FACTOR = 1.2;

    @Inject
    private DouglasPeuckerReducer reducer;

    public boolean decimate(GeometryCollection geometryCollection) {
        return decimate(geometryCollection, NORMAL_SHAPE_DECIMATION_EPSILON);
    }

    private boolean decimate(GeometryCollection geometryCollection, double epsilon) {
        if(geometryCollection == null || geometryCollection.getGeometries() == null)
            return false;

        boolean decimated = false;
        for(GeoJsonObject geoJsonObject: geometryCollection.getGeometries()) {
            decimated |= decimateBasicGeometry(geoJsonObject, epsilon);
        }
        return decimated;
    }

    private boolean decimate(MultiLineString geometry, double epsilon) {
        if(geometry == null || geometry.getCoordinates() == null)
            return false;

        boolean decimated = false;
        for(List<Position> coordinates : geometry.getCoordinates()) {
            decimated |= decimateLine(coordinates, epsilon);
        }
        return decimated;
    }

    private boolean decimate(LineString geometry, double epsilon) {
        if(geometry == null)
            return false;

        return decimateLine(geometry.getCoordinates(), epsilon);
    }

    private boolean decimate(MultiPolygon geometry, double epsilon) {
        if(geometry == null || geometry.getCoordinates() == null)
            return false;

        boolean decimated = false;
        for(List<List<Position>> polygon : geometry.getCoordinates()) {
            for(List<Position> coordinates : polygon) {
                decimated |= decimateLine(coordinates, epsilon);
            }
        }
        return decimated;
    }

    private boolean decimate(Polygon geometry, double epsilon) {
        if(geometry == null || geometry.getCoordinates() == null)
            return false;

        boolean decimated = false;
        for(List<Position> coordinates : geometry.getCoordinates()) {
            decimated |= decimateLine(coordinates, epsilon);
        }
        return decimated;
    }

    private boolean decimate(Point geometry, double epsilon) {
        return false;
    }

    private boolean decimate(MultiPoint geometry, double epsilon) {
        return false;
    }

    private boolean decimateBasicGeometry(GeoJsonObject geometry, double epsilon) {
        if(geometry instanceof MultiLineString) {
            return decimate((MultiLineString) geometry, epsilon);
        }
        else if(geometry instanceof LineString) {
            return decimate((LineString)geometry, epsilon);
        }
        else if(geometry instanceof MultiPolygon) {
            return decimate((MultiPolygon)geometry, epsilon);
        }
        else if(geometry instanceof Polygon) {
            return decimate((Polygon)geometry, epsilon);
        }
        else if(geometry instanceof Point) {
            return decimate((Point)geometry, epsilon);
        }
        else if(geometry instanceof MultiPoint) {
            return decimate((MultiPoint)geometry, epsilon);
        }
        else
            return false;
    }

    private boolean decimateLine(List<Position> coordinates, double epsilon) {
        if(coordinates == null || coordinates.size() < 3)
            return false;

        // Douglas/Peucker algorithm is expensive, apply simple sampling if the line has too many points
        coordinates = downSamplePoints(coordinates);

        List<Integer> pointIndexes = reducer.getPointIndexesToKeep(coordinates, DEGREES_TO_METERS, epsilon);

        boolean decimated = (coordinates.size() > pointIndexes.size());
        if(decimated) {
            List<Position> decimatedCoordinates = new ArrayList<>();
            for(int i : pointIndexes) {
                decimatedCoordinates.add(coordinates.get(i));
            }

            coordinates.clear();
            coordinates.addAll(decimatedCoordinates);
        }
        return decimated;
    }

    private List<Position> downSamplePoints(List<Position> coordinates) {
        //Don't sample it if the number of point is not much larger than MaxShapePointCountForLineDecimation
        if (coordinates.size() <= MAX_SHAPE_POINT_COUNT_FOR_LINE_DECIMATION * TOLERANCE_FACTOR) {
            return coordinates;
        }

        List<Position> sampledPoints = new ArrayList<>();
        int interval = (int)Math.ceil(coordinates.size() / (double)MAX_SHAPE_POINT_COUNT_FOR_LINE_DECIMATION);
        for(int i = 0; i < coordinates.size(); i += interval) {
            sampledPoints.add(coordinates.get(i));
        }
        // Add the last point
        sampledPoints.add(coordinates.get(coordinates.size() -1));
        return sampledPoints;
    }


}
