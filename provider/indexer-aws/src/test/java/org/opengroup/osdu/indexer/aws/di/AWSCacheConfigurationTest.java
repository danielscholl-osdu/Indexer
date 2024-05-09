package org.opengroup.osdu.indexer.aws.di;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.aws.ssm.K8sLocalParameterProvider;
import org.opengroup.osdu.core.aws.ssm.K8sParameterNotFoundException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AWSCacheConfigurationTest {

    private final String CREDENTIAL_KEY = "token";
    private final String CREDENTIAL_VALUE = "dummy_token";

    @Mock
    K8sLocalParameterProvider provider;
    @Test
    public void test_init_without_credentials() throws JsonProcessingException, K8sParameterNotFoundException {
        when(provider.getLocalMode()).thenReturn(true);
        when(provider.getParameterAsStringOrDefault("CACHE_CLUSTER_PORT", null)).thenReturn("12345");
        when(provider.getParameterAsStringOrDefault("CACHE_CLUSTER_ENDPOINT", null)).thenReturn("localhost");
        when(provider.getCredentialsAsMap("CACHE_CLUSTER_KEY")).thenReturn(null);

        AWSCacheConfiguration cacheConfig = new AWSCacheConfiguration(provider);
        assertEquals(3600, cacheConfig.getCacheExpireTimeInSeconds());
    }

    @Test
    public void test_init_with_credentials() throws JsonProcessingException, K8sParameterNotFoundException {
        when(provider.getLocalMode()).thenReturn(true);
        when(provider.getParameterAsStringOrDefault("CACHE_CLUSTER_PORT", null)).thenReturn("12345");
        when(provider.getParameterAsStringOrDefault("CACHE_CLUSTER_ENDPOINT", null)).thenReturn("localhost");
        Map<String, String> credentials = Map.of(CREDENTIAL_KEY, CREDENTIAL_VALUE);
        when(provider.getCredentialsAsMap("CACHE_CLUSTER_KEY")).thenReturn(credentials);

        AWSCacheConfiguration cacheConfig = new AWSCacheConfiguration(provider);
        assertEquals(3600, cacheConfig.getCacheExpireTimeInSeconds());
        assertEquals(CREDENTIAL_VALUE, cacheConfig.getCacheClusterKey());
    }
}
