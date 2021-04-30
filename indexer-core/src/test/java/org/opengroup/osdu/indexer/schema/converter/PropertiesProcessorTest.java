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

    @Test
    public void should_fail_on_bad_reference_definition() {
        PropertiesProcessor propertiesProcessor = new PropertiesProcessor(Mockito.mock(Definitions.class), new SchemaConverterPropertiesConfig());
        propertiesProcessor.processRef(DEFINITIONS_PREFIX + "unknownDefinition");
        assertEquals(1, propertiesProcessor.getErrors().size());
    }

    @Test
    public void should_fail_on_wrong_definition_format() {
        PropertiesProcessor propertiesProcessor = new PropertiesProcessor(Mockito.mock(Definitions.class), new SchemaConverterPropertiesConfig());
        propertiesProcessor.processRef("unknownDefinition");
        assertEquals(1, propertiesProcessor.getErrors().size());
    }

    @Test
    public void should_not_process_special_reference() {
        assertFalse(new PropertiesProcessor(null, new SchemaConverterPropertiesConfig())
                .processRef(DEFINITIONS_PREFIX + "a:b:anyCrsGeoJsonFeatureCollection:1.0.0").findAny().isPresent());
    }

    @Test
    public void should_return_special_type() {
        String res = new PropertiesProcessor(null, PATH, new SchemaConverterPropertiesConfig())
                .processRef(DEFINITIONS_PREFIX + "a:b:core_dl_geopoint:1.0.0").map(Object::toString).reduce("", String::concat);
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

        String defName = "a:b:defName:1.0.0";
        definitions.add(defName, definition);

        String res = new PropertiesProcessor(definitions, PATH, new SchemaConverterPropertiesConfig())
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

        String res = new PropertiesProcessor(Mockito.mock(Definitions.class), new SchemaConverterPropertiesConfig())
                .processItem(allOfItem).map(Object::toString).reduce("", String::concat);
        assertEquals("{path=" + PATH + ", kind=int}", res);
    }
}