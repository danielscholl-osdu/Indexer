package org.opengroup.osdu.indexer.cache;

import org.opengroup.osdu.core.common.cache.VmCache;
import org.opengroup.osdu.indexer.model.Constants;
import org.opengroup.osdu.indexer.model.indexproperty.ParentChildRelatedObjectsSpec;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ParentChildRelatedObjectsSpecsCacheVmImpl implements IParentChildRelatedObjectsSpecsCache {
    private VmCache<String, List<ParentChildRelatedObjectsSpec>> cache;

    public ParentChildRelatedObjectsSpecsCacheVmImpl() {
        cache = new VmCache<>(Constants.SPEC_CACHE_EXPIRATION, Constants.SPEC_MAX_CACHE_SIZE);
    }

    @Override
    public void put(String s, List<ParentChildRelatedObjectsSpec> o) {
        this.cache.put(s, o);
    }

    @Override
    public List<ParentChildRelatedObjectsSpec> get(String s) {
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
