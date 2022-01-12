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
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.opengroup.osdu.indexer.util.ElasticClientHandler;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

public class ClusterConfigurationServiceImpl implements IClusterConfigurationService {

    @Autowired
    private ElasticClientHandler elasticClientHandler;

    @Override
    public boolean updateClusterConfiguration() throws IOException {
        ClusterUpdateSettingsRequest request = new ClusterUpdateSettingsRequest();

        Settings persistentSettings =
                Settings.builder()
                        .put("action.auto_create_index", "false")
                        .build();
        request.persistentSettings(persistentSettings);
        request.timeout(TimeValue.timeValueMinutes(1));
        try (RestHighLevelClient client = this.elasticClientHandler.createRestClient()) {
            ClusterUpdateSettingsResponse response = client.cluster().putSettings(request, RequestOptions.DEFAULT);
            return response.isAcknowledged();
        }
    }
}
