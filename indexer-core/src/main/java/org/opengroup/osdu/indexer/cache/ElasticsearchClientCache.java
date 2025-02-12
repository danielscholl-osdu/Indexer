/*
 *  Copyright 2020-2024 Google LLC
 *  Copyright 2020-2024 EPAM Systems, Inc
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

package org.opengroup.osdu.indexer.cache;

import static org.opengroup.osdu.indexer.model.Constants.CLIENT_CACHE_EXPIRATION;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.core.common.cache.VmCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ElasticsearchClientCache implements ICache<String, ElasticsearchClient> {

  private final VmCache<String, ElasticsearchClient> vmCache;

  @Value("${elasticsearch.client.cache.size:50}")
  private int clientCacheSize;

  public ElasticsearchClientCache() {
    this.vmCache = new VmCache<>(CLIENT_CACHE_EXPIRATION, clientCacheSize);
  }

  @Override
  public void put(String partitionId, ElasticsearchClient client) {
    this.vmCache.put(partitionId, client);
  }

  @Override
  public ElasticsearchClient get(String partitionId) {
    return this.vmCache.get(partitionId);
  }

  @Override
  public void delete(String partitionId) {
    this.vmCache.delete(partitionId);
  }

  @Override
  public void clearAll() {
    this.vmCache.clearAll();
  }
}
