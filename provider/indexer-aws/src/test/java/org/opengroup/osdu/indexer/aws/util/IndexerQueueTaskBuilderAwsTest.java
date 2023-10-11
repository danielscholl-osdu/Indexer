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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.aws.sqs.AmazonSQSConfig;
import org.opengroup.osdu.core.aws.ssm.K8sLocalParameterProvider;
import org.opengroup.osdu.core.aws.ssm.K8sParameterNotFoundException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.indexer.model.Constants;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageRequest;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IndexerQueueTaskBuilderAwsTest {

    private String payload = "{ \"messageId\" : \"messageId\", \"publishTime\" : \"publishTime\", \"data\" : \"data\", \"attributes\" : { \"attribute\" : \"attribute\" } }";
    private String payload_ancestry_kinds = "{ \"messageId\" : \"messageId\", \"publishTime\" : \"publishTime\", \"data\" : \"data\", \"attributes\" : { \"attribute\" : \"attribute\", \"ancestry_kinds\" : \"ancestry_kinds\" } }";
    private String payload_retry = "{ \"messageId\" : \"messageId\", \"publishTime\" : \"publishTime\", \"data\" : \"data\", \"attributes\" : { \"attribute\" : \"attribute\" , \"retry\" : \"11\" } }";
    private static final int INITIAL_RETRY_DELAY_SECONDS = 5;
    private final String retryString = "retry";
    private final Long countDownMillis = 123456L;
    private final String storage_sqs_url = "storage_sqs_url";
    private final String deadletter_queue_sqs_url = "deadletter_queue_sqs_url";

    @InjectMocks
    IndexerQueueTaskBuilderAws builder;

    @Mock
    AmazonSQS sqsClient;

    @Mock
    Gson gson;

    @Test
    public void createWorkerTaskTest_with_out_retryString() throws K8sParameterNotFoundException{

        Gson realGson = new Gson();

        DpsHeaders headers = new DpsHeaders();

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
        messageAttributes.put(retryString, new MessageAttributeValue()
                .withDataType("String")
                .withStringValue(String.valueOf(1)));

        RecordChangedMessages message = realGson.fromJson(payload, RecordChangedMessages.class);

        when(gson.fromJson(payload, RecordChangedMessages.class)).thenReturn(message);

        SendMessageRequest sendMessageRequest = new SendMessageRequest().withQueueUrl(null).withMessageBody(message.getData()).withDelaySeconds(new Integer(INITIAL_RETRY_DELAY_SECONDS)).withMessageAttributes(messageAttributes);

        builder.createWorkerTask(payload, headers);

        builder.createWorkerTask(payload, countDownMillis, headers);

        Mockito.verify(sqsClient, times(2)).sendMessage(Mockito.eq(sendMessageRequest));

    }

    @Test
    public void createWorkerTaskTest_with_retryString() throws K8sParameterNotFoundException{

        Gson realGson = new Gson();

        DpsHeaders headers = new DpsHeaders();

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
        messageAttributes.put(retryString, new MessageAttributeValue()
                .withDataType("String")
                .withStringValue(String.valueOf(1)));

        RecordChangedMessages message = realGson.fromJson(payload_retry, RecordChangedMessages.class);

        when(gson.fromJson(payload, RecordChangedMessages.class)).thenReturn(message);

        SendMessageRequest sendMessageRequest = new SendMessageRequest().withQueueUrl(null).withMessageBody(message.getData());

        builder.createWorkerTask(payload, headers);

        builder.createWorkerTask(payload, countDownMillis, headers);

        Mockito.verify(sqsClient, times(2)).sendMessage(Mockito.eq(sendMessageRequest));

    }

    @Test
    public void createWorkerTaskTest_with_ancestry_kinds() throws K8sParameterNotFoundException{

        Gson realGson = new Gson();

        DpsHeaders headers = new DpsHeaders();

        RecordChangedMessages message = realGson.fromJson(payload_ancestry_kinds, RecordChangedMessages.class);

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
        messageAttributes.put(retryString, new MessageAttributeValue()
                .withDataType("String")
                .withStringValue(String.valueOf(1)));
        messageAttributes.put(Constants.ANCESTRY_KINDS, new MessageAttributeValue()
                    .withDataType("String")
                    .withStringValue(message.getAttributes().get(Constants.ANCESTRY_KINDS)));

      

        when(gson.fromJson(payload, RecordChangedMessages.class)).thenReturn(message);

        SendMessageRequest sendMessageRequest = new SendMessageRequest().withQueueUrl(null).withMessageBody(message.getData()).withDelaySeconds(new Integer(INITIAL_RETRY_DELAY_SECONDS)).withMessageAttributes(messageAttributes);

        builder.createWorkerTask(payload, headers);

        builder.createWorkerTask(payload, countDownMillis, headers);

        Mockito.verify(sqsClient, times(2)).sendMessage(Mockito.eq(sendMessageRequest));

    }

    @Test
    public void createReIndexTaskTest() throws K8sParameterNotFoundException{

        DpsHeaders headers = new DpsHeaders();

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

        SendMessageRequest sendMessageRequest = new SendMessageRequest().withQueueUrl(null).withMessageBody(payload).withMessageAttributes(messageAttributes);

        builder.createReIndexTask(payload, headers);

        builder.createReIndexTask(payload, countDownMillis, headers);

        Mockito.verify(sqsClient, times(2)).sendMessage(Mockito.eq(sendMessageRequest));

    }

    @Test
    public void getWaitTimeExpTest() throws K8sParameterNotFoundException{
        
        int zero_wait_time = IndexerQueueTaskBuilderAws.getWaitTimeExp(0);

        assertEquals(0, zero_wait_time);

        int non_zero_wait_time = IndexerQueueTaskBuilderAws.getWaitTimeExp(4);

        assertEquals(64, non_zero_wait_time);
    }

    @Test
    public void go_through_init_StorageQueue() throws Exception  {

        try (MockedConstruction<K8sLocalParameterProvider> provider = Mockito.mockConstruction(K8sLocalParameterProvider.class, (mock, context) -> {
                                                                                                                when(mock.getParameterAsString(eq("STORAGE_SQS_URL"))).thenReturn(storage_sqs_url);
                                                                                                                when(mock.getParameterAsString("INDEXER_DEADLETTER_QUEUE_SQS_URL")).thenReturn(deadletter_queue_sqs_url);
                                                                                                            })) {

            try (MockedConstruction<AmazonSQSConfig> config = Mockito.mockConstruction(AmazonSQSConfig.class, (mock1, context) -> {
                                                                                                                when(mock1.AmazonSQS()).thenReturn(sqsClient);
                                                                                                            })) {

                builder.init();

                Gson realGson = new Gson();

                DpsHeaders headers = new DpsHeaders();

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
                messageAttributes.put(retryString, new MessageAttributeValue()
                    .withDataType("String")
                    .withStringValue(String.valueOf(1)));

                RecordChangedMessages message = realGson.fromJson(payload, RecordChangedMessages.class);

                SendMessageRequest sendMessageRequest = new SendMessageRequest().withQueueUrl(storage_sqs_url).withMessageBody(message.getData()).withDelaySeconds(new Integer(INITIAL_RETRY_DELAY_SECONDS)).withMessageAttributes(messageAttributes);

                builder.createWorkerTask(payload, headers);

                builder.createWorkerTask(payload, countDownMillis, headers);

                Mockito.verify(sqsClient, times(2)).sendMessage(Mockito.eq(sendMessageRequest));

            }
        }

    }

    @Test
    public void go_through_init_DLQ() throws Exception  {

        try (MockedConstruction<K8sLocalParameterProvider> provider = Mockito.mockConstruction(K8sLocalParameterProvider.class, (mock, context) -> {
                                                                                                                when(mock.getParameterAsString(eq("STORAGE_SQS_URL"))).thenReturn(storage_sqs_url);
                                                                                                                when(mock.getParameterAsString("INDEXER_DEADLETTER_QUEUE_SQS_URL")).thenReturn(deadletter_queue_sqs_url);
                                                                                                            })) {

            try (MockedConstruction<AmazonSQSConfig> config = Mockito.mockConstruction(AmazonSQSConfig.class, (mock1, context) -> {
                                                                                                                when(mock1.AmazonSQS()).thenReturn(sqsClient);
                                                                                                            })) {

                builder.init();

                Gson realGson = new Gson();

                DpsHeaders headers = new DpsHeaders();

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
                messageAttributes.put(retryString, new MessageAttributeValue()
                        .withDataType("String")
                        .withStringValue(String.valueOf(1)));

                RecordChangedMessages message = realGson.fromJson(payload_retry, RecordChangedMessages.class);

                SendMessageRequest sendMessageRequest = new SendMessageRequest().withQueueUrl(deadletter_queue_sqs_url).withMessageBody(message.getData());

                builder.createWorkerTask(payload_retry, headers);

                builder.createWorkerTask(payload_retry, countDownMillis, headers);

                Mockito.verify(sqsClient, times(2)).sendMessage(Mockito.eq(sendMessageRequest));
           

            }

        }

    }

}
