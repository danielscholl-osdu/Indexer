// Copyright © Microsoft Corporation
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

package org.opengroup.osdu.indexer.azure.di;

import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import javax.inject.Inject;
import java.util.*;

@Component
@RequestScope
public class TenantFactoryImpl implements ITenantFactory {
    @Inject
    private CosmosDBTenantInfo db;

    private Map<String, TenantInfo> tenants;

    public boolean exists(String tenantName)
    {
        if (this.tenants == null)
            initTenants();
        return this.tenants.containsKey(tenantName);
    }

    public TenantInfo getTenantInfo(String tenantName) {
        if (this.tenants == null)
            initTenants();
        return this.tenants.get(tenantName);
    }

    public Collection<TenantInfo> listTenantInfo() {
        if (this.tenants == null)
            initTenants();
        return this.tenants.values();
    }

    public <V> ICache<String, V> createCache(String tenantName, String host, int port, int expireTimeSeconds, Class<V> classOfV)
    {
        return null;
    }

    public void flushCache() {}

    private void initTenants() {
        this.tenants = new HashMap<>();

        db.findAll().forEach(doc -> {
            TenantInfo ti = new TenantInfo();
            String tenantName = doc.getId();
            ti.setName(tenantName);
            //set serviceprincipalAppId in Azure side instead of ServiceAccount in Gcp
            ti.setServiceAccount(doc.getServiceprincipalAppId());
            this.tenants.put(tenantName, ti) ;
        });
    }
}
