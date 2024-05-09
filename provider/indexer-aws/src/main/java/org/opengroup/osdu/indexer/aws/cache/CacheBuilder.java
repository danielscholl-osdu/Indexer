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
import org.opengroup.osdu.core.aws.cache.DummyCache;
import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.core.common.cache.VmCache;
import org.opengroup.osdu.indexer.aws.di.AWSCacheConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;


/*
The way we build the caches are a bit over-complicated, but this is required so the cache is properly
recognized by the `CloudConnectedOuterServicesBuilder` class and listed in the `/info` API.
For this, there should exist a bean with the `RedisCache` type. But of course, we can't build this bean
on local mode without a connection to the Elasticache server. That's why conditional bean injection is required.
 */
@Component
@RequiredArgsConstructor
public class CacheBuilder {

    @Bean
    public IndexCacheImpl indexInitCache(@Qualifier("awsIndexCache") ICache<String, Boolean> awsIndexCache, AWSCacheConfiguration awsAppServiceConfig) {
        return new IndexCacheImpl(awsIndexCache, awsAppServiceConfig.isLocalMode());
    }

    @Bean
    public SchemaCacheImpl schemaInitCache(@Qualifier("awsSchemaCache") ICache<String, String> awsSchemaCache, AWSCacheConfiguration awsAppServiceConfig) {
        return new SchemaCacheImpl(awsSchemaCache, awsAppServiceConfig.isLocalMode());
    }

    @Bean("awsIndexCache")
    @Conditional(ShouldUseDummyCacheCondition.class)
    public DummyCache<String, Boolean> createIndexDummyCache() {
        return new DummyCache<>();
    }

    @Bean("awsSchemaCache")
    @Conditional(ShouldUseDummyCacheCondition.class)
    public DummyCache<String, String> createSchemaDummyCache() {
        return new DummyCache<>();
    }

    @Bean("awsIndexCache")
    @Conditional(ShouldUseVMCacheCondition.class)
    public VmCache<String, Boolean> createIndexVMCache(AWSCacheConfiguration awsAppServiceConfig) {
        return new VmCache<>(awsAppServiceConfig.getCacheExpireTimeInSeconds(), 10);
    }

    @Bean("awsSchemaCache")
    @Conditional(ShouldUseVMCacheCondition.class)
    public VmCache<String, String> createSchemaVMCache(AWSCacheConfiguration awsAppServiceConfig) {
        return new VmCache<>(awsAppServiceConfig.getCacheExpireTimeInSeconds(), 10);
    }

    @Bean("awsIndexCache")
    @Conditional(ShouldUseElasticacheCondition.class)
    public RedisCache<String, Boolean> createIndexElasticCache(AWSCacheConfiguration awsAppServiceConfig) {
        return new RedisCache<>(awsAppServiceConfig.getCacheClusterHost(),
                awsAppServiceConfig.getCacheClusterPort(),
                awsAppServiceConfig.getCacheClusterKey(),
                awsAppServiceConfig.getCacheExpireTimeInSeconds(),
                String.class,
                Boolean.class);
    }
    @Bean("awsSchemaCache")
    @Conditional(ShouldUseElasticacheCondition.class)
    public RedisCache<String, String> createSchemaElasticCache(AWSCacheConfiguration awsAppServiceConfig) {
        return new RedisCache<>(awsAppServiceConfig.getCacheClusterHost(),
                awsAppServiceConfig.getCacheClusterPort(),
                awsAppServiceConfig.getCacheClusterKey(),
                awsAppServiceConfig.getCacheExpireTimeInSeconds(),
                String.class,
                String.class);
    }
}
