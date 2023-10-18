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

import org.opengroup.osdu.core.aws.ssm.K8sLocalParameterProvider;
import org.opengroup.osdu.core.common.cache.RedisCache;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class IndexCacheImplTest {


    private final String s = "s";
    private final boolean o = true;
    private final String password = "password";
    private final String endpoint = "CACHE_CLUSTER_ENDPOINT";
    private final String port = "6369";

    @Test
    public void local_VmCache_test() throws Exception{
        try (MockedConstruction<K8sLocalParameterProvider> provider = Mockito.mockConstruction(K8sLocalParameterProvider.class, (mockProvider, context) -> {
                                                                                                                when(mockProvider.getLocalMode()).thenReturn(true);
                                                                                                            })) {                       
            IndexCacheImpl cacheImpl = new IndexCacheImpl();                                                                
            cacheImpl.put(s, o);
            assertEquals(o, cacheImpl.get(s));
            cacheImpl.delete(s);
            assertNull(cacheImpl.get(s));
            cacheImpl.clearAll();                                                                                                                                                                                   
            cacheImpl.close();
        }
    }

    @Test
    public void non_local_Null_Credential_RedisCache_test() throws Exception{
        try (MockedConstruction<K8sLocalParameterProvider> provider = Mockito.mockConstruction(K8sLocalParameterProvider.class, (mockProvider, context) -> {
                                                                                                                when(mockProvider.getLocalMode()).thenReturn(false);
                                                                                                                when(mockProvider.getParameterAsStringOrDefault(eq("CACHE_CLUSTER_ENDPOINT"), any())).thenReturn(endpoint);
                                                                                                                when(mockProvider.getParameterAsStringOrDefault(eq("CACHE_CLUSTER_PORT"), any())).thenReturn(port);
                                                                                                                when(mockProvider.getCredentialsAsMap(eq("CACHE_CLUSTER_KEY"))).thenReturn(null);
                                                                                                            })) {                       
            try (MockedConstruction<RedisCache> cache = Mockito.mockConstruction(RedisCache.class, (mockCache, context) -> {
                                                                                                                doNothing().when(mockCache).put(s,o);
                                                                                                                when(mockCache.get(s)).thenReturn(o);
                                                                                                                doNothing().when(mockCache).delete(s);
                                                                                                                doNothing().when(mockCache).clearAll();
                                                                                                                doNothing().when(mockCache).close();
                                                                                                            })) {   
                IndexCacheImpl cacheImpl = new IndexCacheImpl();
                cacheImpl.put(s, o);
                assertEquals(o, cacheImpl.get(s));
                cacheImpl.delete(s);
                cacheImpl.clearAll();                                                                                                                                                                                   
                cacheImpl.close();
            }
        }
    }

    @Test
    public void non_Local_notNull_Credential_RedisCache_test() throws Exception{

        Map<String, String> map = new HashMap<String, String>();
        map.put("token", password);

        try (MockedConstruction<K8sLocalParameterProvider> provider = Mockito.mockConstruction(K8sLocalParameterProvider.class, (mockProvider, context) -> {
                                                                                                                when(mockProvider.getLocalMode()).thenReturn(false);
                                                                                                                when(mockProvider.getParameterAsStringOrDefault(eq("CACHE_CLUSTER_ENDPOINT"), any())).thenReturn(endpoint);
                                                                                                                when(mockProvider.getParameterAsStringOrDefault(eq("CACHE_CLUSTER_PORT"), any())).thenReturn(port);
                                                                                                                when(mockProvider.getCredentialsAsMap(eq("CACHE_CLUSTER_KEY"))).thenReturn(map);
                                                                                                            })) {                       
            try (MockedConstruction<RedisCache> cache = Mockito.mockConstruction(RedisCache.class, (mockCache, context) -> {
                                                                                                                doNothing().when(mockCache).put(s,o);
                                                                                                                when(mockCache.get(s)).thenReturn(o);
                                                                                                                doNothing().when(mockCache).delete(s);
                                                                                                                doNothing().when(mockCache).clearAll();
                                                                                                                doNothing().when(mockCache).close();
                                                                                                            })) {   
                IndexCacheImpl cacheImpl = new IndexCacheImpl();
                cacheImpl.put(s, o);
                assertEquals(o, cacheImpl.get(s));
                cacheImpl.delete(s);
                cacheImpl.clearAll();                                                                                                                                                                                   
                cacheImpl.close();
            }
        }
    }

}
