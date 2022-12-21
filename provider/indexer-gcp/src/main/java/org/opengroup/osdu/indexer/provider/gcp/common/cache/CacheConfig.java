/*
 *  Copyright 2020-2022 Google LLC
 *  Copyright 2020-2022 EPAM Systems, Inc
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

package org.opengroup.osdu.indexer.provider.gcp.common.cache;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.core.common.model.search.ClusterSettings;
import org.opengroup.osdu.core.common.provider.interfaces.IElasticCredentialsCache;
import org.opengroup.osdu.core.common.provider.interfaces.IIndexCache;
import org.opengroup.osdu.core.gcp.cache.RedisCacheBuilder;
import org.opengroup.osdu.indexer.provider.gcp.common.di.GcpConfigurationProperties;
import org.opengroup.osdu.indexer.provider.interfaces.ISchemaCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class CacheConfig {

    @Bean
    public ISchemaCache fieldTypeMappingCache(GcpConfigurationProperties appProperties) {
        RedisCacheBuilder<String, String> cacheBuilder = new RedisCacheBuilder<>();
        RedisCache<String, String> schemaCache = cacheBuilder.buildRedisCache(
            appProperties.getRedisSearchHost(),
            Integer.parseInt(appProperties.getRedisSearchPort()),
            appProperties.getRedisSearchPassword(),
            appProperties.getRedisSearchExpiration(),
            appProperties.getRedisSearchWithSsl(),
            String.class,
            String.class
        );
        return new SchemaCache(schemaCache);
    }

    @Bean
    public IElasticCredentialsCache<String, ClusterSettings> elasticCredentialsCache(GcpConfigurationProperties gcpAppServiceConfig) {
        RedisCacheBuilder<String, ClusterSettings> cacheBuilder = new RedisCacheBuilder<>();
        RedisCache<String, ClusterSettings> clusterSettingCache = cacheBuilder.buildRedisCache(
            gcpAppServiceConfig.getRedisSearchHost(),
            Integer.parseInt(gcpAppServiceConfig.getRedisSearchPort()),
            gcpAppServiceConfig.getRedisSearchPassword(),
            gcpAppServiceConfig.getRedisSearchExpiration(),
            gcpAppServiceConfig.getRedisSearchWithSsl(),
            String.class,
            ClusterSettings.class
        );
        return new ElasticCredentialsCache(clusterSettingCache);
    }

    @Bean
    public IIndexCache cursorCache(GcpConfigurationProperties gcpAppServiceConfig) {
        RedisCacheBuilder<String, Boolean> cacheBuilder = new RedisCacheBuilder<>();
        RedisCache<String, Boolean> indexCache = cacheBuilder.buildRedisCache(
            gcpAppServiceConfig.getRedisSearchHost(),
            Integer.parseInt(gcpAppServiceConfig.getRedisSearchPort()),
            gcpAppServiceConfig.getRedisSearchPassword(),
            gcpAppServiceConfig.getRedisSearchExpiration(),
            gcpAppServiceConfig.getRedisSearchWithSsl(),
            String.class,
            Boolean.class
        );
        return new IndexCache(indexCache);
    }
}
