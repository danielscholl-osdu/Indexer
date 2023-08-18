package org.opengroup.osdu.indexer.cache.partitionsafe;

import org.opengroup.osdu.core.common.cache.VmCache;
import org.springframework.stereotype.Component;

@Component
public class FeatureFlagCache extends AbstractPartitionSafeCache<String, Boolean> {
    private VmCache<String, Boolean> cache;

    public FeatureFlagCache() {
        cache = new VmCache<>(300, 1000);
    }

    @Override
    public void put(String s, Boolean o) {
        this.cache.put(cacheKey(s), o);
    }

    @Override
    public Boolean get(String s) {
        return this.cache.get(cacheKey(s));
    }

    @Override
    public void delete(String s) {
        this.cache.delete(cacheKey(s));
    }

    @Override
    public void clearAll() {
        this.cache.clearAll();
    }
}
