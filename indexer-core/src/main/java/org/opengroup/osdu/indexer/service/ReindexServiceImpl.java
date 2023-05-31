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

import java.util.*;

import lombok.SneakyThrows;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.indexer.*;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.indexer.config.IndexerConfigurationProperties;
import org.opengroup.osdu.indexer.util.IndexerQueueTaskBuilder;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.core.common.provider.interfaces.IRequestInfo;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.stream.Collectors;

@Component
public class ReindexServiceImpl implements ReindexService {

    @Inject
    private IndexerConfigurationProperties configurationProperties;
    @Inject
    private StorageService storageService;
    @Inject
    private IndexerQueueTaskBuilder indexerQueueTaskBuilder;
    @Inject
    private IRequestInfo requestInfo;
    @Inject
    private IndexSchemaService indexSchemaService;
    @Inject
    private JaxRsDpsLog jaxRsDpsLog;

    @SneakyThrows
    @Override
    public String reindexKind(RecordReindexRequest recordReindexRequest, boolean forceClean) {
        Long initialDelayMillis = 0l;


        DpsHeaders headers = this.requestInfo.getHeadersWithDwdAuthZ();

        if (forceClean) {
            this.indexSchemaService.syncIndexMappingWithStorageSchema(recordReindexRequest.getKind());
            initialDelayMillis = 30000l;
        }

        RecordQueryResponse recordQueryResponse = this.storageService.getRecordsByKind(recordReindexRequest);

        if (recordQueryResponse.getResults() != null && recordQueryResponse.getResults().size() != 0) {

            List<RecordInfo> msgs = recordQueryResponse.getResults().stream()
                    .map(record -> RecordInfo.builder().id(record).kind(recordReindexRequest.getKind()).op(OperationType.create.name()).build()).collect(Collectors.toList());
            String recordChangedMessagePayload = this.replayReindexMsg(msgs, initialDelayMillis, headers);

            // don't call reindex-worker endpoint if it's the last batch
            // previous storage query result size will be less then requested (limit param)
            if (!Strings.isNullOrEmpty(recordQueryResponse.getCursor()) && recordQueryResponse.getResults().size() == configurationProperties.getStorageRecordsByKindBatchSize()) {
                Gson gson = new Gson();
                String newPayLoad = gson.toJson(RecordReindexRequest.builder().cursor(recordQueryResponse.getCursor()).kind(recordReindexRequest.getKind()).build());
                this.indexerQueueTaskBuilder.createReIndexTask(newPayLoad, initialDelayMillis, headers);
                return newPayLoad;
            }

            return recordChangedMessagePayload;
        } else {
            jaxRsDpsLog.info(String.format("kind: %s cannot be re-indexed, storage service cannot locate valid records", recordReindexRequest.getKind()));
        }
        return null;
    }

    @SneakyThrows
    @Override
    public Records reindexRecords(List<String> recordIds) {
        Records records = this.storageService.getStorageRecords(recordIds);
        if (records.getRecords().size() > 0) {
            List<RecordInfo> msgs = records.getRecords().stream()
                    .map(record -> RecordInfo.builder().id(record.getId()).kind(record.getKind()).op(OperationType.create.name()).build()).collect(Collectors.toList());
            this.replayReindexMsg(msgs, 0L, null);
        }
        return records;
    }

    @Override
    public void fullReindex(boolean forceClean) {
        List<String> allKinds = null;
        try {
            allKinds = storageService.getAllKinds();
        } catch (Exception e) {
            jaxRsDpsLog.error("storage service all kinds request failed", e);
            throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "storage service cannot respond with all kinds", "an unknown error has occurred.", e);
        }
        if (Objects.isNull(allKinds)) {
            throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "storage service cannot respond with all kinds", "full reindex failed");
        }
        for (String kind : allKinds) {
            try {
                reindexKind(new RecordReindexRequest(kind, ""), forceClean);
            } catch (Exception e) {
                jaxRsDpsLog.warning(String.format("kind: %s cannot be re-indexed", kind));
                continue;
            }
        }
    }

    private String replayReindexMsg(List<RecordInfo> msgs, Long initialDelayMillis, DpsHeaders headers) {
        Map<String, String> attributes = new HashMap<>();
        if (headers == null) {
            headers = this.requestInfo.getHeadersWithDwdAuthZ();
        }
        attributes.put(DpsHeaders.ACCOUNT_ID, headers.getAccountId());
        attributes.put(DpsHeaders.DATA_PARTITION_ID, headers.getPartitionIdWithFallbackToAccountId());
        attributes.put(DpsHeaders.CORRELATION_ID, headers.getCorrelationId());
        Gson gson = new Gson();
        RecordChangedMessages recordChangedMessages = RecordChangedMessages.builder().data(gson.toJson(msgs)).attributes(attributes).build();
        String recordChangedMessagePayload = gson.toJson(recordChangedMessages);
        this.indexerQueueTaskBuilder.createWorkerTask(recordChangedMessagePayload, initialDelayMillis, headers);
        return recordChangedMessagePayload;
    }
}
