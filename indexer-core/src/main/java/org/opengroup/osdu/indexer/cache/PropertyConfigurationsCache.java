package org.opengroup.osdu.indexer.cache;

import org.opengroup.osdu.core.common.cache.VmCache;
import org.opengroup.osdu.indexer.model.indexproperty.PropertyConfigurations;
import org.springframework.stereotype.Component;

@Component
public class PropertyConfigurationsCache extends VmCache<String, PropertyConfigurations> {
    public PropertyConfigurationsCache() {
        super(600, 1000);
    }

    public boolean containsKey(final String key) {
        return this.get(key) != null;
    }
}
