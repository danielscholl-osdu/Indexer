package org.opengroup.osdu.indexer.cache.partitionsafe;

import org.opengroup.osdu.indexer.cache.interfaces.IRecordChangeInfoCache;
import org.opengroup.osdu.indexer.model.RecordChangeInfo;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class RecordChangeInfoCache extends AbstractPartitionSafeCache<String, RecordChangeInfo> {
    @Inject
    private IRecordChangeInfoCache cache;

    @Override
    public void put(String s, RecordChangeInfo o) {
        this.cache.put(cacheKey(s), o);
    }

    @Override
    public RecordChangeInfo get(String s) {
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
