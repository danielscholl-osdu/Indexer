

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

import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestHighLevelClient;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.search.IndexInfo;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.provider.interfaces.IRequestInfo;
import org.opengroup.osdu.core.common.search.IndicesService;
import org.opengroup.osdu.indexer.util.ElasticClientHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class CronServiceImpl implements CronService{

        @Inject
        private ElasticClientHandler elasticClientHandler;
        @Inject
        private IRequestInfo requestInfo;
        @Inject
        private IndicesService indicesService;
        @Inject
        private JaxRsDpsLog log;

        @Value("${CRON_INDEX_CLEANUP_THRESHOLD_DAYS}")
        private String CRON_INDEX_CLEANUP_THRESHOLD_DAYS;

        @Value("${CRON_EMPTY_INDEX_CLEANUP_THRESHOLD_DAYS}")
        private String CRON_EMPTY_INDEX_CLEANUP_THRESHOLD_DAYS;

        @Override
        public boolean cleanupIndices(String indexPattern) throws IOException {
            long threshHoldTime = Instant.now().minus(Integer.parseInt(CRON_INDEX_CLEANUP_THRESHOLD_DAYS), ChronoUnit.DAYS).toEpochMilli();

            try {
                try (RestHighLevelClient restClient = this.elasticClientHandler.createRestClient()) {
                    final List<IndexInfo> indicesList = this.indicesService.getIndexInfo(restClient, indexPattern);
                    for (IndexInfo settings : indicesList) {
                        long indexCreateTime = Long.parseLong(settings.getCreationDate());
                        if (indexCreateTime < threshHoldTime) {
                            this.deleteIndex(restClient, settings);
                        }
                    }
                }
                return true;
            } catch (ResponseException ex) {
                throw new AppException(ex.getResponse().getStatusLine().getStatusCode(), ex.getResponse().getStatusLine().getReasonPhrase(), "Error deleting indices.", ex);
            }
        }

        @Override
        public boolean cleanupEmptyStaleIndices() throws IOException {
            long threshHoldTime = Instant.now().minus(Integer.parseInt(CRON_EMPTY_INDEX_CLEANUP_THRESHOLD_DAYS), ChronoUnit.DAYS).toEpochMilli();

            try {
                try (RestHighLevelClient restClient = this.elasticClientHandler.createRestClient()) {
                    final List<IndexInfo> indicesList = this.indicesService.getIndexInfo(restClient, null);
                    for (IndexInfo settings : indicesList) {
                        long indexCreateTime = Long.parseLong(settings.getCreationDate());
                        long documentCount = Long.parseLong(settings.getDocumentCount());
                        if (documentCount > 0) break;

                        if (documentCount == 0 && indexCreateTime < threshHoldTime) {
                            this.deleteIndex(restClient, settings);
                        }
                    }
                }
                return true;
            } catch (ResponseException ex) {
                throw new AppException(ex.getResponse().getStatusLine().getStatusCode(), ex.getResponse().getStatusLine().getReasonPhrase(), "Error deleting indices.", ex);
            }
        }

        private void deleteIndex(RestHighLevelClient client, IndexInfo info) throws AppException {
            String partitionId = this.requestInfo.getPartitionId();
            try {
                this.log.info(String.format("Deleting index: %s | tenant: %s | created on: %s", info.getName(), partitionId, info.getCreationDate()));
                if (this.indicesService.deleteIndex(client, info.getName())) {
                    log.info(String.format("Deleted index: %s | tenant: %s | created on: %s", info.getName(), partitionId, info.getCreationDate()));
                }
            } catch (Exception ex) {
                this.log.warning(String.format("Failed to delete index: %s | tenant: %s | created on: %s", info.getName(), partitionId, info.getCreationDate()));
            }
        }
    }