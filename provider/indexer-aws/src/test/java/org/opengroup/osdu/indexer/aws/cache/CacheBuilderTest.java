/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.indexer.aws.cache;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.aws.cache.DummyCache;
import org.opengroup.osdu.core.aws.ssm.K8sLocalParameterProvider;
import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.core.common.cache.VmCache;
import org.opengroup.osdu.core.common.provider.interfaces.IIndexCache;
import org.opengroup.osdu.indexer.aws.di.AWSCacheConfiguration;
import org.opengroup.osdu.indexer.provider.interfaces.ISchemaCache;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CacheBuilderTest {

    @Mock
    AWSCacheConfiguration config;
    private final String s = "s";
    private final String so = "o";
    private final boolean o = true;

    @Test
    public void test_indexDummyCache() throws Exception {
        CacheBuilder cb = new CacheBuilder();
        when(config.isLocalMode()).thenReturn(true);
        DummyCache<String, Boolean> vmCache = cb.createIndexDummyCache();
        IndexCacheImpl cacheImpl = cb.indexInitCache(vmCache, config);
        cacheImpl.put(s, o);
        // Dummy cache doesn't really cache, so should return null
        assertNull(cacheImpl.get(s));
        cacheImpl.delete(s);
        assertNull(cacheImpl.get(s));
        cacheImpl.clearAll();
        cacheImpl.close();
    }

    @Test
    public void test_schemaDummyCache() throws Exception {
        CacheBuilder cb = new CacheBuilder();
        when(config.isLocalMode()).thenReturn(true);
        DummyCache<String, String> vmCache = cb.createSchemaDummyCache();
        SchemaCacheImpl cacheImpl = cb.schemaInitCache(vmCache, config);
        cacheImpl.put(s, so);
        // Dummy cache doesn't really cache, so should return null
        assertNull(cacheImpl.get(s));
        cacheImpl.delete(s);
        assertNull(cacheImpl.get(s));
        cacheImpl.clearAll();
        cacheImpl.close();
    }

    @Test
    public void test_indexVMCache() throws Exception {
        CacheBuilder cb = new CacheBuilder();
        when(config.isLocalMode()).thenReturn(true);
        when(config.getCacheExpireTimeInSeconds()).thenReturn(3600);
        VmCache<String, Boolean> vmCache = cb.createIndexVMCache(config);
        IndexCacheImpl cacheImpl = cb.indexInitCache(vmCache, config);
        cacheImpl.put(s, o);
        assertEquals(o, cacheImpl.get(s));
        cacheImpl.delete(s);
        assertNull(cacheImpl.get(s));
        cacheImpl.clearAll();
        cacheImpl.close();
    }

    @Test
    public void test_schemaVMCache() throws Exception {
        CacheBuilder cb = new CacheBuilder();
        when(config.isLocalMode()).thenReturn(true);
        when(config.getCacheExpireTimeInSeconds()).thenReturn(3600);
        VmCache<String, String> vmCache = cb.createSchemaVMCache(config);
        SchemaCacheImpl cacheImpl = cb.schemaInitCache(vmCache, config);
        cacheImpl.put(s, so);
        assertEquals(so, cacheImpl.get(s));
        cacheImpl.delete(s);
        assertNull(cacheImpl.get(s));
        cacheImpl.clearAll();
        cacheImpl.close();
    }
}
