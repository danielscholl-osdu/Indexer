package org.opengroup.osdu.indexer.cache.partitionsafe;

import org.opengroup.osdu.core.common.model.storage.RecordData;
import org.opengroup.osdu.indexer.cache.interfaces.IRelatedObjectCache;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class RelatedObjectCache extends AbstractPartitionSafeCache<String, RecordData> {
    @Inject
    private IRelatedObjectCache cache;

    @Override
    public void put(String s, RecordData o) {
        this.cache.put(cacheKey(s), o);
    }

    @Override
    public RecordData get(String s) {
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
