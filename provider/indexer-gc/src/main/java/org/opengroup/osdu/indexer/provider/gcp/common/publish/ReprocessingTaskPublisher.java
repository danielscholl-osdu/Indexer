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

package org.opengroup.osdu.indexer.provider.gcp.common.publish;

import static org.opengroup.osdu.core.common.Constants.REINDEX_RELATIVE_URL;
import static org.opengroup.osdu.core.common.Constants.WORKER_RELATIVE_URL;

import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.gcp.oqm.driver.OqmDriver;
import org.opengroup.osdu.core.gcp.oqm.model.OqmDestination;
import org.opengroup.osdu.core.gcp.oqm.model.OqmMessage;
import org.opengroup.osdu.core.gcp.oqm.model.OqmTopic;
import org.opengroup.osdu.indexer.provider.gcp.indexing.processing.IndexerMessagingConfigProperties;
import org.opengroup.osdu.indexer.util.IndexerQueueTaskBuilder;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class ReprocessingTaskPublisher extends IndexerQueueTaskBuilder {

  private final Gson gson = new Gson();

  private final OqmDriver driver;

  private final TenantInfo tenantInfo;

  private final IndexerMessagingConfigProperties properties;

  private OqmTopic reprocessOqmTopic;

  private OqmTopic recordsChangedTopic;

  @PostConstruct
  public void setUp() {
    reprocessOqmTopic = OqmTopic.builder().name(properties.getReprocessTopicName()).build();
    recordsChangedTopic = OqmTopic.builder().name(properties.getRecordsChangedTopicName()).build();
  }

  public void createWorkerTask(String payload, DpsHeaders headers) {
    publishRecordsChangedTask(WORKER_RELATIVE_URL, payload, 0l, headers);
  }

  public void createWorkerTask(String payload, Long countdownMillis, DpsHeaders headers) {
    publishRecordsChangedTask(WORKER_RELATIVE_URL, payload, countdownMillis, headers);
  }

  public void createReIndexTask(String payload, DpsHeaders headers) {
    publishReindexTask(REINDEX_RELATIVE_URL, payload, 0l, headers);
  }

  public void createReIndexTask(String payload, Long countdownMillis, DpsHeaders headers) {
    publishReindexTask(REINDEX_RELATIVE_URL, payload, countdownMillis, headers);
  }

  private void publishReindexTask(String url, String payload, Long countdownMillis,
      DpsHeaders headers) {
    OqmDestination oqmDestination = OqmDestination.builder().partitionId(headers.getPartitionId())
        .build();
    Map<String, String> attributes = getAttributesFromHeaders(headers);
    OqmMessage oqmMessage = OqmMessage.builder().data(payload).attributes(attributes).build();
    log.info("Reprocessing task: {} ,has been published.", oqmMessage);
    driver.publish(oqmMessage, reprocessOqmTopic, oqmDestination);
  }

  private void publishRecordsChangedTask(String url, String payload, Long countdownMillis,
      DpsHeaders headers) {
    OqmDestination oqmDestination = OqmDestination.builder()
        .partitionId(headers.getPartitionId())
        .build();

    RecordChangedMessages recordChangedMessages = gson.fromJson(payload,
        RecordChangedMessages.class);

    OqmMessage oqmMessage = OqmMessage.builder()
        .id(headers.getCorrelationId())
        .data(recordChangedMessages.getData())
        .attributes(getAttributesFromHeaders(headers))
        .build();

    log.info("Reprocessing task: {} ,has been published.", oqmMessage);
    driver.publish(oqmMessage, recordsChangedTopic, oqmDestination);
  }

  @NotNull
  private Map<String, String> getAttributesFromHeaders(DpsHeaders headers) {
    Map<String, String> attributes = new HashMap<>();
    attributes.put(DpsHeaders.USER_EMAIL, headers.getUserEmail());
    attributes.put(DpsHeaders.ACCOUNT_ID, this.tenantInfo.getName());
    attributes.put(DpsHeaders.DATA_PARTITION_ID, headers.getPartitionIdWithFallbackToAccountId());
    headers.addCorrelationIdIfMissing();
    attributes.put(DpsHeaders.CORRELATION_ID, headers.getCorrelationId());
    return attributes;
  }
}
