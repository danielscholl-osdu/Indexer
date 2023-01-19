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

import java.util.Collection;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.auth.TokenProvider;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.indexer.provider.gcp.indexing.scope.ThreadDpsHeaders;
import org.springframework.stereotype.Component;

/**
 * Subscription configuration class.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantSubscriberConfiguration {

    private static final String SUBSCRIPTION_PREFIX = "indexer-";
    private final IndexerMessagingConfigProperties properties;
    private final OqmSubscriberManager subscriberManager;
    private final ITenantFactory tenantInfoFactory;
    private final TokenProvider tokenProvider;
    private final SubscriptionConsumer consumer;
    private final ThreadDpsHeaders headers;

    /**
     * Tenant configurations provided by the Partition service will be used to configure subscribers. If tenants use the same message broker(The same RabbitMQ
     * instance, or the same GCP project Pub/Sub) then only one subscriber in this broker will be used.
     */
    @PostConstruct
    void postConstruct() {
        log.info("OqmSubscriberManager provisioning STARTED");
        IndexerOqmMessageReceiver recordsChangedMessageReceiver = new IndexerOqmMessageReceiver(headers, consumer, tokenProvider);
        IndexerOqmMessageReceiver reprocessOqmMessageReceiver = new IndexerOqmMessageReceiver(headers, consumer, tokenProvider);
        IndexerOqmMessageReceiver schemaOqmMessageReceiver = new IndexerOqmMessageReceiver(headers, consumer, tokenProvider);

        String recordsChangedTopicName = properties.getRecordsChangedTopicName();
        String reprocessTopicName = properties.getReprocessTopicName();
        String schemaChangedTopicName = properties.getSchemaChangedTopicName();

        Collection<TenantInfo> tenantInfos = tenantInfoFactory.listTenantInfo();

        for (TenantInfo tenantInfo : tenantInfos) {
            subscriberManager.registerSubscriber(tenantInfo, recordsChangedTopicName, getSubscriptionName(recordsChangedTopicName), recordsChangedMessageReceiver);
            subscriberManager.registerSubscriber(tenantInfo, reprocessTopicName, getSubscriptionName(reprocessTopicName), reprocessOqmMessageReceiver);
            subscriberManager.registerSubscriber(tenantInfo, schemaChangedTopicName, getSubscriptionName(schemaChangedTopicName), schemaOqmMessageReceiver);
        }
        log.info("OqmSubscriberManager provisioning COMPLETED");
    }

    private String getSubscriptionName(String topicName) {
        return SUBSCRIPTION_PREFIX + topicName;
    }
}
