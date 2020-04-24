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

package org.opengroup.osdu.indexer.aws.util;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.MessageAttributeValue;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.google.gson.Gson;
import org.opengroup.osdu.core.aws.sns.AmazonSNSConfig;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.aws.sqs.AmazonSQSConfig;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.indexer.util.IndexerQueueTaskBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@Primary
@Component
public class IndexerQueueTaskBuilderAws extends IndexerQueueTaskBuilder {

    private AmazonSNS snsClient;

    @Value("${aws.region}")
    private String region;

    @Value("${aws.sns.storage.arn}")
    private String amazonSNSTopic;

    private String retryString = "retry";

    private Gson gson;

    @Inject
    public void init() {
        AmazonSNSConfig config = new AmazonSNSConfig(region);
        snsClient = config.AmazonSNS();
        gson =new Gson();
    }

    @Override
    public void createWorkerTask(String payload, DpsHeaders headers) {
        createTask(payload, headers);
    }

    @Override
    public void createReIndexTask(String payload,DpsHeaders headers) {
        createTask(payload, headers);
    }

    private void createTask(String payload, DpsHeaders headers) {

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
        messageAttributes.put(DpsHeaders.USER_EMAIL, new MessageAttributeValue()
                .withDataType("String")
                .withStringValue(headers.getUserEmail()));
        messageAttributes.put(DpsHeaders.AUTHORIZATION, new MessageAttributeValue()
                .withDataType("String")
                .withStringValue(headers.getAuthorization()));

        RecordChangedMessages message = gson.fromJson(payload, RecordChangedMessages.class);
        int retryCount;
        if(message.getAttributes().containsKey(retryString)){
            retryCount = Integer.parseInt(message.getAttributes().get(retryString));
            retryCount++;
        } else {
            retryCount = 1;
        }
        messageAttributes.put(retryString, new MessageAttributeValue()
                .withDataType("String")
                .withStringValue(String.valueOf(retryCount)));

        PublishRequest publishRequest = new PublishRequest(amazonSNSTopic, message.getData())
                .withMessageAttributes(messageAttributes);

        snsClient.publish(publishRequest);
    }
}
