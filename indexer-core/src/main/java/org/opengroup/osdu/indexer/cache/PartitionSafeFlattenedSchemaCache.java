/*
 * Copyright © Schlumberger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.indexer.cache;

import org.opengroup.osdu.indexer.provider.interfaces.ISchemaCache;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import javax.inject.Inject;

@Component
@RequestScope
public class PartitionSafeFlattenedSchemaCache extends AbstractPartitionSafeCache<String, String> {
    private static final String FLATTENED_SCHEMA = "_flattened";
    @Inject
    private ISchemaCache schemaCache;

    @Override
    public void put(String s, String o) {
        this.schemaCache.put(getKey(s), o);
    }

    @Override
    public String get(String s) {
        return (String)this.schemaCache.get(getKey(s));
    }

    @Override
    public void delete(String s) {
        this.schemaCache.delete(getKey(s));
    }

    @Override
    public void clearAll() {
        this.schemaCache.clearAll();
    }

    private String getKey(String s) {
        return cacheKey(s) + FLATTENED_SCHEMA;
    }
}
