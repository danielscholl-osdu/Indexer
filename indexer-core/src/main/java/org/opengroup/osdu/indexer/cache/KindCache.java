package org.opengroup.osdu.indexer.cache;

import org.opengroup.osdu.core.common.cache.VmCache;
import org.springframework.stereotype.Component;

@Component
public class KindCache extends VmCache<String, String> {
    public KindCache() {
        super(600, 1000);
    }

    public boolean containsKey(final String key) {
        return this.get(key) != null;
    }
}
