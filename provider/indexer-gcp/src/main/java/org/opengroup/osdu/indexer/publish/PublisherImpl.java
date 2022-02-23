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

package org.opengroup.osdu.indexer.publish;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.indexer.JobStatus;
import org.opengroup.osdu.core.gcp.oqm.driver.OqmDriver;
import org.opengroup.osdu.core.gcp.oqm.model.OqmDestination;
import org.opengroup.osdu.core.gcp.oqm.model.OqmMessage;
import org.opengroup.osdu.core.gcp.oqm.model.OqmTopic;
import org.opengroup.osdu.indexer.provider.interfaces.IPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Log
@Component
@RequestScope
@RequiredArgsConstructor
public class PublisherImpl implements IPublisher {

    private static final String TOPIC_ID = "indexing-progress";

    private final OqmDriver driver;

    private final OqmTopic oqmTopic = OqmTopic.builder().name(TOPIC_ID).build();

    @Override
    public void publishStatusChangedTagsToTopic(DpsHeaders headers, JobStatus indexerBatchStatus) {
        OqmDestination oqmDestination = OqmDestination.builder().partitionId(headers.getPartitionId())
            .build();
        String json = generatePubSubMessage(indexerBatchStatus);

        Map<String, String> attributes = getAttributes(headers);
        OqmMessage oqmMessage = OqmMessage.builder().data(json).attributes(attributes).build();
        driver.publish(oqmMessage, oqmTopic, oqmDestination);
    }

    private Map<String, String> getAttributes(DpsHeaders headers) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put(DpsHeaders.ACCOUNT_ID, headers.getPartitionIdWithFallbackToAccountId());
        attributes.put(DpsHeaders.DATA_PARTITION_ID, headers.getPartitionIdWithFallbackToAccountId());
        headers.addCorrelationIdIfMissing();
        attributes.put(DpsHeaders.CORRELATION_ID, headers.getCorrelationId());
        return attributes;
    }

    private String generatePubSubMessage(JobStatus jobStatus) {
        Gson gson = new GsonBuilder().create();
        JsonElement statusChangedTagsJson = gson.toJsonTree(jobStatus, JobStatus.class);
        return statusChangedTagsJson.toString();
    }
}