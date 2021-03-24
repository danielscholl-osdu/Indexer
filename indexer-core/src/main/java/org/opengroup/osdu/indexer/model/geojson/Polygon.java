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

package org.opengroup.osdu.indexer.model.geojson;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;

@NoArgsConstructor
public class Polygon extends Geometry<List<Position>> {

    public Polygon(List<Position> polygon) {
        add(polygon);
    }

    public Polygon(Position... polygon) {
        add(Arrays.asList(polygon));
    }

    public void setExteriorRing(List<Position> points) {
        if (coordinates.isEmpty()) {
            coordinates.add(0, points);
        } else {
            coordinates.set(0, points);
        }
    }

    @JsonIgnore
    public List<Position> getExteriorRing() {
        assertExteriorRing();
        return coordinates.get(0);
    }

    @JsonIgnore
    public List<List<Position>> getInteriorRings() {
        assertExteriorRing();
        return coordinates.subList(1, coordinates.size());
    }

    public List<Position> getInteriorRing(int index) {
        assertExteriorRing();
        return coordinates.get(1 + index);
    }

    public void addInteriorRing(List<Position> points) {
        assertExteriorRing();
        coordinates.add(points);
    }

    public void addInteriorRing(Position... points) {
        assertExteriorRing();
        coordinates.add(Arrays.asList(points));
    }

    private void assertExteriorRing() {
        if (coordinates.isEmpty())
            throw new RuntimeException("No exterior ring defined");
    }
}
