package org.opengroup.osdu.indexer.cache;

import org.opengroup.osdu.core.common.cache.VmCache;
import org.opengroup.osdu.indexer.model.SearchRecord;
import org.springframework.stereotype.Component;

@Component
public class SearchRecordCache extends VmCache<String, SearchRecord> {
    public SearchRecordCache() {
        super(600, 1000);
    }

    public boolean containsKey(final String key) {
        return this.get(key) != null;
    }
}
