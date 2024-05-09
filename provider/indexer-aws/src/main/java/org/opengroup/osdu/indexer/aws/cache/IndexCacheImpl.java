/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.indexer.aws.cache;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.core.common.provider.interfaces.IIndexCache;

@RequiredArgsConstructor
public class IndexCacheImpl implements IIndexCache<String, Boolean>, AutoCloseable {

    private ICache<String, Boolean> cache;
    private Boolean local;

    public IndexCacheImpl(ICache<String, Boolean> cache, boolean local) {
        this.cache = cache;
        this.local = local;
    }

    @Override
    public void close() throws Exception {
        if (Boolean.TRUE.equals(this.local)) {
            // do nothing, this is using local dummy cache or vm cache
        } else {
            // cast to redis cache so it can be closed
            ((AutoCloseable) this.cache).close();
        }
    }

    @Override
    public void put(String s, Boolean o) {
        this.cache.put(s, o);
    }

    @Override
    public Boolean get(String s) {
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
