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

import com.google.api.gax.retrying.RetrySettings;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import lombok.extern.java.Log;
import org.apache.http.HttpStatus;
import org.elasticsearch.common.Strings;

import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.indexer.JobStatus;
import org.opengroup.osdu.core.common.model.indexer.RecordStatus;
import org.opengroup.osdu.indexer.config.IndexerConfigurationProperties;
import org.opengroup.osdu.indexer.provider.interfaces.IPublisher;
import org.opengroup.osdu.core.gcp.PubSub.PubSubExtensions;
import org.opengroup.osdu.core.common.model.search.DeploymentEnvironment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import org.threeten.bp.Duration;

import javax.inject.Inject;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Log
@Component
@RequestScope
public class PublisherImpl implements IPublisher {

    private static final Map<String, Publisher> PUBSUB_CLIENTS = new HashMap<>();

    private static final String TOPIC_ID = "indexing-progress";


    @Inject
    private ITenantFactory tenantStorageFactory;

    @Inject
    private PubSubExtensions pubSubExtensions;

    @Inject
    private IndexerConfigurationProperties properties;

    @Override
    public void publishStatusChangedTagsToTopic(DpsHeaders headers, JobStatus indexerBatchStatus) throws Exception {

        // Don't publish to pubsub when testing locally
        if (properties.getDeploymentEnvironment() == DeploymentEnvironment.LOCAL) {
            return;
        }

        String tenant = headers.getPartitionId();
        if(Strings.isNullOrEmpty(tenant))
            tenant = headers.getAccountId();

        Publisher publisher = this.getPublisher(tenant);
        if (publisher == null) {
            throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Internal error", "A fatal internal error occurred creating publisher client.");
        }
        PubsubMessage pubsubMessage = getPubsubMessage(headers, indexerBatchStatus);

        pubSubExtensions.publishAndCreateTopicIfNotExist(publisher, pubsubMessage);

    }

    private static final RetrySettings RETRY_SETTINGS = RetrySettings.newBuilder()
            .setTotalTimeout(Duration.ofSeconds(30))
            .setInitialRetryDelay(Duration.ofSeconds(2))
            .setRetryDelayMultiplier(2)
            .setMaxRetryDelay(Duration.ofSeconds(5))
            .setInitialRpcTimeout(Duration.ofSeconds(10))
            .setRpcTimeoutMultiplier(2)
            .setMaxRpcTimeout(Duration.ofSeconds(10))
            .build();


    private PubsubMessage getPubsubMessage(DpsHeaders headers, JobStatus indexerBatchStatus) {

        Gson gson = new GsonBuilder().create();
        Type listType = new TypeToken<List<RecordStatus>>() {}.getType();
        JsonElement statusChangedTagsJson = gson.toJsonTree(indexerBatchStatus.getStatusesList(), listType);
        ByteString statusChangedTagsData = ByteString.copyFromUtf8(statusChangedTagsJson.toString());

        PubsubMessage.Builder builder = PubsubMessage.newBuilder();
        String tenant = headers.getPartitionId();
        //This code it to provide backward compatibility to slb-account-id
        if(!Strings.isNullOrEmpty(tenant)) {
            builder.putAttributes(DpsHeaders.DATA_PARTITION_ID, headers.getPartitionId());
        } else {
            builder.putAttributes(DpsHeaders.ACCOUNT_ID, headers.getAccountId());
        }

        builder.putAttributes(DpsHeaders.CORRELATION_ID, headers.getCorrelationId());
        // TODO: uncomment when deploying to production
//        builder.putAttributes(  AppEngineHeaders.CLOUD_TRACE_CONTEXT, headers.getHeaders().get(AppEngineHeaders.CLOUD_TRACE_CONTEXT));
        builder.setData(statusChangedTagsData);

        return builder.build();
    }

    private Publisher getPublisher(String tenantName) throws IOException {
        TenantInfo info = this.tenantStorageFactory.getTenantInfo(tenantName);
        if (info == null) {
            return null;
        } else {
            if (PUBSUB_CLIENTS.containsKey(tenantName)) return PUBSUB_CLIENTS.get(tenantName);

            ProjectTopicName topicName = ProjectTopicName.newBuilder().setProject(info.getProjectId()).setTopic(TOPIC_ID).build();
            Publisher publisher = Publisher.newBuilder(topicName).setRetrySettings(RETRY_SETTINGS).build();

            if (publisher == null) return null;

            PUBSUB_CLIENTS.put(tenantName, publisher);
            return publisher;
        }
    }
}