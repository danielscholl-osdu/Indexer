package org.opengroup.osdu.indexer.azure.util;

import com.microsoft.azure.servicebus.TopicClient;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.Test;
import org.mockito.Spy;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.azure.servicebus.ITopicClientFactory;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.indexer.RecordQueryResponse;
import org.opengroup.osdu.core.common.model.indexer.RecordReindexRequest;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.indexer.config.IndexerConfigurationProperties;
import org.opengroup.osdu.indexer.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import javax.inject.Inject;
import javax.inject.Named;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.opengroup.osdu.core.common.model.http.DpsHeaders.AUTHORIZATION;

@RunWith(MockitoJUnitRunner.class)
public class IndexerQueueTaskBuilderAzureTest {

    private String payload="{payload : value }";
    private static String partitionId = "opendes";
    private static String correlationId = "correlationId";
    private static String serviceBusReindexTopicNameField = "serviceBusReindexTopicName";
    private static String serviceBusReindexTopicNameValue = "recordChangeTopic";
    private static String authorisedHeader = "Bearer opendes";

    @Spy
    private ITopicClientFactory topicClientFactory;

    @Mock
    private IndexerConfigurationProperties configurationProperties;

    @Mock
    private JaxRsDpsLog logger;

    @Mock
    DpsHeaders dpsHeaders;

    @Mock
    RequestInfoImpl requestInfo;

    @Mock
    StorageService storageService;

    @InjectMocks
    IndexerQueueTaskBuilderAzure sut;

    @Test
    public void createWorkerTask_should_invoke_correctMethods() throws ServiceBusException, InterruptedException, NoSuchFieldException {
        when(dpsHeaders.getPartitionIdWithFallbackToAccountId()).thenReturn(partitionId);
        when(dpsHeaders.getPartitionId()).thenReturn(partitionId);
        when(dpsHeaders.getCorrelationId()).thenReturn(correlationId);
        ReflectionTestUtils.setField(sut, serviceBusReindexTopicNameField, serviceBusReindexTopicNameValue);

        sut.createWorkerTask(payload, dpsHeaders);

        verify(dpsHeaders, times(4)).getPartitionIdWithFallbackToAccountId();
        verify(dpsHeaders, times(2)).getCorrelationId();
        verify(dpsHeaders, times(1)).addCorrelationIdIfMissing();
        verify(topicClientFactory, times(1)).getClient(partitionId, serviceBusReindexTopicNameValue);
    }

    @Test
    public void createWorkerTaskWithCountDown_should_invoke_correctMethods() throws ServiceBusException, InterruptedException, NoSuchFieldException {
        Long milliseconds=8000L;
        when(dpsHeaders.getPartitionIdWithFallbackToAccountId()).thenReturn(partitionId);
        when(dpsHeaders.getPartitionId()).thenReturn(partitionId);
        when(dpsHeaders.getCorrelationId()).thenReturn(correlationId);
        ReflectionTestUtils.setField(sut, serviceBusReindexTopicNameField, serviceBusReindexTopicNameValue);

        sut.createWorkerTask(payload, milliseconds, dpsHeaders);

        verify(dpsHeaders, times(2)).addCorrelationIdIfMissing();
        verify(dpsHeaders, times(4)).getPartitionIdWithFallbackToAccountId();
        verify(dpsHeaders, times(2)).getCorrelationId();
        verify(topicClientFactory, times(1)).getClient(partitionId, serviceBusReindexTopicNameValue);
    }

    @Test(expected = AppException.class)
    public void createReIndexTask_InvalidParameter_ShouldThrowException()
    {
        sut.createReIndexTask(payload,dpsHeaders);
    }

    @Test
    public void createReIndexTaskWithEmptyStorageResponse_should_invoke_correctMethods() throws ServiceBusException, InterruptedException, NoSuchFieldException, URISyntaxException {
        Long milliseconds = 8000L;
        RecordQueryResponse recordQueryResponse = new RecordQueryResponse();
        when(requestInfo.checkOrGetAuthorizationHeader()).thenReturn(authorisedHeader);
        when(storageService.getRecordsByKind(any())).thenReturn(recordQueryResponse);
        ReflectionTestUtils.setField(sut, serviceBusReindexTopicNameField, serviceBusReindexTopicNameValue);

        sut.createReIndexTask(payload, milliseconds, dpsHeaders);

        verify(requestInfo,times(1)).checkOrGetAuthorizationHeader();
        verify(dpsHeaders, times(1)).put(AUTHORIZATION, authorisedHeader);
        verify(storageService,times(1)).getRecordsByKind(any());
        verify(dpsHeaders, times(1)).addCorrelationIdIfMissing();
    }

    @Test
    public void createReIndexTaskWithNonEmptyStorageResponse_should_invoke_correctMethods() throws ServiceBusException, InterruptedException, NoSuchFieldException, URISyntaxException {
        Long milliseconds = 8000L;
        RecordQueryResponse recordQueryResponse = new RecordQueryResponse();
        List<String> res = Arrays.asList("r1","r2","r3");
        recordQueryResponse.setResults(res);
        when(dpsHeaders.getPartitionIdWithFallbackToAccountId()).thenReturn(partitionId);
        when(dpsHeaders.getCorrelationId()).thenReturn(correlationId);
        when(dpsHeaders.getPartitionId()).thenReturn(partitionId);
        when(requestInfo.checkOrGetAuthorizationHeader()).thenReturn(authorisedHeader);
        when(storageService.getRecordsByKind(any())).thenReturn(recordQueryResponse);
        ReflectionTestUtils.setField(sut, serviceBusReindexTopicNameField, serviceBusReindexTopicNameValue);

        sut.createReIndexTask(payload, milliseconds, dpsHeaders);

        verify(requestInfo,times(1)).checkOrGetAuthorizationHeader();
        verify(dpsHeaders, times(1)).put(AUTHORIZATION, authorisedHeader);
        verify(storageService,times(1)).getRecordsByKind(any());
        verify(dpsHeaders, times(6)).getPartitionIdWithFallbackToAccountId();
        verify(dpsHeaders, times(3)).getCorrelationId();
        verify(dpsHeaders, times(2)).addCorrelationIdIfMissing();
        verify(topicClientFactory, times(1)).getClient(partitionId, serviceBusReindexTopicNameValue);
    }

    @Test
    public void createReIndexTaskWithCountdown_should_invoke_correctMethods() throws ServiceBusException, InterruptedException, NoSuchFieldException, URISyntaxException {
        Long milliseconds = 8000L;
        RecordQueryResponse recordQueryResponse = new RecordQueryResponse();
        when(requestInfo.checkOrGetAuthorizationHeader()).thenReturn(authorisedHeader);
        when(storageService.getRecordsByKind(any())).thenReturn(recordQueryResponse);
        ReflectionTestUtils.setField(sut, serviceBusReindexTopicNameField, serviceBusReindexTopicNameValue);

        sut.createReIndexTask(payload, milliseconds, dpsHeaders);

        verify(requestInfo,times(1)).checkOrGetAuthorizationHeader();
        verify(dpsHeaders, times(1)).put(AUTHORIZATION, authorisedHeader);
        verify(storageService,times(1)).getRecordsByKind(any());
        verify(dpsHeaders, times(1)).addCorrelationIdIfMissing();
    }
}
