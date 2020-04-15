// Copyright © Amazon Web Services
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

package org.opengroup.osdu.indexer.aws.publish;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.MessageAttributeValue;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.aws.sns.PublishRequestBuilder;
import org.opengroup.osdu.indexer.aws.IndexerAwsApplication;
import org.opengroup.osdu.core.common.model.indexer.JobStatus;
import org.springframework.boot.test.context.SpringBootTest;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
@SpringBootTest(classes = {IndexerAwsApplication.class})
public class PublisherImplTest {

    @InjectMocks
    private PublisherImpl publisher = new PublisherImpl();

    @Mock
    AmazonSNS snsClient;

    public void setUp() {
        snsClient = mock(AmazonSNS.class);
    }

    @Test
    public void publishStatusChangedTagsToTopic() throws Exception {
        // Arrange
        DpsHeaders headers = new DpsHeaders();
        JobStatus jobStatus = new JobStatus();
        Mockito.when(snsClient.publish(Mockito.any(PublishRequest.class)))
                .thenReturn(Mockito.any(PublishResult.class));

        Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
        messageAttributes.put(DpsHeaders.ACCOUNT_ID, new MessageAttributeValue()
                .withDataType("String")
                .withStringValue(headers.getPartitionIdWithFallbackToAccountId()));
        messageAttributes.put(DpsHeaders.DATA_PARTITION_ID, new MessageAttributeValue()
                .withDataType("String")
                .withStringValue(headers.getPartitionIdWithFallbackToAccountId()));
        headers.addCorrelationIdIfMissing();
        messageAttributes.put(DpsHeaders.CORRELATION_ID, new MessageAttributeValue()
                .withDataType("String")
                .withStringValue(headers.getCorrelationId()));

        PublishRequest publishRequest = new PublishRequestBuilder().generatePublishRequest("data", jobStatus.getStatusesList(), messageAttributes,null);
        // Act
        publisher.publishStatusChangedTagsToTopic(headers, jobStatus);

        // Assert
        Mockito.verify(snsClient, Mockito.times(1)).publish(Mockito.eq(publishRequest));
    }
}
