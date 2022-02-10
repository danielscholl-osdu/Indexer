// Copyright © Schlumberger
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

package org.opengroup.osdu.indexer.service;

import org.elasticsearch.action.admin.cluster.settings.ClusterUpdateSettingsRequest;
import org.elasticsearch.action.admin.cluster.settings.ClusterUpdateSettingsResponse;
import org.elasticsearch.client.ClusterClient;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opengroup.osdu.indexer.util.ElasticClientHandler;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

import static junit.framework.TestCase.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(SpringRunner.class)
@PrepareForTest({RestHighLevelClient.class, ClusterClient.class})
public class ClusterConfigurationServiceTest {

    @Mock
    private ElasticClientHandler elasticClientHandler;
    @InjectMocks
    private ClusterConfigurationServiceImpl sut;

    private RestHighLevelClient restHighLevelClient;
    private ClusterClient clusterClient;

    @Before
    public void setup() {
        initMocks(this);
        clusterClient = PowerMockito.mock(ClusterClient.class);
        restHighLevelClient = PowerMockito.mock(RestHighLevelClient.class);
    }

    @Test
    public void should_updateClusterConfiguration() throws IOException {
        ClusterUpdateSettingsResponse clusterUpdateSettingsResponse = mock(ClusterUpdateSettingsResponse.class);
        when(elasticClientHandler.createRestClient()).thenReturn(restHighLevelClient);
        when(clusterUpdateSettingsResponse.isAcknowledged()).thenReturn(true);
        doReturn(clusterClient).when(restHighLevelClient).cluster();
        doReturn(clusterUpdateSettingsResponse).when(clusterClient).putSettings(any(ClusterUpdateSettingsRequest.class), any(RequestOptions.class));

        boolean result = this.sut.updateClusterConfiguration();

        assertTrue(result);
    }
}
