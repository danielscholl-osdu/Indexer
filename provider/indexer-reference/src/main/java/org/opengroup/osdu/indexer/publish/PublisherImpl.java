/*
 * Copyright 2020 Google LLC
 * Copyright 2020 EPAM Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.indexer.publish;


import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.elasticsearch.common.Strings;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.indexer.JobStatus;
import org.opengroup.osdu.core.common.model.indexer.RecordStatus;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.indexer.messagebus.IMessageFactory;
import org.opengroup.osdu.indexer.provider.interfaces.IPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class PublisherImpl implements IPublisher {

	private static final Logger LOG = LoggerFactory.getLogger(PublisherImpl.class);

	@Autowired
	IMessageFactory mq;


	@Override
	public void publishStatusChangedTagsToTopic(DpsHeaders headers, JobStatus indexerBatchStatus) throws Exception {

		String tenant = headers.getPartitionId();
		if (Strings.isNullOrEmpty(tenant)) {
			tenant = headers.getAccountId();
		}

		Map<String, String> message = new HashMap<>();
		message.put(tenant, headers.getPartitionIdWithFallbackToAccountId());
		headers.addCorrelationIdIfMissing();
		message.put(DpsHeaders.CORRELATION_ID, headers.getCorrelationId());

		RecordChangedMessages recordChangedMessages = getRecordChangedMessage(headers, indexerBatchStatus);
		message.put("data", recordChangedMessages.toString());

		try {
			LOG.info("Indexer publishes message " + headers.getCorrelationId());
			mq.sendMessage(IMessageFactory.INDEXER_QUEUE_NAME, message.toString());
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}

	private RecordChangedMessages getRecordChangedMessage(DpsHeaders headers, JobStatus indexerBatchStatus) {

		Gson gson = new GsonBuilder().create();
		Map<String, String> attributesMap = new HashMap<>();
		Type listType = new TypeToken<List<RecordStatus>>() {
		}.getType();

		JsonElement statusChangedTagsJson = gson.toJsonTree(indexerBatchStatus.getStatusesList(), listType);
		String statusChangedTagsData = (statusChangedTagsJson.toString());

		String tenant = headers.getPartitionId();
		// This code it to provide backward compatibility to slb-account-id
		if (!Strings.isNullOrEmpty(tenant)) {
			attributesMap.put(DpsHeaders.DATA_PARTITION_ID, headers.getPartitionIdWithFallbackToAccountId());
		} else {
			attributesMap.put(DpsHeaders.ACCOUNT_ID, headers.getPartitionIdWithFallbackToAccountId());
		}
		headers.addCorrelationIdIfMissing();
		attributesMap.put(DpsHeaders.CORRELATION_ID, headers.getCorrelationId());

		RecordChangedMessages recordChangedMessages = new RecordChangedMessages();
		// statusChangedTagsData is not ByteString but String
		recordChangedMessages.setData(statusChangedTagsData);
		recordChangedMessages.setAttributes(attributesMap);

		return recordChangedMessages;
	}
}
