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

package org.opengroup.osdu.indexer.cache;

import org.opengroup.osdu.core.common.provider.interfaces.IRequestInfo;
import org.opengroup.osdu.indexer.model.indexproperty.PropertyConfigurations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import javax.inject.Inject;

@Component
@RequestScope
public class PartitionSafePropertyConfigurationsCache {
    @Inject
    private IPropertyConfigurationsCache cache;
    @Autowired
    private IRequestInfo requestInfo;

    public void put(String s, PropertyConfigurations o) {
        this.cache.put(cacheKey(s), o);
    }

    public PropertyConfigurations get(String s) {
        return this.cache.get(cacheKey(s));
    }

    public void delete(String s) {
        this.cache.delete(cacheKey(s));
    }

    public void clearAll() {
        this.cache.clearAll();
    }

    private String cacheKey(String s) {
        return this.requestInfo.getPartitionId() + "-" + s;
    }
}
