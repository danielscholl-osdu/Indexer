package org.opengroup.osdu.indexer.azure.di;

import org.junit.jupiter.api.BeforeEach;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static reactor.core.publisher.Mono.when;

@RunWith(MockitoJUnitRunner.class)
public class TenantInfoServiceTest {

    @Mock
    private ITenantFactory tenantFactory;

    @Mock
    private DpsHeaders headers;

    @InjectMocks
    TenantInfoService sut;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(TenantInfoServiceTest.this);
    }

    @Test
    public void shouldReturnSetValue_when_getTenantInfo_isCalled() {
        TenantInfo tenantInfo = new TenantInfo();
        Mockito.when(headers.getPartitionId()).thenReturn("opendes");
        Mockito.when(tenantFactory.getTenantInfo("opendes")).thenReturn(tenantInfo);

        TenantInfo tenantInfoExpected = sut.getTenantInfo();

        assertEquals(tenantInfo, tenantInfoExpected);
    }

    @Test
    public void shouldReturnSetList_when_getAllTenantInfos_isCalled() {
        List<TenantInfo> tenantInfoArrayList = new ArrayList<>();
        Mockito.when(tenantFactory.listTenantInfo()).thenReturn(tenantInfoArrayList);

        List<TenantInfo> tenantInfoArrayListExpected = sut.getAllTenantInfos();

        assertEquals(tenantInfoArrayList, tenantInfoArrayListExpected);
    }
}
