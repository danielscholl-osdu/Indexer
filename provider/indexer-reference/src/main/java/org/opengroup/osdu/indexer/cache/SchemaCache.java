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

package org.opengroup.osdu.indexer.cache;

import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.indexer.config.IndexerConfigurationProperties;
import org.opengroup.osdu.indexer.provider.interfaces.ISchemaCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SchemaCache implements ISchemaCache<String, String>, AutoCloseable {

  private RedisCache<String, String> cache;

  @Autowired
  public SchemaCache(IndexerConfigurationProperties indexerConfigurationProperties) {
    cache = new RedisCache<>(indexerConfigurationProperties.getRedisSearchHost(),
        Integer.parseInt(indexerConfigurationProperties.getRedisSearchPort()),
        indexerConfigurationProperties.getSchemaCacheExpiration() * 60,
        String.class,
        String.class);
  }

  @Override
  public void close() throws Exception {
    this.cache.close();
  }

  @Override
  public void put(String s, String o) {
    this.cache.put(s, o);
  }

  @Override
  public String get(String s) {
    return this.cache.get(s);
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
