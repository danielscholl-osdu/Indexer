package org.opengroup.osdu.indexer.cache;

import org.opengroup.osdu.core.common.cache.VmCache;
import org.opengroup.osdu.indexer.model.Constants;
import org.opengroup.osdu.indexer.model.indexproperty.PropertyConfigurations;
import org.springframework.stereotype.Component;

@Component
public class PropertyConfigurationsCacheVmImpl implements IPropertyConfigurationsCache {
    private VmCache<String, PropertyConfigurations> cache;

    public PropertyConfigurationsCacheVmImpl() {
        cache = new VmCache<>(Constants.SPEC_CACHE_EXPIRATION, Constants.SPEC_MAX_CACHE_SIZE);
    }

    @Override
    public void put(String s, PropertyConfigurations o) {
        this.cache.put(s, o);
    }

    @Override
    public PropertyConfigurations get(String s) {
        return this.cache.get(s);
    }

    @Override
    public void delete(String s) {
        this.cache.delete(s);
    }

    @Override
    public void clearAll() {
        this.cache.clearAll();
    }
}
