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

package org.opengroup.osdu.indexer.azure.util;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import com.microsoft.azure.servicebus.Message;
import lombok.extern.java.Log;
import org.opengroup.osdu.azure.servicebus.ITopicClientFactory;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.indexer.RecordInfo;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.indexer.util.IndexerQueueTaskBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import javax.inject.Inject;
import javax.inject.Named;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Type;


@Log
@Component
@RequestScope
@Primary
public class IndexerQueueTaskBuilderAzure extends IndexerQueueTaskBuilder {

    @Autowired
    private ITopicClientFactory topicClientFactory;

    @Inject
    private JaxRsDpsLog logger;

    @Inject
    @Named("SERVICE_BUS_TOPIC")
    private String serviceBusTopic;

    @Override
    public void createWorkerTask(String payload, Long countdownMillis, DpsHeaders headers) {
        createTask(payload, headers);
    }

    @Override
    public void createReIndexTask(String payload, Long countdownMillis, DpsHeaders headers) {
        createTask(payload, headers);
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
        jo.addProperty(DpsHeaders.ACCOUNT_ID, headers.getPartitionIdWithFallbackToAccountId());
        jo.addProperty(DpsHeaders.DATA_PARTITION_ID, headers.getPartitionIdWithFallbackToAccountId());
        jo.addProperty(DpsHeaders.CORRELATION_ID, headers.getCorrelationId());
        JsonObject jomsg = new JsonObject();
        jomsg.add("message", jo);

        message.setBody(jomsg.toString().getBytes(StandardCharsets.UTF_8));
        message.setContentType("application/json");

        try {
            logger.info("Storage publishes message to Service Bus " + headers.getCorrelationId());
            topicClientFactory.getClient(headers.getPartitionId(), serviceBusTopic).send(message);
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
