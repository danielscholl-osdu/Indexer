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

package org.opengroup.osdu.indexer.service;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.model.indexer.IndexingStatus;
import org.opengroup.osdu.core.common.model.indexer.JobStatus;
import org.opengroup.osdu.indexer.util.parser.DateTimeParser;
import org.opengroup.osdu.indexer.util.parser.GeoShapeParser;
import org.opengroup.osdu.indexer.util.parser.NumberParser;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import javax.inject.Inject;
import java.lang.reflect.Type;
import java.util.Map;

@Service
@RequestScope
public class AttributeParsingServiceImpl implements IAttributeParsingService {

    private static final String GEOJSON = "GeoJSON";

    @Inject
    private NumberParser numberParser;
    @Inject
    private DateTimeParser dateTimeParser;
    @Inject
    private GeoShapeParser geoShapeParser;
    @Inject
    private GeometryConversionService geometryConversionService;
    @Inject
    private JobStatus jobStatus;

    @Override
    public void tryParseInteger(String recordId, String attributeName, Object attributeVal, Map<String, Object> dataMap) {
        try {
            int parsedInteger = this.numberParser.parseInteger(attributeName, attributeVal);
            dataMap.put(attributeName, parsedInteger);
        } catch (IllegalArgumentException e) {
            jobStatus.addOrUpdateRecordStatus(recordId, IndexingStatus.WARN, HttpStatus.SC_BAD_REQUEST, e.getMessage(), String.format("record-id: %s | %s", recordId, e.getMessage()));
        }
    }

    @Override
    public void tryParseLong(String recordId, String attributeName, Object attributeVal, Map<String, Object> dataMap) {
        try {
            long parsedLong = this.numberParser.parseLong(attributeName, attributeVal);
            dataMap.put(attributeName, parsedLong);
        } catch (IllegalArgumentException e) {
            jobStatus.addOrUpdateRecordStatus(recordId, IndexingStatus.WARN, HttpStatus.SC_BAD_REQUEST, e.getMessage(), String.format("record-id: %s | %s", recordId, e.getMessage()));
        }
    }

    @Override
    public void tryParseFloat(String recordId, String attributeName, Object attributeVal, Map<String, Object> dataMap) {
        try {
            float parsedFloat = this.numberParser.parseFloat(attributeName, attributeVal);
            dataMap.put(attributeName, parsedFloat);
        } catch (IllegalArgumentException e) {
            jobStatus.addOrUpdateRecordStatus(recordId, IndexingStatus.WARN, HttpStatus.SC_BAD_REQUEST, e.getMessage(), String.format("record-id: %s | %s", recordId, e.getMessage()));
        }
    }

    @Override
    public void tryParseDouble(String recordId, String attributeName, Object attributeVal, Map<String, Object> dataMap) {
        try {
            double parsedDouble = this.numberParser.parseDouble(attributeName, attributeVal);
            dataMap.put(attributeName, parsedDouble);
        } catch (IllegalArgumentException e) {
            jobStatus.addOrUpdateRecordStatus(recordId, IndexingStatus.WARN, HttpStatus.SC_BAD_REQUEST, e.getMessage(), String.format("record-id: %s | %s", recordId, e.getMessage()));
        }
    }

    @Override
    public void tryParseBoolean(String recordId, String attributeName, Object attributeVal, Map<String, Object> dataMap) {
        String val = attributeVal == null ? null : String.valueOf(attributeVal);
        dataMap.put(attributeName, Boolean.parseBoolean(val));
    }

    @Override
    public void tryParseDate(String recordId, String attributeName, Object attributeVal, Map<String, Object> dataMap) {
        String val = attributeVal == null ? null : String.valueOf(attributeVal);
        if (Strings.isNullOrEmpty(val)) {
            // skip indexing
            return;
        }

        String utcDate = this.dateTimeParser.convertDateObjectToUtc(val);
        if (Strings.isNullOrEmpty(utcDate)) {
            String parsingError = String.format("datetime parsing error: unknown format for attribute: %s | value: %s", attributeName, attributeVal);
            jobStatus.addOrUpdateRecordStatus(recordId, IndexingStatus.WARN, HttpStatus.SC_BAD_REQUEST, parsingError, String.format("record-id: %s | %s", recordId, parsingError));
        } else {
            dataMap.put(attributeName, utcDate);
        }
    }

    @Override
    public void tryParseGeopoint(String recordId, String attributeName, Map<String, Object> storageRecordData, Map<String, Object> dataMap) {

        Object attributeVal = storageRecordData.get(attributeName);

        try {
            Type type = new TypeToken<Map<String, Double>>() {}.getType();
            Map<String, Double> positionMap = new Gson().fromJson(attributeVal.toString(), type);

            if (positionMap == null || positionMap.isEmpty()) return;

            Map<String, Double> position = this.geometryConversionService.tryGetGeopoint(positionMap);

            if (position == null || position.isEmpty()) return;

            dataMap.put(attributeName, position);

            // check if geo shape is not there and if it is not then create it in the schema as well as create the data.
            LinkedTreeMap<String, Object> map = (LinkedTreeMap) storageRecordData.get(GEOJSON);
            if (map == null || map.isEmpty()) {
                Map<String, Object> geometry = this.geometryConversionService.getGeopointGeoJson(positionMap);

                if (geometry == null) return;

                dataMap.put(DATA_GEOJSON_TAG, geometry);
            }
        } catch (JsonSyntaxException | IllegalArgumentException e) {
            String parsingError = String.format("geopoint parsing error: %s attribute: %s | value: %s", e.getMessage(), attributeName, attributeVal);
            jobStatus.addOrUpdateRecordStatus(recordId, IndexingStatus.WARN, HttpStatus.SC_BAD_REQUEST, parsingError, String.format("record-id: %s | %s", recordId, parsingError));
        }
    }

    @Override
    public void tryParseGeojson(String recordId, String attributeName, Object attributeVal, Map<String, Object> dataMap) {

        try {
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> geoJsonMap = new Gson().fromJson(attributeVal.toString(), type);

            if (geoJsonMap == null || geoJsonMap.isEmpty()) return;

            this.geoShapeParser.parseGeoJson(geoJsonMap);

            dataMap.put(attributeName, geoJsonMap);
        } catch (JsonSyntaxException | IllegalArgumentException e) {
            String parsingError = String.format("geo-json shape parsing error: %s attribute: %s", e.getMessage(), attributeName);
            jobStatus.addOrUpdateRecordStatus(recordId, IndexingStatus.WARN, HttpStatus.SC_BAD_REQUEST, parsingError, String.format("record-id: %s | %s", recordId, parsingError));
        }
    }
}

