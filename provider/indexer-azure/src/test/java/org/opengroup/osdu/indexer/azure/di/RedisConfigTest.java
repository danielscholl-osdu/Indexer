package org.opengroup.osdu.indexer.azure.di;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Value;
import static org.junit.jupiter.api.Assertions.*;

public class RedisConfigTest {
    @Spy
    @Value("5000")
    private int port;

    @Mock
    @Value("4000")
    public int indexRedisTtl;

    @Mock
    @Value("3000")
    public int jwtTtl;

    @Mock
    @Value("2000")
    public int schemaTtl;

    @InjectMocks
    public RedisConfig sut = new RedisConfig();

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
}
