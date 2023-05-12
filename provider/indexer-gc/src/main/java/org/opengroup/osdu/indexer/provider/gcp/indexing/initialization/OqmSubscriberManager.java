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

package org.opengroup.osdu.indexer.provider.gcp.indexing.initialization;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.gcp.oqm.driver.OqmDriver;
import org.opengroup.osdu.core.gcp.oqm.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;

@Service
@Slf4j
@RequiredArgsConstructor
public class OqmSubscriberManager {

    private final OqmDriver driver;

    private OqmSubscription getSubscriptionForTenant(TenantInfo tenantInfo, String topicName, String subscriptionName) {
        String dataPartitionId = tenantInfo.getDataPartitionId();
        log.info("OQM: provisioning tenant {}:", dataPartitionId);
        log.info("OQM: check for topic {} existence:", topicName);
        OqmTopic topic = driver.getTopic(topicName, getDestination(tenantInfo)).orElse(null);

        if (topic == null) {
            log.error("OQM: check for topic: {}, tenant: {} existence: ABSENT.", topicName,
                dataPartitionId);
            throw new AppException(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Required topic not exists.",
                String.format(
                    "Required topic not exists. Create topic: %s for tenant: %s and restart service.",
                    topicName, dataPartitionId
                )
            );
        }

        log.info("OQM: check for topic {} existence: PRESENT", topicName);
        OqmSubscription subscription = getSubscription(tenantInfo, topic, subscriptionName);

        if (subscription == null) {
            log.error(
                "OQM: check for subscription {}, tenant: {} existence: ABSENT.",
                subscriptionName,
                dataPartitionId
            );
            throw new AppException(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Required subscription not exists.",
                String.format(
                    "Required subscription not exists. Create subscription: %s for tenant: %s and restart service.",
                    subscriptionName,
                    dataPartitionId
                )
            );
        }
        log.info("OQM: provisioning tenant {}: COMPLETED.", dataPartitionId);
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

    public void registerSubscriber(TenantInfo tenantInfo, String topicName, String subscriptionName, OqmMessageReceiver messageReceiver, OqmSubscriberThroughput throughput) {
        OqmSubscription subscriptionForTenant = getSubscriptionForTenant(tenantInfo, topicName, subscriptionName);
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
