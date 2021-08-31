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

import com.amazonaws.services.sqs.AmazonSQS;
import org.opengroup.osdu.core.aws.sqs.AmazonSQSConfig;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.google.gson.Gson;
import org.opengroup.osdu.core.aws.ssm.K8sLocalParameterProvider;
import org.opengroup.osdu.core.aws.ssm.K8sParameterNotFoundException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.indexer.util.IndexerQueueTaskBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
//
@Primary
@Component
public class IndexerQueueTaskBuilderAws extends IndexerQueueTaskBuilder {

    private static final int INITIAL_RETRY_DELAY_SECONDS = 5;
    private static final int MAX_RETRY_DELAY_SECONDS = 900; // 15 minutes (900 seconds) is the hard limit SQS sets of message delays

    private AmazonSQS sqsClient;

    private String storageQueue;
    private String dlq;
    private final String retryString = "retry";

    private Gson gson;

    @Value("${aws.region}")
    private String region;

    @Inject
    public void init() throws K8sParameterNotFoundException {
        AmazonSQSConfig config = new AmazonSQSConfig(region);
        sqsClient = config.AmazonSQS();
        gson =new Gson();
        K8sLocalParameterProvider provider = new K8sLocalParameterProvider();
        storageQueue = provider.getParameterAsString("storage-sqs-url");
        dlq =  provider.getParameterAsString("indexer-deadletter-queue-sqs-url");
    }

    @Override
    public void createWorkerTask(String payload, DpsHeaders headers) {
        this.createTask(payload, headers);
    }

    @Override
    public  void createWorkerTask(String payload, Long countDownMillis, DpsHeaders headers){
        this.createTask(payload, headers);
    }
    @Override
    public void createReIndexTask(String payload,DpsHeaders headers) {
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
        messageAttributes.put("ReIndexCursor", new MessageAttributeValue()
                .withDataType("String")
                .withStringValue("True"));
        SendMessageRequest sendMessageRequest = new SendMessageRequest()
                .withQueueUrl(storageQueue)
                .withMessageBody(payload)
                .withMessageAttributes(messageAttributes);
        sqsClient.sendMessage(sendMessageRequest);
    }
    @Override
    public void createReIndexTask(String payload, Long countDownMillis, DpsHeaders headers){
        this.createReIndexTask(payload, headers);
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
        int retryDelay;
        if (message.getAttributes().containsKey(retryString)) {
            retryCount = Integer.parseInt(message.getAttributes().get(retryString));
            retryCount++;
            retryDelay = Math.min(getWaitTimeExp(retryCount), MAX_RETRY_DELAY_SECONDS);
        } else {
            // This will be the first retry; initialize the retry counter and set the delay to the initial constant value
            retryCount = 1;
            retryDelay = INITIAL_RETRY_DELAY_SECONDS;
        }
        System.out.println("Re-queuing for retry attempt #: " + retryCount);
        System.out.println("Delay (in seconds) before next retry: " + retryDelay);

        // Append the retry count to the message attributes
        messageAttributes.put(retryString, new MessageAttributeValue()
                .withDataType("String")
                .withStringValue(String.valueOf(retryCount))
        );

        // Send a message with an attribute and a delay
        final SendMessageRequest sendMessageRequest ;
        if (retryCount< 10) {

            sendMessageRequest = new SendMessageRequest()
                    .withQueueUrl(storageQueue)
                    .withMessageBody(message.getData())
                    .withDelaySeconds(new Integer(retryDelay))
                    .withMessageAttributes(messageAttributes);
        }else{
            sendMessageRequest = new SendMessageRequest()
                    .withQueueUrl(dlq)
                    .withMessageBody(message.getData());
        }
        sqsClient.sendMessage(sendMessageRequest);
    }

    /*
     * Returns the next wait interval based on the current number of retries,
     * in seconds, using an exponential backoff algorithm.
     */
    public static int getWaitTimeExp(int retryCount) {
        if (0 == retryCount) {
            return 0;
        }

        return ((int) Math.pow(2, retryCount) * 4);
    }
}
