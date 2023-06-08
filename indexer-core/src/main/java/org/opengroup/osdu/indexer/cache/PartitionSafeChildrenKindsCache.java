package org.opengroup.osdu.indexer.cache;

import org.opengroup.osdu.indexer.model.indexproperty.ChildrenKinds;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import javax.inject.Inject;


@Component
@RequestScope
public class PartitionSafeChildrenKindsCache extends AbstractPartitionSafeCache<String, ChildrenKinds>{
    @Inject
    private IChildrenKindsCache cache;

    @Override
    public void put(String s, ChildrenKinds o) {
        this.cache.put(cacheKey(s), o);
    }

    @Override
    public ChildrenKinds get(String s) {
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
