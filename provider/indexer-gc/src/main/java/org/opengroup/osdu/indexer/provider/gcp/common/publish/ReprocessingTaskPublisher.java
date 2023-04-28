/*
 *  Copyright 2020-2023 Google LLC
 *  Copyright 2020-2023 EPAM Systems, Inc
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

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.core.gcp.oqm.driver.OqmDriver;
import org.opengroup.osdu.core.gcp.oqm.model.OqmDestination;
import org.opengroup.osdu.core.gcp.oqm.model.OqmMessage;
import org.opengroup.osdu.core.gcp.oqm.model.OqmTopic;
import org.opengroup.osdu.indexer.model.Constants;
import org.opengroup.osdu.indexer.provider.gcp.indexing.processing.IndexerMessagingConfigProperties;
import org.opengroup.osdu.indexer.util.IndexerQueueTaskBuilder;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class ReprocessingTaskPublisher extends IndexerQueueTaskBuilder {

  private final Gson gson = new Gson();

  private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

  private final OqmDriver driver;

  private final IndexerMessagingConfigProperties properties;

  private OqmTopic reprocessOqmTopic;

  private OqmTopic recordsChangedTopic;

  @PostConstruct
  public void setUp() {
    reprocessOqmTopic = OqmTopic.builder().name(properties.getReprocessTopicName()).build();
    recordsChangedTopic = OqmTopic.builder().name(properties.getRecordsChangedTopicName()).build();
  }

  public void createWorkerTask(String payload, DpsHeaders headers) {
    publishRecordsChangedTask(payload, headers);
  }

  public void createWorkerTask(String payload, Long countdownMillis, DpsHeaders headers) {
    DpsHeaders headersCopy = DpsHeaders.createFromMap(headers.getHeaders());
    scheduledExecutorService.schedule(
        () -> {
          try {
            publishRecordsChangedTask(payload, headersCopy);
          } catch (Exception e) {
            // If error or exception not caught, executor will die out silently.
            log.error("The exception was thrown during scheduled event publishing!", e);
            throw e;
          } catch (Throwable e) {
            log.error("The Error was thrown during scheduled event publishing!", e);
            throw e;
          }
        },
        countdownMillis,
        TimeUnit.MILLISECONDS
    );
  }

  public void createReIndexTask(String payload, DpsHeaders headers) {
    publishReindexTask(payload, headers);
  }

  public void createReIndexTask(String payload, Long countdownMillis, DpsHeaders headers) {
    DpsHeaders headersCopy = DpsHeaders.createFromMap(headers.getHeaders());
    scheduledExecutorService.schedule(
        () -> {
          try {
            publishReindexTask(payload, headersCopy);
          } catch (Exception e) {
            // If error or exception not caught, executor will die out silently.
            log.error("The exception was thrown during scheduled event publishing!", e);
            throw e;
          } catch (Throwable e) {
            log.error("The Error was thrown during scheduled event publishing!", e);
            throw e;
          }
        },
        countdownMillis,
        TimeUnit.MILLISECONDS
    );
  }

  private void publishReindexTask(String payload, DpsHeaders headers) {
    OqmDestination oqmDestination = OqmDestination.builder().partitionId(headers.getPartitionId())
        .build();
    Map<String, String> attributes = getAttributesFromHeaders(headers);
    OqmMessage oqmMessage = OqmMessage.builder().data(payload).attributes(attributes).build();
    log.info("Reprocessing task: {} ,has been published.", oqmMessage);
    driver.publish(oqmMessage, reprocessOqmTopic, oqmDestination);
  }

  private void publishRecordsChangedTask(String payload, DpsHeaders headers) {
    OqmDestination oqmDestination = OqmDestination.builder()
        .partitionId(headers.getPartitionId())
        .build();

    RecordChangedMessages recordChangedMessages = gson.fromJson(payload,
        RecordChangedMessages.class);

    Map<String, String> attributes = getAttributesFromHeaders(headers);
    // Append the ancestry kinds used to prevent circular chasing
    if(recordChangedMessages.getAttributes().containsKey(Constants.ANCESTRY_KINDS)) {
      attributes.put(Constants.ANCESTRY_KINDS, recordChangedMessages.getAttributes().get(Constants.ANCESTRY_KINDS));
    }

    OqmMessage oqmMessage = OqmMessage.builder()
        .id(headers.getCorrelationId())
        .data(recordChangedMessages.getData())
        .attributes(attributes)
        .build();

    log.info("Reprocessing task: {} ,has been published.", oqmMessage);
    driver.publish(oqmMessage, recordsChangedTopic, oqmDestination);
  }

  @NotNull
  private Map<String, String> getAttributesFromHeaders(DpsHeaders headers) {
    Map<String, String> attributes = new HashMap<>();
    attributes.put(DpsHeaders.USER_EMAIL, headers.getUserEmail());
    attributes.put(DpsHeaders.ACCOUNT_ID, headers.getPartitionId());
    attributes.put(DpsHeaders.DATA_PARTITION_ID, headers.getPartitionIdWithFallbackToAccountId());
    headers.addCorrelationIdIfMissing();
    attributes.put(DpsHeaders.CORRELATION_ID, headers.getCorrelationId());
    return attributes;
  }
}
