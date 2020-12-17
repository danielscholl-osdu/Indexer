// Copyright 2017-2020, Schlumberger
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

package org.opengroup.osdu.indexer.schema.converter;

import org.junit.Test;
import org.mockito.Mockito;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.indexer.schema.converter.config.SchemaConverterPropertiesConfig;
import org.opengroup.osdu.indexer.schema.converter.tags.AllOfItem;
import org.opengroup.osdu.indexer.schema.converter.tags.Definition;
import org.opengroup.osdu.indexer.schema.converter.tags.Definitions;
import org.opengroup.osdu.indexer.schema.converter.tags.TypeProperty;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class PropertiesProcessorTest {

    private static final String PATH = "given_path";
    private static final String DEFINITIONS_PREFIX = "#/definitions/";

    @Test(expected = AppException.class)
    public void should_fail_on_unknown_reference_definition() {
        JaxRsDpsLog log = Mockito.mock(JaxRsDpsLog.class);

        new PropertiesProcessor(Mockito.mock(Definitions.class), log, new SchemaConverterPropertiesConfig())
                .processRef(DEFINITIONS_PREFIX + "unknownDefinition");
    }

    @Test
    public void should_not_process_special_reference() {
        JaxRsDpsLog log = Mockito.mock(JaxRsDpsLog.class);

        assertFalse(new PropertiesProcessor(null, log, new SchemaConverterPropertiesConfig())
                .processRef(DEFINITIONS_PREFIX + "anyCrsGeoJsonFeatureCollection").findAny().isPresent());
    }

    @Test
    public void should_return_special_type() {
        JaxRsDpsLog log = Mockito.mock(JaxRsDpsLog.class);

        String res = new PropertiesProcessor(null, PATH, log, new SchemaConverterPropertiesConfig())
                .processRef(DEFINITIONS_PREFIX + "core_dl_geopoint").map(Object::toString).reduce("", String::concat);
        assertEquals("{path=" + PATH + ", kind=core:dl:geopoint:1.0.0}", res);
    }

    @Test
    public void should_process_definition_correctly() {
        JaxRsDpsLog log = Mockito.mock(JaxRsDpsLog.class);

        Definitions definitions = new Definitions();
        Definition definition = new Definition();

        TypeProperty property = new TypeProperty();
        property.setFormat("string");
        String propertyName = "propName";

        Map<String, TypeProperty> properties = new LinkedHashMap<>();
        properties.put(propertyName, property);

        definition.setProperties(properties);

        String defName = "defName";
        definitions.add(defName, definition);

        String res = new PropertiesProcessor(definitions, PATH, log, new SchemaConverterPropertiesConfig())
                .processRef(DEFINITIONS_PREFIX + defName).map(Object::toString).reduce("", String::concat);
        assertEquals(res, "{path="+ PATH + "." + propertyName + ", kind=string}");
    }

    @Test
    public void should_return_int_item() {
        JaxRsDpsLog log = Mockito.mock(JaxRsDpsLog.class);

        AllOfItem allOfItem = new AllOfItem();

        TypeProperty property = new TypeProperty();
        property.setFormat("integer");

        Map<String, TypeProperty> properties = new LinkedHashMap<>();
        properties.put(PATH, property);
        allOfItem.setProperties(properties);

        String res = new PropertiesProcessor(Mockito.mock(Definitions.class), log, new SchemaConverterPropertiesConfig())
                .processItem(allOfItem).map(Object::toString).reduce("", String::concat);
        assertEquals("{path=" + PATH + ", kind=int}", res);
    }
}