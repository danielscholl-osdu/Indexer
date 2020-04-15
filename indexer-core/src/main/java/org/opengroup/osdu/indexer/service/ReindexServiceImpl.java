// Copyright 2017-2019, Schlumberger
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

package org.opengroup.osdu.indexer.service;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import lombok.extern.java.Log;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.indexer.OperationType;
import org.opengroup.osdu.core.common.model.indexer.RecordQueryResponse;
import org.opengroup.osdu.core.common.model.indexer.RecordReindexRequest;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.indexer.util.IndexerQueueTaskBuilder;
import org.opengroup.osdu.core.common.model.indexer.RecordInfo;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.core.common.provider.interfaces.IRequestInfo;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Log
@Component
public class ReindexServiceImpl implements ReindexService {

    @Inject
    private StorageService storageService;
    @Inject
    private IndexerQueueTaskBuilder indexerQueueTaskBuilder;
    @Inject
    private IRequestInfo requestInfo;
    @Inject
    private JaxRsDpsLog jaxRsDpsLog;

    @Override
    public String reindexRecords(RecordReindexRequest recordReindexRequest) {

        try {
            DpsHeaders headers = this.requestInfo.getHeadersWithDwdAuthZ();

            RecordQueryResponse recordQueryResponse = this.storageService.getRecordsByKind(recordReindexRequest);

            if (recordQueryResponse.getResults() != null && recordQueryResponse.getResults().size() != 0) {

                List<RecordInfo> msgs = recordQueryResponse.getResults().stream()
                        .map(record -> RecordInfo.builder().id(record).kind(recordReindexRequest.getKind()).op(OperationType.create.name()).build()).collect(Collectors.toList());

                Map<String, String> attributes = new HashMap<>();
                attributes.put(DpsHeaders.ACCOUNT_ID, headers.getAccountId());
                attributes.put(DpsHeaders.DATA_PARTITION_ID, headers.getPartitionIdWithFallbackToAccountId());
                attributes.put(DpsHeaders.CORRELATION_ID, headers.getCorrelationId());

                Gson gson = new Gson();
                RecordChangedMessages recordChangedMessages = RecordChangedMessages.builder().data(gson.toJson(msgs)).attributes(attributes).build();
                String recordChangedMessagePayload = gson.toJson(recordChangedMessages);
                this.indexerQueueTaskBuilder.createWorkerTask(recordChangedMessagePayload, headers);

                if (!Strings.isNullOrEmpty(recordQueryResponse.getCursor())) {
                    String newPayLoad = gson.toJson(RecordReindexRequest.builder().cursor(recordQueryResponse.getCursor()).kind(recordReindexRequest.getKind()).build());
                    this.indexerQueueTaskBuilder.createReIndexTask(newPayLoad, headers);
                    return newPayLoad;
                }

                return recordChangedMessagePayload;
            } else {
                jaxRsDpsLog.info(String.format("kind: %s cannot be re-indexed, storage service cannot locate valid records", recordReindexRequest.getKind()));
            }
            return null;
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Unknown error", "An unknown error has occurred.", e);
        }
    }
}