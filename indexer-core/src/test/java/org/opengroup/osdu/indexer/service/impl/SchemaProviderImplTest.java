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
// limitations under the License.package org.opengroup.osdu.indexer.service.impl;

package org.opengroup.osdu.indexer.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.opengroup.osdu.core.common.http.IUrlFetchService;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.HttpResponse;
import org.opengroup.osdu.core.common.provider.interfaces.IRequestInfo;
import org.opengroup.osdu.indexer.config.IndexerConfigurationProperties;
import org.opengroup.osdu.indexer.schema.converter.SchemaToStorageFormatImpl;
import org.opengroup.osdu.indexer.service.StorageService;
import org.powermock.api.mockito.PowerMockito;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(SpringRunner.class)
public class SchemaProviderImplTest {

    private ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
    private JaxRsDpsLog jaxRsDpsLog = Mockito.mock(JaxRsDpsLog.class);

    @Spy
    private SchemaToStorageFormatImpl schemaToStorageFormat = new SchemaToStorageFormatImpl(objectMapper, jaxRsDpsLog, null);

    @Mock
    private IUrlFetchService urlFetchService;

    @Mock
    private IRequestInfo requestInfo;

    @Mock
    private StorageService storageService;

    @Mock
    private IndexerConfigurationProperties configurationProperties;

    @InjectMocks
    private SchemaProviderImpl sut;

    @Test
    public void test_empty_schema() throws UnsupportedEncodingException, URISyntaxException {
        org.opengroup.osdu.core.common.model.http.HttpResponse httpResponse =
                mock(org.opengroup.osdu.core.common.model.http.HttpResponse.class);
        when(httpResponse.getResponseCode()).thenReturn(HttpStatus.SC_OK);
        when(httpResponse.getBody()).thenReturn("{}");

        when(urlFetchService.sendRequest(any())).thenReturn(httpResponse);
        String schema = sut.getSchema("fake");
        Assert.assertEquals("{\n" +
                "  \"kind\" : \"fake\",\n" +
                "  \"schema\" : [ ]\n" +
                "}", schema);

    }

    @Test
    public void should_returnValidResponse_givenValidSchema() throws Exception {

        String validSchemaFromSchemaService = "{\n" +
                "\"properties\": {" +
                "   \"data\":{\n" +
                "      \"allOf\":[\n" +
                "         {\n" +
                "            \"type\":\"object\",\n" +
                "            \"properties\":{\n" +
                "               \"WellID\":{\n" +
                "                  \"type\":\"string\",\n" +
                "                  \"pattern\":\"^srn:<namespace>:master-data\\\\/Well:[^:]+:[0-9]*$\"\n" +
                "               }\n" +
                "            }\n" +
                "         }\n" +
                "      ]\n" +
                "   }\n" +
                "   }\n" +
                "}";
        String kind = "tenant:test:test:1.0.0";

        HttpResponse httpResponse = new HttpResponse();
        httpResponse.setResponseCode(org.springframework.http.HttpStatus.OK.value());
        httpResponse.setBody(validSchemaFromSchemaService);

        PowerMockito.when(this.urlFetchService.sendRequest(any())).thenReturn(httpResponse);

        String recordSchemaResponse = this.sut.getSchema(kind);

        Map<String, Object> result = objectMapper.readValue(recordSchemaResponse,
                new TypeReference<Map<String,Object>>(){});
        assertEquals("Schema must have two root items", 2, result.size());
        assertEquals("Wrong kind", "tenant:test:test:1.0.0", result.get("kind"));
        assertEquals("Wrong schema attributes", "[{path=WellID, kind=link}]", result.get("schema").toString());

        assertNotNull(recordSchemaResponse);
    }

    @Test
    public void should_returnNullResponse_givenAbsentKind_getSchemaByKind() throws Exception {

        String kind = "tenant:test:test:1.0.0";

        HttpResponse httpResponse = new HttpResponse();
        httpResponse.setResponseCode(org.springframework.http.HttpStatus.NOT_FOUND.value());

        PowerMockito.when(this.urlFetchService.sendRequest(any())).thenReturn(httpResponse);

        String recordSchemaResponse = this.sut.getSchema(kind);

        assertNull(recordSchemaResponse);
    }

    @Test
    public void should_call_Storage_then_Schema() throws Exception {
        String kind = "tenant:test:test:1.0.0";

        SchemaProviderImpl schemaService = Mockito.mock(SchemaProviderImpl.class);
        PowerMockito.when(schemaService.getSchema(any())).thenCallRealMethod();

        InOrder inOrder = inOrder(schemaService);

        String recordSchemaResponse = schemaService.getSchema(kind);
        assertNull(recordSchemaResponse);

        inOrder.verify(schemaService).getSchema(any());
        inOrder.verify(schemaService).getFromStorageService(any());
        inOrder.verify(schemaService).getFromSchemaService(any());
        verify(schemaService, times(1)).getFromStorageService(any());
        verify(schemaService, times(1)).getFromSchemaService(any());
    }

    @Test
    public void should_call_only_Storage_if_it_returns_result() throws Exception {
        String kind = "tenant:test:test:1.0.0";

        SchemaProviderImpl schemaService = Mockito.mock(SchemaProviderImpl.class);
        when(schemaService.getSchema(any())).thenCallRealMethod();
        String someSchema = "some schema";
        when(schemaService.getFromStorageService(any())).thenReturn(someSchema);

        InOrder inOrder = inOrder(schemaService);

        String recordSchemaResponse = schemaService.getSchema(kind);
        assertEquals(recordSchemaResponse, someSchema);

        inOrder.verify(schemaService).getSchema(any());
        inOrder.verify(schemaService).getFromStorageService(any());
        verify(schemaService, times(0)).getFromSchemaService(any());
        verify(schemaService, times(1)).getFromStorageService(any());
    }

}