// Copyright © Amazon Web Services
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

package org.opengroup.osdu.indexer.aws.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.opengroup.osdu.core.aws.cache.AwsRedisCache;
import org.opengroup.osdu.core.aws.ssm.K8sParameterNotFoundException;
import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.indexer.provider.interfaces.ISchemaCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SchemaCacheImpl implements ISchemaCache<String, String>, AutoCloseable {

    private ICache<String, String> cache;
    private Boolean local = false;
    public SchemaCacheImpl(@Value("${aws.elasticache.cluster.schema.expiration}") final String SCHEMA_CACHE_EXPIRATION) throws K8sParameterNotFoundException, JsonProcessingException {
        cache = AwsRedisCache.RedisCache(Integer.parseInt(SCHEMA_CACHE_EXPIRATION) * 60, String.class, String.class);
        if (cache.getClass() == RedisCache.class){
            local = false;
        }else{
            local = true;
        }
    }

    @Override
    public void close() throws Exception {
        if (this.local){
            // do nothing, this is using local dummy cache
        }else {
            // cast to redis cache so it can be closed
            ((RedisCache)this.cache).close();
        }
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
