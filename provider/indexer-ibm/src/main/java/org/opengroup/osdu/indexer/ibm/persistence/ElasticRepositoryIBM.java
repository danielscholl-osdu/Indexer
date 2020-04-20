// Copyright 2020 IBM Corp. All Rights Reserved.
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

package org.opengroup.osdu.indexer.ibm.persistence;

import javax.inject.Inject;

import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.search.ClusterSettings;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.IElasticRepository;
import org.opengroup.osdu.core.common.search.Preconditions;
import org.opengroup.osdu.indexer.ibm.model.ElasticSettingSchema;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ElasticRepositoryIBM implements IElasticRepository {

    @Value("${ELASTIC_DATASTORE_KIND}")
    private String ELASTIC_DATASTORE_KIND;

    @Value("${ELASTIC_DATASTORE_ID}")
    private String ELASTIC_DATASTORE_ID;

    @Inject
    private ISchemaRepository schemaRepository;

    @Value("${ELASTIC_HOST}")
    private String ELASTIC_HOST;
    @Value("${ELASTIC_PORT:443}")
    private String ELASTIC_PORT;
    @Value("${ELASTIC_USER_PASSWORD}")
    private String ELASTIC_USER_PASSWORD;

    @Inject
    private JaxRsDpsLog log;
    
    @Override
    public ClusterSettings getElasticClusterSettings(TenantInfo tenantInfo) {

        if(tenantInfo == null)
            throw  new AppException(HttpStatus.SC_NOT_FOUND, "TenantInfo is null", "");

        String settingId = tenantInfo.getName().concat("-").concat(ELASTIC_DATASTORE_ID);
        ElasticSettingSchema schema = this.schemaRepository.get(settingId);

        if (schema == null) {
        	// if creds not in the db, use default from env
        	log.warning(settingId + " credentials not found at database.");
        	return new ClusterSettings(ELASTIC_HOST, Integer.parseInt(ELASTIC_PORT), ELASTIC_USER_PASSWORD, true, false);
            //throw new AppException(HttpStatus.SC_NOT_FOUND, "Elastic setting not found", "The requested cluster setting was not found in CosmosDB.", String.format("Elastic setting with key: '%s' does not exist in CosmostDB.", ELASTIC_DATASTORE_KIND));
        }

        String host = schema.getHost();
        String portString = schema.getPort();
        String usernameAndPassword = schema.getUsernameAndPassword();

        Preconditions.checkNotNullOrEmpty(host, "host cannot be null");
        Preconditions.checkNotNullOrEmpty(portString, "port cannot be null");
        Preconditions.checkNotNullOrEmpty(usernameAndPassword, "configuration cannot be null");

        int port = Integer.parseInt(portString);

        return new ClusterSettings(host, port, usernameAndPassword, schema.isHttps(), schema.isHttps());
        
    }
}
