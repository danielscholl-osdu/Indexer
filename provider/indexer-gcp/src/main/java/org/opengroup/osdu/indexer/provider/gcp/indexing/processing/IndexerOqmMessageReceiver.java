/*
 *  Copyright 2020-2022 Google LLC
 *  Copyright 2020-2022 EPAM Systems, Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opengroup.osdu.indexer.provider.gcp.indexing.processing;

import static org.opengroup.osdu.core.common.Constants.WORKER_RELATIVE_URL;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.opengroup.osdu.core.auth.TokenProvider;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.CloudTaskRequest;
import org.opengroup.osdu.core.gcp.oqm.model.OqmAckReplier;
import org.opengroup.osdu.core.gcp.oqm.model.OqmMessage;
import org.opengroup.osdu.core.gcp.oqm.model.OqmMessageReceiver;
import org.opengroup.osdu.indexer.provider.gcp.indexing.scope.ThreadDpsHeaders;
import org.opengroup.osdu.indexer.provider.gcp.indexing.thread.ThreadScopeContextHolder;

@Slf4j
@RequiredArgsConstructor
public class IndexerOqmMessageReceiver implements OqmMessageReceiver {

    private final ThreadDpsHeaders dpsHeaders;
    private final SubscriptionConsumer consumer;
    private final TokenProvider tokenProvider;
    private final Gson gson = new Gson();


    @Override
    public void receiveMessage(OqmMessage oqmMessage, OqmAckReplier oqmAckReplier) {
        log.info("OQM message: {} - {} - {}", oqmMessage.getId(), oqmMessage.getData(), oqmMessage.getAttributes());
        boolean acked = false;
        try {
            if (!validInput(oqmMessage)) {
                log.warn("Not valid event payload, event will not be processed.");
                oqmAckReplier.ack();
                return;
            }
            DpsHeaders headers = getHeaders(oqmMessage);
            // Filling thread context required by the core services.
            dpsHeaders.setThreadContext(headers.getHeaders());
            acked = sendMessage(oqmMessage);
        } catch (Exception e) {
            log.error("Error occurred during message receiving: ", e);
        } finally {
            if (!acked) {
                oqmAckReplier.nack();
            } else {
                oqmAckReplier.ack();
            }
            // Cleaning thread context after processing is finished and the thread dies out.
            ThreadScopeContextHolder.currentThreadScopeAttributes().clear();
        }
    }

    private boolean validInput(OqmMessage oqmMessage) {
        boolean isValid = true;
        if (Strings.isNullOrEmpty(oqmMessage.getData()) || oqmMessage.getData().equals("{}")) {
            log.warn("Message body is empty, message id: {}, attributes: {}", oqmMessage.getId(), oqmMessage.getAttributes());
            isValid = false;
        }
        if (oqmMessage.getAttributes() == null || oqmMessage.getAttributes().size() == 0) {
            log.warn("Attribute map not found, message id: {}, attributes: {}", oqmMessage.getId(), oqmMessage.getAttributes());
            isValid = false;
        }
        return isValid;
    }

    private boolean sendMessage(OqmMessage oqmMessage) {
        CloudTaskRequest cloudTaskRequest;
        JsonElement jsonElement = JsonParser.parseString(oqmMessage.getData());

        if (jsonElement.isJsonArray()) {
            cloudTaskRequest = getCloudTaskRequestProducedByStorageService(oqmMessage);
        } else {
            cloudTaskRequest = getCloudTaskRequestProducedByIndexerService(oqmMessage);
        }

        return consumer.consume(cloudTaskRequest);
    }

    /**
     * @param oqmMessage produced by Indexer packs messages in org.opengroup.osdu.core.common.model.search.CloudTaskRequest
     * @return CloudTaskRequest as it was packed by Indexer
     */
    private CloudTaskRequest getCloudTaskRequestProducedByIndexerService(OqmMessage oqmMessage) {
        return this.gson.fromJson(oqmMessage.getData(), CloudTaskRequest.class);
    }

    /**
     * @param oqmMessage produced by Storage packs messages in array of org.opengroup.osdu.core.common.model.storage.PubSubInfo ;
     * @return CloudTaskRequest with array of PubSubInfo's packed in message property
     */
    private CloudTaskRequest getCloudTaskRequestProducedByStorageService(OqmMessage oqmMessage) {
        return CloudTaskRequest.builder()
            .url(WORKER_RELATIVE_URL)
            .message(gson.toJson(oqmMessage))
            .build();
    }

    @NotNull
    private DpsHeaders getHeaders(OqmMessage oqmMessage) {
        DpsHeaders headers = new DpsHeaders();
        headers.getHeaders().put("data-partition-id", oqmMessage.getAttributes().get("data-partition-id"));
        headers.getHeaders().put("correlation-id", oqmMessage.getAttributes().get("correlation-id"));
        headers.getHeaders().put("account-id", oqmMessage.getAttributes().get("account-id"));
        headers.getHeaders().put("user", oqmMessage.getAttributes().get("user"));
        headers.getHeaders().put("authorization", "Bearer " + tokenProvider.getIdToken());
        return headers;
    }
}
