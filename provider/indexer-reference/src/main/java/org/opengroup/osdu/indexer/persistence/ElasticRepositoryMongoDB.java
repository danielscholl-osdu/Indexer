/*
 * Copyright 2021 Google LLC
 * Copyright 2021 EPAM Systems, Inc
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

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import java.util.Objects;
import org.apache.http.HttpStatus;
import org.bson.Document;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.search.ClusterSettings;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.IElasticRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ElasticRepositoryMongoDB implements IElasticRepository {

  private static final Logger LOG = LoggerFactory.getLogger(ElasticRepositoryMongoDB.class);

  private static final String SCHEMA_DATABASE = "local";
  private static final String SEARCH_SETTINGS = "SearchSettings";
  private static final String HOST = "host";
  private static final String PORT = "port";
  private static final String XPACK_RESTCLIENT_CONFIGURATION = "configuration";

  private final MongoDdmsClient mongoClient;

  @Autowired
  public ElasticRepositoryMongoDB(MongoDdmsClient mongoClient) {
    this.mongoClient = mongoClient;
  }

  @Override
  public ClusterSettings getElasticClusterSettings(TenantInfo tenantInfo) {
    MongoCollection<Document> mongoCollection = mongoClient
        .getMongoCollection(SCHEMA_DATABASE, SEARCH_SETTINGS);

    FindIterable<Document> results = mongoCollection.find();

    if (Objects.isNull(results) && Objects.isNull(results.first())) {
      LOG.error(String.format("Collection \'%s\' is empty.", results));
      throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Cluster setting fetch error",
          "An error has occurred fetching cluster settings from the database.");
    }

    Document document = results.first();

    String encryptedConfiguration = document.get(XPACK_RESTCLIENT_CONFIGURATION).toString();
    String encryptedHost = document.get(HOST).toString();
    String encryptedPort = document.get(PORT).toString();

    try {

      String host = encryptedHost;//this.kmsClient.decryptString(encryptedHost);
      String portString = encryptedPort;//this.kmsClient.decryptString(encryptedPort);
      String usernameAndPassword = encryptedConfiguration;//this.kmsClient.decryptString(encryptedConfiguration);

      int port = Integer.parseInt(portString);
      ClusterSettings clusterSettings = new ClusterSettings(host, port, usernameAndPassword);
      clusterSettings.setHttps(false);
      return clusterSettings;

    } catch (Exception e) {
      throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Cluster setting fetch error",
          "An error has occurred fetching cluster settings from the database.", e);
    }
  }
}
