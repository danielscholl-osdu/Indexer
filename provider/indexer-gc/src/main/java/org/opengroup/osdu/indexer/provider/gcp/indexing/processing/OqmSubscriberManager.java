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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.gcp.oqm.driver.OqmDriver;
import org.opengroup.osdu.core.gcp.oqm.model.OqmDestination;
import org.opengroup.osdu.core.gcp.oqm.model.OqmMessageReceiver;
import org.opengroup.osdu.core.gcp.oqm.model.OqmSubscriber;
import org.opengroup.osdu.core.gcp.oqm.model.OqmSubscriberThroughput;
import org.opengroup.osdu.core.gcp.oqm.model.OqmSubscription;
import org.opengroup.osdu.core.gcp.oqm.model.OqmSubscriptionQuery;
import org.opengroup.osdu.core.gcp.oqm.model.OqmTopic;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class OqmSubscriberManager {

    private final OqmDriver driver;

    private OqmSubscription getOrCreateSubscriptionForTenant(TenantInfo tenantInfo, String topicName, String subscriptionName) {
        log.info("OQM: provisioning tenant {}:", tenantInfo.getDataPartitionId());
        log.info("OQM: check for topic {} existence:", topicName);
        OqmTopic topic = driver.getTopic(topicName, getDestination(tenantInfo))
            .orElse(null);
        if (topic == null) {
            log.info("OQM: check for topic {} existence: ABSENT. Skipped", topicName);
            throw new RuntimeException();
        }

        log.info("OQM: check for topic {} existence: PRESENT", topicName);
        OqmSubscription subscription = getSubscription(tenantInfo, topic, subscriptionName);

        if (subscription == null) {
            subscription = createSubscription(tenantInfo, topic, subscriptionName);
        } else {
            log.info("OQM: check for subscription {} existence: PRESENT", subscriptionName);
        }
        log.info("OQM: provisioning tenant {}: COMPLETED.", tenantInfo.getDataPartitionId());
        return subscription;
    }

    @Nullable
    private OqmSubscription getSubscription(TenantInfo tenantInfo, OqmTopic topic, String subscriptionName) {
        log.info("OQM: check for subscription {} existence:", subscriptionName);
        OqmSubscriptionQuery query = OqmSubscriptionQuery.builder()
            .namePrefix(subscriptionName)
            .subscriberable(true)
            .build();
        return driver
            .listSubscriptions(topic, query, getDestination(tenantInfo)).stream()
            .findAny()
            .orElse(null);
    }

    private OqmSubscription createSubscription(TenantInfo tenantInfo, OqmTopic topic, String subscriptionName) {
        log.info("OQM: check for subscription {} existence: ABSENT. Will create.", subscriptionName);
        OqmSubscription request = OqmSubscription.builder()
            .topic(topic)
            .name(subscriptionName)
            .build();
        return driver.createAndGetSubscription(request, getDestination(tenantInfo));
    }

    public void registerSubscriber(TenantInfo tenantInfo, String topicName, String subscriptionName, OqmMessageReceiver messageReceiver, OqmSubscriberThroughput throughput) {
        OqmSubscription subscriptionForTenant = getOrCreateSubscriptionForTenant(tenantInfo, topicName, subscriptionName);
        log.info("OQM: registering Subscriber for subscription {}", subscriptionName);
        OqmDestination destination = getDestination(tenantInfo);

        OqmSubscriber subscriber = OqmSubscriber.builder()
            .subscription(subscriptionForTenant)
            .messageReceiver(messageReceiver)
            .throughput(throughput)
            .build();
        driver.subscribe(subscriber, destination);
        log.info("OQM: provisioning subscription {}: Subscriber REGISTERED.", subscriptionName);
    }

    private OqmDestination getDestination(TenantInfo tenantInfo) {
        return OqmDestination.builder()
            .partitionId(tenantInfo.getDataPartitionId())
            .build();
    }
}
