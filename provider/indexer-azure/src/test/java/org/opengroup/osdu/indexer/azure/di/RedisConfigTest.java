package org.opengroup.osdu.indexer.azure.di;

import com.azure.security.keyvault.secrets.SecretClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.azure.KeyVaultFacade;
import org.springframework.beans.factory.annotation.Value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

@RunWith(MockitoJUnitRunner.class)
public class RedisConfigTest {
    @Value("5000")
    private int port;

    @Value("4000")
    public int indexRedisTtl;

    @Value("3000")
    public int jwtTtl;

    @Value("2000")
    public int schemaTtl;

    @Value("500")
    public int recordChangeInfoTtl;

    @Value("200")
    public int recordsTtl;

    @InjectMocks
    public RedisConfig sut;

    @Mock
    SecretClient secretClient;

    public void setup()
    {
        MockitoAnnotations.openMocks(RedisConfigTest.this);
    }

    @Test
    public void  shouldReturnPort_whenGetRedisPortCalled() {
        int port_val = sut.getRedisPort();
        assertEquals(port_val, port);
    }

    @Test
    public void shouldReturnSetValue_when_GetIndexRedisTtl_isCalled() {
        int indexRedisTtl_val = sut.getIndexRedisTtl();
        assertEquals(indexRedisTtl_val, indexRedisTtl);
    }

    @Test
    public void shouldReturnSetValue_when_GetJwtRedisTtl_isCalled() {
        int jwtTtl_val = sut.getJwtRedisTtl();
        assertEquals(jwtTtl_val, jwtTtl);
    }

    @Test
    public void shouldReturnSetValue_when_GetSchemaRedisTtl_isCalled() {
        int schemaTtl_val = sut.getSchemaRedisTtl();
        assertEquals(schemaTtl_val, schemaTtl);
    }

    @Test
    public void shouldReturnRedisHostFromKeyVault_when_redisHostIsCalled() {
        try (MockedStatic<KeyVaultFacade> keyVaultFacadeMockedStatic = mockStatic(KeyVaultFacade.class)) {
            // Mock the SecretClient
            SecretClient mockedSecretClient = mock(SecretClient.class);

            // Set up the static method call with arguments
            keyVaultFacadeMockedStatic.when(() -> KeyVaultFacade.getSecretWithValidation(mockedSecretClient, "redis-hostname"))
                    .thenReturn("host-name");


            String result = sut.redisHost(mockedSecretClient);

            // Verify the result
            assertEquals("host-name", result);
            //check that keyvault facade was called with our secret client for redis-hostname
            keyVaultFacadeMockedStatic.verify(()->KeyVaultFacade.getSecretWithValidation(mockedSecretClient, "redis-hostname"));
        }
    }

    @Test
    public void shouldReturnRedisPasswordFromKeyvault_when_redisHostIsCalled() {
        try (MockedStatic<KeyVaultFacade> keyVaultFacadeMockedStatic = mockStatic(KeyVaultFacade.class)) {
            SecretClient mockedSecretClient = mock(SecretClient.class);

            keyVaultFacadeMockedStatic.when(() -> KeyVaultFacade.getSecretWithValidation(mockedSecretClient, "redis-password"))
                    .thenReturn("password");

            String result = sut.redisPassword(mockedSecretClient);

            assertEquals("password", result);
            keyVaultFacadeMockedStatic.verify(()->KeyVaultFacade.getSecretWithValidation(mockedSecretClient, "redis-password"));
        }
    }

    @Test
    public void shouldReturnSetValue_when_GetRecordsRedisTtl_isCalled() {
        int indexRedisTtl_val = sut.getRecordsRedisTtl();
        assertEquals(indexRedisTtl_val, recordsTtl);
    }

    @Test
    public void shouldReturnSetValue_when_GetRecordsChangeInfoRedisTtl_isCalled() {
        int indexRedisTtl_val = sut.getRecordChangeInfoRedisTtl();
        assertEquals(indexRedisTtl_val, recordChangeInfoTtl);
    }
}
