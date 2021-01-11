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

package org.opengroup.osdu.indexer.cache;

import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.core.common.provider.interfaces.IAttributesCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Set;

@Component
public class AttributesCache implements IAttributesCache<String,Set>, AutoCloseable {

    private RedisCache<String, Set> cache;

    public AttributesCache(@Value("${REDIS_SEARCH_HOST}") final String REDIS_SEARCH_HOST,
                           @Value("${REDIS_SEARCH_PORT}") final String REDIS_SEARCH_PORT,
                           @Value("${INDEX_CACHE_EXPIRATION}") final String INDEX_CACHE_EXPIRATION) {

        cache = new RedisCache(REDIS_SEARCH_HOST, Integer.parseInt(REDIS_SEARCH_PORT),
                Integer.parseInt(INDEX_CACHE_EXPIRATION) * 60, String.class, Boolean.class);
    }

    @Override
    public void put(String key, Set value) {
        this.cache.put(key, value);
    }

    @Override
    public Set get(String key) {
        return this.cache.get(key);
    }

    @Override
    public void delete(String key) {
        this.cache.delete(key);
    }

    @Override
    public void clearAll() {
        this.cache.clearAll();
    }

    @Override
    public void close() {
        this.cache.close();
    }
}