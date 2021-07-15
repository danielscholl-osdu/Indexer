// Copyright © Amazon Web Services
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

package org.opengroup.osdu.indexer.aws.persistence;

import org.opengroup.osdu.core.aws.secrets.SecretsManager;
import org.opengroup.osdu.core.aws.ssm.ParameterStorePropertySource;
import org.opengroup.osdu.core.aws.ssm.SSMConfig;
import org.opengroup.osdu.core.common.model.search.ClusterSettings;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.IElasticRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class ElasticRepositoryImpl implements IElasticRepository {

    @Value("${aws.es.host}")
    String host;

    @Value("${aws.es.port}")
    int port;

    @Value("${aws.es.isHttps}")
    boolean isHttps;

    @Value("${aws.es.username}")
    String username;

    @Value("${aws.es.password}")
    String password;

    String usernameAndPassword;


    @Value("${aws.elasticsearch.port}")
    String portParameter;

    @Value("${aws.elasticsearch.host}")
    String hostParameter;

    @Value("${aws.elasticsearch.credentials.secret}")
    String elasticCredentialsSecret;

    @Value("${aws.region}")
    private String amazonRegion;

    @Value("${aws.ssm}")
    String ssmEnabledString;

    private ParameterStorePropertySource ssm;

    @PostConstruct
    private void postConstruct() {
        if( Boolean.parseBoolean(ssmEnabledString)) {
            SSMConfig ssmConfig = new SSMConfig();
            ssm = ssmConfig.amazonSSM();
            host = ssm.getProperty(hostParameter).toString();
            port = Integer.parseInt(ssm.getProperty(portParameter).toString());            

        }
        SecretsManager sm = new SecretsManager();
        username = sm.getSecret(elasticCredentialsSecret,amazonRegion,"username");
        password = sm.getSecret(elasticCredentialsSecret,amazonRegion,"password");
        //elastic expects username:password format
        usernameAndPassword = String.format("%s:%s", username, password);
    }

    @Override
    public ClusterSettings getElasticClusterSettings(TenantInfo tenantInfo) {
        ClusterSettings settings = new ClusterSettings(host, port, usernameAndPassword);
        
        if (!isHttps) {
            settings.setHttps(false);
            settings.setTls(false);
        }

        return settings;
    }
}
