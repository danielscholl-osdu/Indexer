package org.opengroup.osdu.indexer.azure.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.indexer.JobStatus;
import org.opengroup.osdu.indexer.azure.publish.PublisherImpl;
import org.mockito.junit.MockitoJUnitRunner;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;


@RunWith(MockitoJUnitRunner.class)
public class PublisherImplTest {
    @InjectMocks
    private PublisherImpl publisher = new PublisherImpl();
    @Mock
    private JaxRsDpsLog logger;
    private DpsHeaders dpsHeaders;
    private JobStatus jobStatus;

    @Test
    public void indexer_should_not_publish_message_to_stale_service_bus_topic() throws Exception {
        // Arrange
        this.dpsHeaders = new DpsHeaders();
        this.jobStatus = new JobStatus();
        Map<String, String> messageAttributes = new HashMap<>();
        messageAttributes.put(DpsHeaders.ACCOUNT_ID,  dpsHeaders.getPartitionIdWithFallbackToAccountId());
        messageAttributes.put(DpsHeaders.DATA_PARTITION_ID,  dpsHeaders.getPartitionIdWithFallbackToAccountId());
        messageAttributes.put(DpsHeaders.CORRELATION_ID, dpsHeaders.getCorrelationId());

        // Act
        publisher.publishStatusChangedTagsToTopic(dpsHeaders, jobStatus);

        // Assert
        verify(this.logger,never()).debug("Indexer publishes message " + dpsHeaders.getCorrelationId());
    }
}
