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

import com.amazonaws.services.sns.model.MessageAttributeValue;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.AmazonSNS;

import org.opengroup.osdu.core.aws.ssm.K8sParameterNotFoundException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.aws.sns.AmazonSNSConfig;
import org.opengroup.osdu.core.aws.sns.PublishRequestBuilder;
import org.opengroup.osdu.indexer.provider.interfaces.IPublisher;
import org.opengroup.osdu.core.common.model.indexer.JobStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.opengroup.osdu.core.aws.ssm.K8sLocalParameterProvider;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@Component
public class PublisherImpl implements IPublisher {

    AmazonSNS snsClient;
    private String amazonSNSTopic;

    @Value("${aws.region}")
    private String amazonSNSRegion;

    @Inject
    public void init() throws K8sParameterNotFoundException {
        AmazonSNSConfig snsConfig = new AmazonSNSConfig(amazonSNSRegion);
        snsClient = snsConfig.AmazonSNS();
        K8sLocalParameterProvider provider = new K8sLocalParameterProvider();
        amazonSNSTopic = provider.getParameterAsString("indexer-sns-topic-arn");
    }

    public void publishStatusChangedTagsToTopic(DpsHeaders headers, JobStatus indexerBatchStatus) throws Exception
    {
        // Attributes
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

        PublishRequest publishRequest = new PublishRequestBuilder().generatePublishRequest("data", indexerBatchStatus.getStatusesList(), messageAttributes, amazonSNSTopic);

        snsClient.publish(publishRequest);
    }

}
