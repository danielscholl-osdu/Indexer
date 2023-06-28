//  Copyright © Schlumberger
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.opengroup.osdu.indexer.azure.di;

import com.azure.security.keyvault.secrets.SecretClient;
import org.opengroup.osdu.azure.KeyVaultFacade;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.inject.Named;

@Configuration
public class RedisConfig {

    @Value("${redis.port:6380}")
    private int port;

    @Value("${redis.index.ttl:3600}")
    public int indexRedisTtl;

    // Azure service account id_token can be requested only for 1 hr
    @Value("${redis.jwt.ttl:3540}")
    public int jwtTtl;

    @Value("${redis.schema.ttl:3600}")
    public int schemaTtl;

    @Value("${redis.records.ttl:120}")
    public int recordsTtl;

    @Bean
    @Named("REDIS_PORT")
    public int getRedisPort() {
        return port;
    }

    @Bean
    @Named("INDEX_REDIS_TTL")
    public int getIndexRedisTtl() {
        return indexRedisTtl;
    }

    @Bean
    @Named("JWT_REDIS_TTL")
    public int getJwtRedisTtl() {
        return jwtTtl;
    }

    @Bean
    @Named("SCHEMA_REDIS_TTL")
    public int getSchemaRedisTtl() {
        return schemaTtl;
    }

    @Bean
    @Named("RECORDS_REDIS_TTL")
    public int getRecordsRedisTtl() {
        return recordsTtl;
    }

    @Bean
    @Named("REDIS_HOST")
    public String redisHost(SecretClient kv) {
        return KeyVaultFacade.getSecretWithValidation(kv, "redis-hostname");
    }

    @Bean
    @Named("REDIS_PASSWORD")
    public String redisPassword(SecretClient kv) {
        return KeyVaultFacade.getSecretWithValidation(kv, "redis-password");
    }
}
