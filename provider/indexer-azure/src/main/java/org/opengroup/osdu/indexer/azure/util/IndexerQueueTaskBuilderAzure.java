// Copyright © Azure
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

package org.opengroup.osdu.indexer.azure.util;

import com.google.common.base.Strings;
import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import com.microsoft.azure.servicebus.Message;
import lombok.extern.java.Log;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.azure.servicebus.ITopicClientFactory;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.indexer.OperationType;
import org.opengroup.osdu.core.common.model.indexer.RecordInfo;
import org.opengroup.osdu.core.common.model.indexer.RecordQueryResponse;
import org.opengroup.osdu.core.common.model.indexer.RecordReindexRequest;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.indexer.config.IndexerConfigurationProperties;
import org.opengroup.osdu.indexer.service.StorageService;
import org.opengroup.osdu.indexer.util.IndexerQueueTaskBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import javax.inject.Inject;
import javax.inject.Named;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.opengroup.osdu.core.common.model.http.DpsHeaders.AUTHORIZATION;

@Log
@Component
@RequestScope
@Primary
public class IndexerQueueTaskBuilderAzure extends IndexerQueueTaskBuilder {

    @Autowired
    private ITopicClientFactory topicClientFactory;

    @Inject
    private IndexerConfigurationProperties configurationProperties;

    @Inject
    private JaxRsDpsLog logger;

    @Inject
    @Named("SERVICE_BUS_REINDEX_TOPIC")
    private String serviceBusReindexTopicName;

    @Inject
    private StorageService storageService;

    @Inject
    private RequestInfoImpl requestInfo;

    @Override
    public void createWorkerTask(String payload, DpsHeaders headers) {
        createTask(payload, headers);
    }

    @Override
    public void createWorkerTask(String payload, Long countdownMillis, DpsHeaders headers) {
        headers.addCorrelationIdIfMissing();
        createTask(payload, headers);
    }

    @Override
    public void createReIndexTask(String payload, DpsHeaders headers) {
        headers.addCorrelationIdIfMissing();
        publishAllRecordsToServiceBus(payload, headers);
    }

    @Override
    public void createReIndexTask(String payload, Long countdownMillis, DpsHeaders headers) {
        headers.addCorrelationIdIfMissing();
        publishAllRecordsToServiceBus(payload, headers);
    }

    private void publishAllRecordsToServiceBus(String payload, DpsHeaders headers) {
        // fetch all the remaining records
        // This logic is temporary and would be updated to call the storage service async.
        // Currently the storage client can't be called out of request scope hence making the
        // storage calls sync here
        Gson gson = new Gson();
        RecordReindexRequest recordReindexRequest = gson.fromJson(payload, RecordReindexRequest.class);
        final String recordKind = recordReindexRequest.getKind();
        RecordQueryResponse recordQueryResponse = null;

        try {
            do {
                headers.put(AUTHORIZATION, this.requestInfo.checkOrGetAuthorizationHeader());

                if (recordQueryResponse != null) {
                    recordReindexRequest = RecordReindexRequest.builder().cursor(recordQueryResponse.getCursor()).kind(recordKind).build();
                }
                recordQueryResponse = this.storageService.getRecordsByKind(recordReindexRequest);
                if (recordQueryResponse.getResults() != null && recordQueryResponse.getResults().size() != 0) {

                    List<RecordInfo> records = recordQueryResponse.getResults().stream()
                            .map(record -> RecordInfo.builder().id(record).kind(recordKind).op(OperationType.create.name()).build()).collect(Collectors.toList());

                    Map<String, String> attributes = new HashMap<>();
                    attributes.put(DpsHeaders.ACCOUNT_ID,  headers.getPartitionIdWithFallbackToAccountId());
                    attributes.put(DpsHeaders.DATA_PARTITION_ID,  headers.getPartitionIdWithFallbackToAccountId());
                    attributes.put(DpsHeaders.CORRELATION_ID, headers.getCorrelationId());

                    RecordChangedMessages recordChangedMessages = RecordChangedMessages.builder().data(gson.toJson(records)).attributes(attributes).build();
                    String recordChangedMessagePayload = gson.toJson(recordChangedMessages);
                    createTask(recordChangedMessagePayload, headers);
                }
            } while (!Strings.isNullOrEmpty(recordQueryResponse.getCursor()) && recordQueryResponse.getResults().size() == configurationProperties.getStorageRecordsByKindBatchSize());

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Unknown error", "An unknown error has occurred.", e);
        }
    }

    private void createTask(String payload, DpsHeaders headers) {
        Gson gson = new Gson();
        RecordChangedMessages receivedPayload = gson.fromJson(payload, RecordChangedMessages.class);

        Message message = new Message();
        Map<String, Object> properties = new HashMap<>();

        // properties
        properties.put(DpsHeaders.ACCOUNT_ID, headers.getPartitionIdWithFallbackToAccountId());
        properties.put(DpsHeaders.DATA_PARTITION_ID, headers.getPartitionIdWithFallbackToAccountId());
        headers.addCorrelationIdIfMissing();
        properties.put(DpsHeaders.CORRELATION_ID, headers.getCorrelationId());
        message.setProperties(properties);

        // data
        List<RecordInfo> recordInfos = parseRecordsAsJSON(receivedPayload.getData());

        // add all to body {"message": {"data":[], "id":...}}
        JsonObject jo = new JsonObject();
        jo.add("data", gson.toJsonTree(recordInfos));
        jo.add("attributes", gson.toJsonTree(receivedPayload.getAttributes()));
        jo.addProperty(DpsHeaders.ACCOUNT_ID, headers.getPartitionIdWithFallbackToAccountId());
        jo.addProperty(DpsHeaders.DATA_PARTITION_ID, headers.getPartitionIdWithFallbackToAccountId());
        jo.addProperty(DpsHeaders.CORRELATION_ID, headers.getCorrelationId());
        JsonObject jomsg = new JsonObject();
        jomsg.add("message", jo);

        message.setBody(jomsg.toString().getBytes(StandardCharsets.UTF_8));
        message.setContentType("application/json");

        try {
            long startTime = System.currentTimeMillis();
            topicClientFactory.getClient(headers.getPartitionId(), serviceBusReindexTopicName).send(message);
            long stopTime = System.currentTimeMillis();
            logger.info(String.format("Indexer publishes message to Service Bus, messageId: %s | time taken to send message: %d milliseconds ", message.getMessageId(), stopTime - startTime));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private List<RecordInfo> parseRecordsAsJSON(String inputPayload) {
        Gson gson = new Gson();
        Type type = new TypeToken<List<RecordInfo>>(){}.getType();
        List<RecordInfo> recordInfoList = gson.fromJson(inputPayload, type);
        return  recordInfoList;
    }
}
