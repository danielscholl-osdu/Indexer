/*
 * Copyright © Schlumberger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.indexer.util.geo.decimator;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.partition.*;
import org.opengroup.osdu.core.common.util.IServiceAccountJwtClient;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(SpringRunner.class)
public class GeoShapeDecimationSettingTest {
    private static final String PROPERTY_NAME =  "indexer-decimation-enabled";

    @InjectMocks
    private GeoShapeDecimationSetting sut;

    @Mock
    private DecimationSettingCache cache;

    @Mock
    private JaxRsDpsLog logger;

    @Mock
    private DpsHeaders headers;

    @Mock
    private IPartitionFactory factory;

    @Mock
    private IServiceAccountJwtClient tokenService;

    @Mock
    IPartitionProvider partitionProvider;

    @Before
    public void setup() {
        when(this.headers.getPartitionId()).thenReturn("dataPartitionId");
        when(this.headers.getHeaders()).thenReturn(new HashMap());
        when(this.factory.create(any())).thenReturn(partitionProvider);
        when(this.tokenService.getIdToken(anyString())).thenReturn("token");
    }

    @Test
    public void isDecimationEnabled_return_true() throws PartitionException {
        PartitionInfo partitionInfo = new PartitionInfo();
        Property property = new Property();
        property.setSensitive(false);
        property.setValue("true");
        partitionInfo.getProperties().put(PROPERTY_NAME, property);
        when(this.partitionProvider.get(anyString())).thenReturn(partitionInfo);
        boolean enabled = sut.isDecimationEnabled();
        Assert.assertTrue(enabled);
    }

    @Test
    public void isDecimationEnabled_return_false_when_property_set_to_false() throws PartitionException {
        PartitionInfo partitionInfo = new PartitionInfo();
        Property property = new Property();
        property.setSensitive(false);
        property.setValue("false");
        partitionInfo.getProperties().put(PROPERTY_NAME, property);
        when(this.partitionProvider.get(anyString())).thenReturn(partitionInfo);
        boolean enabled = sut.isDecimationEnabled();
        Assert.assertFalse(enabled);
    }

    @Test
    public void isDecimationEnabled_return_true_when_property_does_not_exist() throws PartitionException {
        // The feature flag is enabled by default
        PartitionInfo partitionInfo = new PartitionInfo();
        when(this.partitionProvider.get(anyString())).thenReturn(partitionInfo);
        boolean enabled = sut.isDecimationEnabled();
        Assert.assertTrue(enabled);
    }

    @Test
    public void isDecimationEnabled_return_true_when_partitionProvider_throws_exception() throws PartitionException {
        // The feature flag is enabled by default
        when(this.partitionProvider.get(anyString())).thenThrow(PartitionException.class);
        boolean enabled = sut.isDecimationEnabled();
        Assert.assertTrue(enabled);
    }

}
