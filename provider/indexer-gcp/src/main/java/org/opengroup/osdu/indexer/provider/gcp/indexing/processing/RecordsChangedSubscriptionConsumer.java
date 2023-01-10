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

import static org.opengroup.osdu.core.common.Constants.REINDEX_RELATIVE_URL;
import static org.opengroup.osdu.core.common.Constants.WORKER_RELATIVE_URL;

import com.google.gson.Gson;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.indexer.JobStatus;
import org.opengroup.osdu.core.common.model.indexer.RecordReindexRequest;
import org.opengroup.osdu.core.common.model.indexer.SchemaChangedMessages;
import org.opengroup.osdu.core.common.model.search.CloudTaskRequest;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.indexer.api.RecordIndexerApi;
import org.opengroup.osdu.indexer.api.ReindexApi;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecordsChangedSubscriptionConsumer implements SubscriptionConsumer {

    private static final String SCHEMA_RELATIVE_URL = "schema";

    private final DpsHeaders dpsHeaders;
    private final RecordIndexerApi recordIndexerApi;
    private final ReindexApi reindexApi;
    private final Gson gson = new Gson();

    public boolean consume(CloudTaskRequest request) {
        String url = request.getUrl();
        log.debug("Incoming async processing task: {} with headers: {}", request, dpsHeaders.getHeaders());

        try {
            if (url.equals(WORKER_RELATIVE_URL)) {
                RecordChangedMessages indexWorkerRequestBody = getIndexWorkerRequestBody(request, dpsHeaders);
                log.debug("Job message body: {}", indexWorkerRequestBody);
                ResponseEntity<JobStatus> jobStatusResponse = recordIndexerApi.indexWorker(indexWorkerRequestBody);
                log.info("Job status: {}", jobStatusResponse);
            } else if (url.equals(REINDEX_RELATIVE_URL)) {
                RecordReindexRequest reindexBody = getReindexBody(request);
                log.debug("Reindex job message body: {}", reindexBody);
                ResponseEntity<?> reindexResponse = reindexApi.reindex(reindexBody, false);
                log.info("Reindex job status: {}", reindexResponse);
            } else if (url.equals(SCHEMA_RELATIVE_URL)) {
                SchemaChangedMessages schemaChangedMessage = getSchemaWorkerRequestBody(request);
                log.debug("Schema changed job message body: {}", schemaChangedMessage);
                ResponseEntity<?> schemaChangeResponse = recordIndexerApi.schemaWorker(schemaChangedMessage);
                log.info("Schema changed job status: {}", schemaChangeResponse);
            }
            return true;
        } catch (AppException e) {
            int statusCode = e.getError().getCode();
            if (statusCode > 199 && statusCode < 300) {
                log.info("Event : {}, with headers: {} was not processed, with AppException: {} and will not be rescheduled", request, dpsHeaders.getHeaders(),
                    e);
                return true;
            } else {
                log.warn("Event : {}, with headers: {} was not processed, with AppException: {}, stack trace: {} and will be rescheduled", request,
                    dpsHeaders.getHeaders(), e.getOriginalException(), e.getOriginalException().getStackTrace());
                return false;
            }
        } catch (Exception e) {
            log.error("Error, Event : {}, with headers: {} was not processed, and will be rescheduled, reason: {}, stack trace: {}", request,
                dpsHeaders.getHeaders(), e, e.getStackTrace());
            return false;
        }
    }

    private RecordReindexRequest getReindexBody(CloudTaskRequest request) {
        return this.gson.fromJson(request.getMessage(), RecordReindexRequest.class);
    }

    private RecordChangedMessages getIndexWorkerRequestBody(CloudTaskRequest request, DpsHeaders dpsHeaders) {
        RecordChangedMessages recordChangedMessages = this.gson.fromJson(request.getMessage(), RecordChangedMessages.class);
        recordChangedMessages.setMessageId(dpsHeaders.getCorrelationId());
        recordChangedMessages.setPublishTime(LocalDateTime.now().toString());
        return recordChangedMessages;
    }

    private SchemaChangedMessages getSchemaWorkerRequestBody(CloudTaskRequest request) {
        SchemaChangedMessages schemaChangedMessage = this.gson.fromJson(request.getMessage(), SchemaChangedMessages.class);
        schemaChangedMessage.setMessageId(dpsHeaders.getCorrelationId());
        schemaChangedMessage.setPublishTime(LocalDateTime.now().toString());
        return schemaChangedMessage;
    }

}
