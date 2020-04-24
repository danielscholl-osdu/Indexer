// Copyright 2017-2019, Schlumberger
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

package org.opengroup.osdu.indexer.ibm.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.indexer.ibm.di.TenantInfoService;
import org.opengroup.osdu.indexer.ibm.util.IHeadersInfo;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class TenantInfoServiceTest {

    private static final String HEADER_NAME = "ANY_HEADER";
    private static final String HEADER_VALUE = "ANY_VALUE";

    @Mock
    private ITenantFactory tenantFactory;
    @Mock
    private IHeadersInfo headersInfo;
    
    @InjectMocks
    private TenantInfoService sut;

    @Mock
    private TenantInfo info;

    @Mock
    private HttpHeaders httpHeaders;

    @InjectMocks
    private DpsHeaders HEADERS;

    @Before
    public void setup() {
        HEADERS.put(HEADER_NAME, HEADER_VALUE);
    }

    @Ignore
    @Test
    public void should_return_validTenant_given_validAccountId() {

        when(this.info.getName()).thenReturn("tenant1");
        when(tenantFactory.getTenantInfo("tenant1")).thenReturn(info);

        when(this.headersInfo.getHeaders()).thenReturn(HEADERS);

        when(this.headersInfo.getPartitionId()).thenReturn("tenant1");

        when(this.sut.getTenantInfo()).thenReturn(info);

        assertNotNull(this.sut.getTenantInfo());
        assertEquals("tenant1", this.sut.getTenantInfo().getName());
    }

    @Ignore
    @Test(expected = AppException.class)
    public void should_throwException_given_invalidAccountId() {

        when(this.info.getName()).thenReturn("tenant2");
        when(tenantFactory.getTenantInfo("tenant1")).thenReturn(null);

        when(this.sut.getTenantInfo()).thenReturn(info);

        assertNotNull(this.sut.getTenantInfo());
    }
}