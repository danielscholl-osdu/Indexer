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

package org.opengroup.osdu.indexer.persistence;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import lombok.extern.java.Log;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.model.search.ClusterSettings;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.provider.interfaces.IKmsClient;
import org.opengroup.osdu.core.common.provider.interfaces.IElasticRepository;
import org.opengroup.osdu.core.common.search.Preconditions;
import org.opengroup.osdu.core.gcp.multitenancy.DatastoreFactory;
import org.opengroup.osdu.indexer.config.IndexerConfigurationProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.inject.Inject;

@Log
@Component
public class ElasticRepositoryDatastore implements IElasticRepository {

    static final String HOST = "host";
    static final String PORT = "port";
    static final String XPACK_RESTCLIENT_CONFIGURATION = "configuration";

    @Inject
    private IKmsClient kmsClient;
    @Inject
    private DatastoreFactory datastoreFactory;

    @Inject
    private IndexerConfigurationProperties properties;


    @Override
    public ClusterSettings getElasticClusterSettings(TenantInfo tenantInfo) {

        Datastore googleDatastore = this.datastoreFactory.getDatastore(tenantInfo);
        Key key = googleDatastore.newKeyFactory().setKind(properties.getElasticDatastoreKind()).newKey(properties.getElasticDatastoreId());
        Entity datastoreEntity = googleDatastore.get(key);

        if (datastoreEntity == null) {
            throw new AppException(HttpStatus.SC_NOT_FOUND, "Cluster setting not found", "The requested cluster setting was not found in datastore.", String.format("Cluster setting with key: '%s' does not exist in datastore.", key.getName()));
        }

        String encryptedHost = null;
        String encryptedPort = null;
        String encryptedConfiguration = null;

        try {
            encryptedHost = datastoreEntity.getString(HOST);
            encryptedPort = datastoreEntity.getString(PORT);
            encryptedConfiguration = datastoreEntity.getString(XPACK_RESTCLIENT_CONFIGURATION);

            String host = this.kmsClient.decryptString(encryptedHost);
            String portString = this.kmsClient.decryptString(encryptedPort);
            String usernameAndPassword = this.kmsClient.decryptString(encryptedConfiguration);

            Preconditions.checkNotNullOrEmpty(host, "host cannot be null");
            Preconditions.checkNotNullOrEmpty(portString, "port cannot be null");
            Preconditions.checkNotNullOrEmpty(usernameAndPassword, "configuration cannot be null");

            int port = Integer.parseInt(portString);

            return new ClusterSettings(host, port, usernameAndPassword);
        } catch (GoogleJsonResponseException e) {
            String debuggingInfo = String.format("Host: %s | port: %s | configuration: %s", encryptedHost, encryptedPort, encryptedConfiguration);
            throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Cluster setting decryption error", "An error has occurred decrypting cluster settings.", debuggingInfo, e);
        } catch (Exception e) {
            throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Cluster setting fetch error", "An error has occurred fetching cluster settings from the datastore.", e);
        }
    }
}