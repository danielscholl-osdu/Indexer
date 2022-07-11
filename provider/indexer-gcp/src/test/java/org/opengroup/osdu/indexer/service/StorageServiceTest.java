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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.indexer.IndexingStatus;
import org.opengroup.osdu.core.common.model.indexer.JobStatus;
import org.opengroup.osdu.core.common.model.indexer.RecordInfo;
import org.opengroup.osdu.core.common.model.http.HttpResponse;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.provider.interfaces.IRequestInfo;
import org.opengroup.osdu.core.common.http.IUrlFetchService;
import org.opengroup.osdu.core.common.model.indexer.RecordQueryResponse;
import org.opengroup.osdu.core.common.model.indexer.RecordReindexRequest;
import org.opengroup.osdu.core.common.model.indexer.Records;
import org.opengroup.osdu.indexer.config.IndexerConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;

import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(SpringRunner.class)
public class StorageServiceTest {

    @Mock
    private IUrlFetchService urlFetchService;
    @Mock
    private JobStatus jobStatus;
    @Mock
    private JaxRsDpsLog log;
    @Mock
    private IRequestInfo requestInfo;
    @Mock
    private IndexerConfigurationProperties configurationProperties;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks
    private StorageServiceImpl sut;

    private List<String> ids;
    private static final String RECORD_ID1 = "tenant1:doc:1dbf528e0e0549cab7a08f29fbfc8465";
    private static final String RECORDS_ID2 = "tenant1:doc:15e790a69beb4d789b1f979e2af2e813";

    @Before
    public void setup() {

        String recordChangedMessages = "[{\"id\":\"tenant1:doc:1dbf528e0e0549cab7a08f29fbfc8465\",\"kind\":\"tenant1:testindexer1528919679710:well:1.0.0\",\"op\":\"purge\"}," +
                "{\"id\":\"tenant1:doc:15e790a69beb4d789b1f979e2af2e813\",\"kind\":\"tenant1:testindexer1528919679710:well:1.0.0\",\"op\":\"create\"}]";

        when(this.requestInfo.getHeadersMap()).thenReturn(new HashMap<>());
        when(this.requestInfo.getHeaders()).thenReturn(new DpsHeaders());

        Type listType = new TypeToken<List<RecordInfo>>() {}.getType();

        List<RecordInfo> msgs = (new Gson()).fromJson(recordChangedMessages, listType);
        jobStatus.initialize(msgs);
        ids = Arrays.asList(RECORD_ID1, RECORDS_ID2);

        when(configurationProperties.getStorageRecordsBatchSize()).thenReturn(20);
    }

    @Test
    public void should_return404_givenNullData_getValidStorageRecordsTest() throws URISyntaxException {

        HttpResponse httpResponse = mock(HttpResponse.class);
        Mockito.when(httpResponse.getBody()).thenReturn(null);

        when(this.urlFetchService.sendRequest(ArgumentMatchers.any())).thenReturn(httpResponse);

        should_return404_getValidStorageRecordsTest();
    }

    @Test
    public void should_return404_givenEmptyData_getValidStorageRecordsTest() throws URISyntaxException {

        String emptyDataFromStorage = "{\"records\":[],\"notFound\":[]}";

        HttpResponse httpResponse = mock(HttpResponse.class);
        Mockito.when(httpResponse.getBody()).thenReturn(emptyDataFromStorage);

        when(this.urlFetchService.sendRequest(ArgumentMatchers.any())).thenReturn(httpResponse);

        should_return404_getValidStorageRecordsTest();
    }

    @Test
    public void should_returnOneValidRecords_givenValidData_getValidStorageRecordsTest() throws URISyntaxException {

        String validDataFromStorage = "{\"records\":[{\"id\":\"testid\", \"version\":1, \"kind\":\"tenant:test:test:1.0.0\"}],\"notFound\":[\"invalid1\"], \"conversionStatuses\": []}";

        HttpResponse httpResponse = mock(HttpResponse.class);
        Mockito.when(httpResponse.getBody()).thenReturn(validDataFromStorage);

        when(this.urlFetchService.sendRequest(ArgumentMatchers.any())).thenReturn(httpResponse);
        Records storageRecords = this.sut.getStorageRecords(ids);

        assertEquals(1, storageRecords.getRecords().size());
    }

    @Test
    public void should_logMissingRecord_given_storageMissedRecords() throws URISyntaxException {

        String validDataFromStorage = "{\"records\":[{\"id\":\"tenant1:doc:1dbf528e0e0549cab7a08f29fbfc8465\", \"version\":1, \"kind\":\"tenant:test:test:1.0.0\"}],\"notFound\":[]}";

        HttpResponse httpResponse = mock(HttpResponse.class);
        Mockito.when(httpResponse.getBody()).thenReturn(validDataFromStorage);

        when(this.urlFetchService.sendRequest(any())).thenReturn(httpResponse);
        Records storageRecords = this.sut.getStorageRecords(ids);

        assertEquals(1, storageRecords.getRecords().size());
        verify(this.jobStatus).addOrUpdateRecordStatus(singletonList(RECORDS_ID2), IndexingStatus.FAIL, HttpStatus.NOT_FOUND.value(), "Partial response received from Storage service - missing records", "Partial response received from Storage service: tenant1:doc:15e790a69beb4d789b1f979e2af2e813");
    }

    @Test
    public void should_returnValidJobStatus_givenFailedUnitsConversion_processRecordChangedMessageTest() throws URISyntaxException {
        String validDataFromStorage = "{\"records\":[{\"id\":\"tenant1:doc:15e790a69beb4d789b1f979e2af2e813\", \"version\":1, \"kind\":\"tenant:test:test:1.0.0\"}],\"notFound\":[],\"conversionStatuses\":[{\"id\":\"tenant1:doc:15e790a69beb4d789b1f979e2af2e813\",\"status\":\"ERROR\",\"errors\":[\"crs conversion failed\"]}]}";

        HttpResponse httpResponse = mock(HttpResponse.class);
        Mockito.when(httpResponse.getBody()).thenReturn(validDataFromStorage);

        when(this.urlFetchService.sendRequest(any())).thenReturn(httpResponse);
        Records storageRecords = this.sut.getStorageRecords(singletonList(RECORDS_ID2));

        assertEquals(1, storageRecords.getRecords().size());
        verify(this.jobStatus).addOrUpdateRecordStatus(RECORDS_ID2, IndexingStatus.WARN, HttpStatus.BAD_REQUEST.value(), "crs conversion failed", String.format("record-id: %s | %s", "tenant1:doc:15e790a69beb4d789b1f979e2af2e813", "crs conversion failed"));
    }

    @Test
    public void should_returnValidResponse_givenValidRecordQueryRequest_getRecordListByKind() throws Exception {

        RecordReindexRequest recordReindexRequest = RecordReindexRequest.builder().kind("tenant:test:test:1.0.0").cursor("100").build();

        HttpResponse httpResponse = new HttpResponse();
        httpResponse.setBody(new Gson().toJson(recordReindexRequest, RecordReindexRequest.class));

        when(this.urlFetchService.sendRequest(ArgumentMatchers.any())).thenReturn(httpResponse);

        RecordQueryResponse recordQueryResponse = this.sut.getRecordsByKind(recordReindexRequest);

        assertEquals("100", recordQueryResponse.getCursor());
        assertNull(recordQueryResponse.getResults());
    }

    @Test
    public void should_returnValidResponse_givenValidKind_getSchemaByKind() throws Exception {

        String validSchemaFromStorage = "{" +
                "  \"kind\": \"tenant:test:test:1.0.0\"," +
                "  \"schema\": [" +
                "    {" +
                "      \"path\": \"msg\"," +
                "      \"kind\": \"string\"" +
                "    }," +
                "    {" +
                "      \"path\": \"references.entity\"," +
                "      \"kind\": \"string\"" +
                "    }" +
                "  ]," +
                "  \"ext\": null" +
                "}";
        String kind = "tenant:test:test:1.0.0";

        HttpResponse httpResponse = new HttpResponse();
        httpResponse.setResponseCode(HttpStatus.OK.value());
        httpResponse.setBody(validSchemaFromStorage);

        when(this.urlFetchService.sendRequest(ArgumentMatchers.any())).thenReturn(httpResponse);

        String recordSchemaResponse = this.sut.getStorageSchema(kind);

        assertNotNull(recordSchemaResponse);
    }

    @Test
    public void should_returnNullResponse_givenAbsentKind_getSchemaByKind() throws Exception {

        String kind = "tenant:test:test:1.0.0";

        HttpResponse httpResponse = new HttpResponse();
        httpResponse.setResponseCode(HttpStatus.NOT_FOUND.value());

        when(this.urlFetchService.sendRequest(ArgumentMatchers.any())).thenReturn(httpResponse);

        String recordSchemaResponse = this.sut.getStorageSchema(kind);

        assertNull(recordSchemaResponse);
    }

    @Test
    public void should_returnOneValidRecords_givenValidData_getValidStorageRecordsWithInvalidConversionTest() throws URISyntaxException {

        String validDataFromStorage = "{\"records\":[{\"id\":\"testid\", \"version\":1, \"kind\":\"tenant:test:test:1.0.0\"}],\"notFound\":[\"invalid1\"],\"conversionStatuses\": [{\"id\":\"testid\",\"status\":\"ERROR\",\"errors\":[\"conversion error occurred\"] } ]}";

        HttpResponse httpResponse = mock(HttpResponse.class);
        Mockito.when(httpResponse.getBody()).thenReturn(validDataFromStorage);

        when(this.urlFetchService.sendRequest(ArgumentMatchers.any())).thenReturn(httpResponse);
        Records storageRecords = this.sut.getStorageRecords(ids);

        assertEquals(1, storageRecords.getRecords().size());

        assertEquals(1, storageRecords.getConversionStatuses().get(0).getErrors().size());

        assertEquals("conversion error occurred", storageRecords.getConversionStatuses().get(0).getErrors().get(0));
    }

    private void should_return404_getValidStorageRecordsTest() {
        try {
            this.sut.getStorageRecords(ids);
            fail("Should throw exception");
        } catch (AppException e) {
            assertEquals(HttpStatus.NOT_FOUND.value(), e.getError().getCode());
        } catch (Exception e) {
            fail("Should not throw this exception" + e.getMessage());
        }
    }
}
