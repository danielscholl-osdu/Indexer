package org.opengroup.osdu.indexer.azure.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.azure.util.AzureServicePrincipleTokenService;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.junit4.SpringRunner;
import javax.inject.Inject;
import java.util.*;
import static junit.framework.TestCase.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ServiceAccountJwtClientImplTest {

    private String partitionId="opendes";
    private static String authorizationToken = "Bearer authorizationToken";

    @Mock
    private ITenantFactory tenantInfoServiceProvider;

    @Mock
    private DpsHeaders dpsHeaders;

    @Mock
    private JaxRsDpsLog log;

    @Mock
    private AzureServicePrincipleTokenService tokenService;

    @InjectMocks
    ServiceAccountJwtClientImpl sut;

    @Test
    public void should_invoke_methodsWithRightArguments_andReturnAuthToken_when_getIdToken_isCalled() {
        TenantInfo tenant=new TenantInfo();
        when(tenantInfoServiceProvider.getTenantInfo(partitionId)).thenReturn(tenant);
        when(tokenService.getAuthorizationToken()).thenReturn(authorizationToken);

        String authToken=sut.getIdToken(partitionId);

        verify(tenantInfoServiceProvider,times(1)).getTenantInfo(partitionId);
        verify(tokenService,times(1)).getAuthorizationToken();
        verify(dpsHeaders,times(1)).put(DpsHeaders.USER_EMAIL, tenant.getServiceAccount());
        assertEquals(authorizationToken, authToken);
    }

    @Test(expected = AppException.class)
    public void should_throw_appException_when_getIdToken_isCalled_with_tenantNull() {
        when(tenantInfoServiceProvider.getTenantInfo(partitionId)).thenReturn(null);

        sut.getIdToken(partitionId);

        Assert.fail("Invalid tenant Name from azure");
    }
}
