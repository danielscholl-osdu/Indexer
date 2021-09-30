/*
  Copyright 2020 Google LLC
  Copyright 2020 EPAM Systems, Inc

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

package org.opengroup.osdu.indexer.cache;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.Objects;
import javax.inject.Inject;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.search.ClusterSettings;
import org.opengroup.osdu.core.common.provider.interfaces.IElasticCredentialsCache;
import org.opengroup.osdu.core.common.provider.interfaces.IKmsClient;
import org.opengroup.osdu.indexer.config.IndexerConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
public class ElasticCredentialsCache implements IElasticCredentialsCache<String, ClusterSettings>, AutoCloseable {

    private IKmsClient kmsClient;
    private RedisCache<String, String> cache;

    @Inject
    public ElasticCredentialsCache(final IndexerConfigurationProperties properties, final IKmsClient kmsClient) {
        this.cache = new RedisCache<>(properties.getRedisSearchHost(), Integer.parseInt(properties.getRedisSearchPort()),
            properties.getElasticCacheExpiration() * 60, String.class, String.class);
        this.kmsClient = kmsClient;
    }

    @Override
    public void close() throws Exception {
        this.cache.close();
    }

    @Override
    public void put(String s, ClusterSettings o) {
        try {
            String jsonSettings = new Gson().toJson(o);
            String encryptString = kmsClient.encryptString(jsonSettings);
            this.cache.put(s, encryptString);
        } catch (IOException e) {
            throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Internal server error", "Unable to encrypt settings before being cached", e);
        }
    }

    @Override
    public ClusterSettings get(String s) {
        try {
            String encryptedSettings = this.cache.get(s);
            if (Objects.isNull(encryptedSettings) || encryptedSettings.isEmpty()) {
                return null;
            }
            String jsonSettings = this.kmsClient.decryptString(encryptedSettings);
            return new Gson().fromJson(jsonSettings, ClusterSettings.class);
        } catch (IOException e) {
            throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Internal server error", "Unable to decrypt settings from cache", e);
        }
    }

    @Override
    public void delete(String s) {
        this.cache.delete(s);
    }

    @Override
    public void clearAll() {
        this.cache.clearAll();
    }
}
