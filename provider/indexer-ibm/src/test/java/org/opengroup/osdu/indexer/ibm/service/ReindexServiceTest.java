/* Licensed Materials - Property of IBM              */		
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/
package org.opengroup.osdu.indexer.ibm.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.indexer.RecordQueryResponse;
import org.opengroup.osdu.core.common.model.indexer.RecordReindexRequest;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.indexer.config.IndexerConfigurationProperties;
import org.opengroup.osdu.indexer.service.ReindexServiceImpl;
import org.opengroup.osdu.indexer.service.StorageService;
import org.opengroup.osdu.indexer.util.IndexerQueueTaskBuilder;
import org.opengroup.osdu.core.common.provider.interfaces.IRequestInfo;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;

import static org.junit.Assert.fail;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@Ignore
@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(SpringRunner.class)
@PrepareForTest({IndexerConfigurationProperties.class})
public class ReindexServiceTest {

    private final String cursor = "100";

    private final String correlationId = UUID.randomUUID().toString();

    @Mock
    private IndexerConfigurationProperties indexerConfigurationProperties;

    @Mock
    private StorageService storageService;

    @Mock
    private Map<String, String> httpHeaders;
    @Mock
    private IRequestInfo requestInfo;
    @Mock
    private IndexerQueueTaskBuilder indexerQueueTaskBuilder;
    @Mock
    private JaxRsDpsLog log;
    @InjectMocks
    private ReindexServiceImpl sut;

    private RecordReindexRequest recordReindexRequest;
    private RecordQueryResponse recordQueryResponse;

    @Before
    public void setup() {
        initMocks(this);

        mockStatic(UUID.class);

        recordReindexRequest = RecordReindexRequest.builder().kind("tenant:test:test:1.0.0").cursor(cursor).build();
        recordQueryResponse = new RecordQueryResponse();

        httpHeaders = new HashMap<>();
        httpHeaders.put(DpsHeaders.AUTHORIZATION, "testAuth");
        httpHeaders.put(DpsHeaders.CORRELATION_ID, correlationId);
        DpsHeaders standardHeaders = DpsHeaders.createFromMap(httpHeaders);
        when(requestInfo.getHeaders()).thenReturn(standardHeaders);
        when(requestInfo.getHeadersMapWithDwdAuthZ()).thenReturn(httpHeaders);
    }

    @Test
    public void should_returnNull_givenNullResponseResult_reIndexRecordsTest() {
        try {
            recordQueryResponse.setResults(null);
            when(storageService.getRecordsByKind(ArgumentMatchers.any())).thenReturn(recordQueryResponse);

            String response = sut.reindexRecords(recordReindexRequest, false);

            Assert.assertNull(response);
        } catch (Exception e) {
            fail("Should not throw this exception" + e.getMessage());
        }
    }

    @Test
    public void should_returnNull_givenEmptyResponseResult_reIndexRecordsTest() {
        try {
            recordQueryResponse.setResults(new ArrayList<>());
            when(storageService.getRecordsByKind(ArgumentMatchers.any())).thenReturn(recordQueryResponse);

            String response = sut.reindexRecords(recordReindexRequest, false);

            Assert.assertNull(response);
        } catch (Exception e) {
            fail("Should not throw this exception" + e.getMessage());
        }
    }

    @Test
    public void should_returnRecordQueryRequestPayload_givenValidResponseResult_reIndexRecordsTest() {
        try {
            recordQueryResponse.setCursor(cursor);
            List<String> results = new ArrayList<>();
            results.add("test1");
            recordQueryResponse.setResults(results);

            when(indexerConfigurationProperties.getStorageRecordsBatchSize()).thenReturn(1);

            when(storageService.getRecordsByKind(ArgumentMatchers.any())).thenReturn(recordQueryResponse);

            String taskQueuePayload = sut.reindexRecords(recordReindexRequest, false);

            Assert.assertEquals("{\"kind\":\"tenant:test:test:1.0.0\",\"cursor\":\"100\"}", taskQueuePayload);
        } catch (Exception e) {
            fail("Should not throw exception" + e.getMessage());
        }
    }

    @Test
    public void should_returnRecordChangedMessage_givenValidResponseResult_reIndexRecordsTest() {
        try {
            List<String> results = new ArrayList<>();
            results.add("test1");
            recordQueryResponse.setResults(results);
            when(storageService.getRecordsByKind(ArgumentMatchers.any())).thenReturn(recordQueryResponse);

            String taskQueuePayload = sut.reindexRecords(recordReindexRequest, false);

            Assert.assertEquals(String.format("{\"data\":\"[{\\\"id\\\":\\\"test1\\\",\\\"kind\\\":\\\"tenant:test:test:1.0.0\\\",\\\"op\\\":\\\"create\\\"}]\",\"attributes\":{\"slb-correlation-id\":\"%s\"}}", correlationId), taskQueuePayload);
        } catch (Exception e) {
            fail("Should not throw exception" + e.getMessage());
        }
    }
}
