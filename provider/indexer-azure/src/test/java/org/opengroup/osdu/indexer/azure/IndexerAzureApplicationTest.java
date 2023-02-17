package org.opengroup.osdu.indexer.azure;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.jupiter.api.Assertions.*;

@RunWith(MockitoJUnitRunner.class)
public class IndexerAzureApplicationTest {

    @Test
    public void shouldReturn_notNullInstance_when_creatingNewObject() {
        assertNotNull(new IndexerAzureApplication());
    }
}
