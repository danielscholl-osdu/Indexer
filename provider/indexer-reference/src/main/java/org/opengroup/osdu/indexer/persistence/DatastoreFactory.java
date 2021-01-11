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

package org.opengroup.osdu.indexer.persistence;

import com.google.api.gax.retrying.RetrySettings;
import com.google.cloud.TransportOptions;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.http.HttpTransportOptions;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.indexer.cache.DatastoreCredentialCache;
import org.springframework.stereotype.Component;
import org.threeten.bp.Duration;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@Component
public class DatastoreFactory {

    @Inject
    private DatastoreCredentialCache cache;

    private static Map<String, Datastore> DATASTORE_CLIENTS = new HashMap<>();

    private static final RetrySettings RETRY_SETTINGS = RetrySettings.newBuilder()
            .setMaxAttempts(6)
            .setInitialRetryDelay(Duration.ofSeconds(10))
            .setMaxRetryDelay(Duration.ofSeconds(32))
            .setRetryDelayMultiplier(2.0)
            .setTotalTimeout(Duration.ofSeconds(50))
            .setInitialRpcTimeout(Duration.ofSeconds(50))
            .setRpcTimeoutMultiplier(1.0)
            .setMaxRpcTimeout(Duration.ofSeconds(50))
            .build();

    private static final TransportOptions TRANSPORT_OPTIONS = HttpTransportOptions.newBuilder()
            .setReadTimeout(30000)
            .build();

    public Datastore getDatastoreInstance(TenantInfo tenantInfo) {
        if (DATASTORE_CLIENTS.get(tenantInfo.getName()) == null) {
            Datastore googleDatastore = DatastoreOptions.newBuilder()
                    .setCredentials(new DatastoreCredential(tenantInfo, this.cache))
                    .setRetrySettings(RETRY_SETTINGS)
                    .setTransportOptions(TRANSPORT_OPTIONS)
                    .setNamespace(tenantInfo.getName())
                    .setProjectId(tenantInfo.getProjectId())
                    .build().getService();
            DATASTORE_CLIENTS.put(tenantInfo.getName(), googleDatastore);
        }
        return DATASTORE_CLIENTS.get(tenantInfo.getName());
    }
}
