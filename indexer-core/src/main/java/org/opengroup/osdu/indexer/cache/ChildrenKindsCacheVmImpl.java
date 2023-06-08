package org.opengroup.osdu.indexer.cache;

import org.opengroup.osdu.core.common.cache.VmCache;
import org.opengroup.osdu.indexer.model.Constants;
import org.opengroup.osdu.indexer.model.indexproperty.ChildrenKinds;
import org.springframework.stereotype.Component;

@Component
public class ChildrenKindsCacheVmImpl implements IChildrenKindsCache{
    private VmCache<String, ChildrenKinds> cache;

    public ChildrenKindsCacheVmImpl() {
        cache = new VmCache<>(Constants.SPEC_CACHE_EXPIRATION, Constants.SPEC_MAX_CACHE_SIZE);
    }

    @Override
    public void put(String s, ChildrenKinds o) {
        this.cache.put(s, o);
    }

    @Override
    public ChildrenKinds get(String s) {
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
