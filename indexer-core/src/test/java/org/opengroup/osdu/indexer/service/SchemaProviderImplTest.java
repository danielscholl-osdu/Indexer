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

package org.opengroup.osdu.indexer.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.rest.RestStatus;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.opengroup.osdu.core.common.http.IUrlFetchService;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.HttpResponse;
import org.opengroup.osdu.core.common.model.indexer.SchemaInfo;
import org.opengroup.osdu.core.common.provider.interfaces.IRequestInfo;
import org.opengroup.osdu.indexer.config.IndexerConfigurationProperties;
import org.opengroup.osdu.indexer.logging.AuditLogger;
import org.opengroup.osdu.indexer.schema.converter.SchemaToStorageFormatImpl;
import org.opengroup.osdu.indexer.util.ElasticClientHandler;
import org.powermock.api.mockito.PowerMockito;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

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
    @Mock
    private ElasticClientHandler elasticClientHandler;
    @Mock
    private AuditLogger auditLogger;
    @Mock
    private IndexSchemaService indexSchemaService;

    @InjectMocks
    private SchemaProviderImpl sut;

    private RestHighLevelClient restClient;

    @Test
    public void test_empty_schema() throws UnsupportedEncodingException, URISyntaxException {
        org.opengroup.osdu.core.common.model.http.HttpResponse httpResponse =
                mock(org.opengroup.osdu.core.common.model.http.HttpResponse.class);
        when(httpResponse.getResponseCode()).thenReturn(HttpStatus.SC_OK);
        when(httpResponse.getBody()).thenReturn("{ \"properties\" : { \"data\": {} } }");

        when(urlFetchService.sendRequest(any())).thenReturn(httpResponse);
        String schema = sut.getSchema("fake");
        Assert.assertEquals("{\n" +
                "  \"kind\" : \"fake\",\n" +
                "  \"schema\" : [ ]\n" +
                "}", schema.replaceAll("\r", ""));

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
                new TypeReference<Map<String, Object>>() {
                });
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
    public void should_call_Schema_then_Storage() throws Exception {
        String kind = "tenant:test:test:1.0.0";

        SchemaProviderImpl schemaService = Mockito.mock(SchemaProviderImpl.class);
        when(schemaService.getSchema(any())).thenCallRealMethod();

        InOrder inOrder = inOrder(schemaService);

        String recordSchemaResponse = schemaService.getSchema(kind);
        assertNull(recordSchemaResponse);

        inOrder.verify(schemaService).getSchema(any());
        inOrder.verify(schemaService).getFromSchemaService(any());
        inOrder.verify(schemaService).getFromStorageService(any());
        verify(schemaService, times(1)).getFromStorageService(any());
        verify(schemaService, times(1)).getFromSchemaService(any());
    }

    @Test
    public void should_call_only_SchemaService_if_it_returns_result() throws Exception {
        String kind = "tenant:test:test:1.0.0";

        SchemaProviderImpl schemaService = Mockito.mock(SchemaProviderImpl.class);
        when(schemaService.getSchema(any())).thenCallRealMethod();
        String someSchema = "some schema";
        when(schemaService.getFromSchemaService(any())).thenReturn(someSchema);

        InOrder inOrder = inOrder(schemaService);

        String recordSchemaResponse = schemaService.getSchema(kind);
        assertEquals(recordSchemaResponse, someSchema);

        inOrder.verify(schemaService).getSchema(any());
        inOrder.verify(schemaService).getFromSchemaService(any());
        verify(schemaService, times(1)).getFromSchemaService(any());
        verify(schemaService, times(0)).getFromStorageService(any());
    }

    @Test
    public void should_process_validSchemaCreateEvent() throws IOException, URISyntaxException {
        SchemaInfo event1 = new SchemaInfo("slb:indexer:test-data--SchemaEventIntegration:1.0.0", "create");
        this.restClient = PowerMockito.mock(RestHighLevelClient.class);
        when(elasticClientHandler.createRestClient()).thenReturn(restClient);

        this.sut.processSchemaMessages(singletonList(event1));

        verify(this.indexSchemaService, times(1)).processSchemaUpsertEvent(this.restClient, event1.getKind());
        verify(this.auditLogger, times(1)).indexMappingUpsertSuccess(singletonList(event1.getKind()));
    }

    @Test
    public void should_process_validSchemaUpdateEvent() throws IOException, URISyntaxException {
        SchemaInfo event1 = new SchemaInfo("slb:indexer:test-data--SchemaEventIntegration:1.0.0", "update");
        this.restClient = PowerMockito.mock(RestHighLevelClient.class);
        when(elasticClientHandler.createRestClient()).thenReturn(restClient);

        this.sut.processSchemaMessages(singletonList(event1));

        verify(this.indexSchemaService, times(1)).processSchemaUpsertEvent(this.restClient, event1.getKind());
        verify(this.auditLogger, times(1)).indexMappingUpsertSuccess(singletonList(event1.getKind()));
    }

    @Test
    public void should_throwError_given_unsupportedEvent() {
        SchemaInfo event1 = new SchemaInfo("slb:indexer:test-data--SchemaEventIntegration:1.0.0", "delete");

        try {
            this.sut.processSchemaMessages(singletonList(event1));
            fail("Should throw exception");
        } catch (AppException e) {
            assertEquals(BAD_REQUEST.value(), e.getError().getCode());
            assertEquals("Error parsing schema events in request payload.", e.getError().getMessage());
        } catch (Exception e) {
            fail("Should not throw this exception" + e.getMessage());
        }
    }

    @Test
    public void should_throwError_given_schemaUpsertFails() throws IOException, URISyntaxException {
        SchemaInfo event1 = new SchemaInfo("slb:indexer:test-data--SchemaEventIntegration:1.0.0", "update");
        this.restClient = PowerMockito.mock(RestHighLevelClient.class);
        when(elasticClientHandler.createRestClient()).thenReturn(restClient);
        doThrow(new ElasticsearchStatusException("unknown error", RestStatus.INTERNAL_SERVER_ERROR)).when(this.indexSchemaService).processSchemaUpsertEvent(any(RestHighLevelClient.class), anyString());

        try {
            this.sut.processSchemaMessages(singletonList(event1));
            fail("Should throw exception");
        } catch (AppException e) {
            assertEquals(INTERNAL_SERVER_ERROR.value(), e.getError().getCode());
            assertEquals("unknown error", e.getError().getMessage());
            verify(this.auditLogger, times(1)).indexMappingUpsertFail(singletonList(event1.getKind()));
        } catch (Exception e) {
            fail("Should not throw this exception" + e.getMessage());
        }
    }
}