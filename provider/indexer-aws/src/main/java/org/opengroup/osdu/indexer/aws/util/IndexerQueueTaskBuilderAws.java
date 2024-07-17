/**
* Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
* 
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*      http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.opengroup.osdu.indexer.aws.util;

import com.amazonaws.services.sqs.AmazonSQS;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.aws.sqs.AmazonSQSConfig;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.google.gson.Gson;
import org.opengroup.osdu.core.aws.ssm.K8sLocalParameterProvider;
import org.opengroup.osdu.core.aws.ssm.K8sParameterNotFoundException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.indexer.model.Constants;
import org.opengroup.osdu.indexer.model.XcollaborationHolder;
import org.opengroup.osdu.indexer.util.IndexerQueueTaskBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.Map;
//
@Slf4j
@Primary
@Component
public class IndexerQueueTaskBuilderAws extends IndexerQueueTaskBuilder {

    private static final String TYPE_STRING = "String";
    private static final String RETRY_STRING = "retry";
    private static final int INITIAL_RETRY_DELAY_SECONDS = 5;
    private static final int MAX_RETRY_DELAY_SECONDS = 900; // 15 minutes (900 seconds) is the hard limit SQS sets of message delays
    private static final Logger LOGGER = LoggerFactory.getLogger(IndexerQueueTaskBuilderAws.class);

    private AmazonSQS sqsClient;

    private String storageQueue;
    private String dlq;

    private Gson gson;

    @Value("${aws.region}")
    private String region;

    @Inject
    private XcollaborationHolder xCollaborationHolder;

    @Inject
    public void init() throws K8sParameterNotFoundException {
        AmazonSQSConfig config = new AmazonSQSConfig(region);
        sqsClient = config.AmazonSQS();
        gson =new Gson();
        K8sLocalParameterProvider provider = new K8sLocalParameterProvider();
        storageQueue = provider.getParameterAsString("STORAGE_SQS_URL");
        dlq =  provider.getParameterAsString("INDEXER_DEADLETTER_QUEUE_SQS_URL");
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
                .withDataType(TYPE_STRING)
                .withStringValue(headers.getPartitionIdWithFallbackToAccountId()));
        messageAttributes.put(DpsHeaders.DATA_PARTITION_ID, new MessageAttributeValue()
                .withDataType(TYPE_STRING)
                .withStringValue(headers.getPartitionIdWithFallbackToAccountId()));
        headers.addCorrelationIdIfMissing();
        messageAttributes.put(DpsHeaders.CORRELATION_ID, new MessageAttributeValue()
                .withDataType(TYPE_STRING)
                .withStringValue(headers.getCorrelationId()));
        messageAttributes.put(DpsHeaders.USER_EMAIL, new MessageAttributeValue()
                .withDataType(TYPE_STRING)
                .withStringValue(headers.getUserEmail()));
        messageAttributes.put(DpsHeaders.AUTHORIZATION, new MessageAttributeValue()
                .withDataType(TYPE_STRING)
                .withStringValue(headers.getAuthorization()));
        messageAttributes.put("ReIndexCursor", new MessageAttributeValue()
                .withDataType(TYPE_STRING)
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
                .withDataType(TYPE_STRING)
                .withStringValue(headers.getPartitionIdWithFallbackToAccountId()));
        messageAttributes.put(DpsHeaders.DATA_PARTITION_ID, new MessageAttributeValue()
                .withDataType(TYPE_STRING)
                .withStringValue(headers.getPartitionIdWithFallbackToAccountId()));
        headers.addCorrelationIdIfMissing();
        messageAttributes.put(DpsHeaders.CORRELATION_ID, new MessageAttributeValue()
                .withDataType(TYPE_STRING)
                .withStringValue(headers.getCorrelationId()));
        messageAttributes.put(DpsHeaders.USER_EMAIL, new MessageAttributeValue()
                .withDataType(TYPE_STRING)
                .withStringValue(headers.getUserEmail()));
        messageAttributes.put(DpsHeaders.AUTHORIZATION, new MessageAttributeValue()
                .withDataType(TYPE_STRING)
                .withStringValue(headers.getAuthorization()));

        log.debug("Is isFeatureEnabledAndHeaderExists: {}", xCollaborationHolder.isFeatureEnabledAndHeaderExists());
        String xCollabValue = headers.getHeaders().get(DpsHeaders.COLLABORATION);
        if (xCollabValue != null) {
            xCollaborationHolder.setxCollaborationHeader(xCollabValue);
            if (xCollaborationHolder.isFeatureEnabledAndHeaderExists()) {
                messageAttributes.put(XcollaborationHolder.X_COLLABORATION, new MessageAttributeValue()
                        .withDataType(TYPE_STRING)
                        .withStringValue(xCollaborationHolder.getCollaborationContext().orElseThrow().getId()));
            }
        }

        RecordChangedMessages message = gson.fromJson(payload, RecordChangedMessages.class);

        int retryCount;
        int delay;
        if (message.getAttributes().containsKey(RETRY_STRING)) {
            retryCount = Integer.parseInt(message.getAttributes().get(RETRY_STRING));
            retryCount++;
            delay = Math.min(getWaitTimeExp(retryCount), MAX_RETRY_DELAY_SECONDS);
        } else {
            // This will be the first retry; initialize the retry counter and set the delay to the initial constant value
            retryCount = 1;
            delay = INITIAL_RETRY_DELAY_SECONDS;
        }

        LOGGER.info("Re-queuing for retry attempt #: {}", retryCount);
        LOGGER.info("Delay (in seconds) before next retry: {}", delay);

        // Append the retry count to the message attributes
        messageAttributes.put(RETRY_STRING, new MessageAttributeValue()
                .withDataType(TYPE_STRING)
                .withStringValue(String.valueOf(retryCount))
        );
        // Append the ancestry kinds used to prevent circular chasing
        if(message.getAttributes().containsKey(Constants.ANCESTRY_KINDS)) {
            delay = Math.max(delay, Constants.CHASING_MESSAGE_DELAY_SECONDS);
            messageAttributes.put(Constants.ANCESTRY_KINDS, new MessageAttributeValue()
                    .withDataType(TYPE_STRING)
                    .withStringValue(message.getAttributes().get(Constants.ANCESTRY_KINDS)));
        }

        // Send a message with an attribute and a delay
        final SendMessageRequest sendMessageRequest ;
        if (retryCount< 10) {

            sendMessageRequest = new SendMessageRequest()
                    .withQueueUrl(storageQueue)
                    .withMessageBody(message.getData())
                    .withDelaySeconds(Integer.valueOf(delay))
                    .withMessageAttributes(messageAttributes);
        }else{
            sendMessageRequest = new SendMessageRequest()
                    .withQueueUrl(dlq)
                    .withMessageBody(message.getData());
        }
        log.debug("sendMessageRequest x-collab: {}", sendMessageRequest);
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
