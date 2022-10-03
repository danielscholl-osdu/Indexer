package org.opengroup.osdu.indexer.util.geo.decimator;

import org.opengroup.osdu.core.common.cache.VmCache;
import org.springframework.stereotype.Component;

@Component
public class DecimationSettingCache extends VmCache<String, Boolean> {
    public DecimationSettingCache() {
        super(300, 1000);
    }

    public boolean containsKey(final String key) {
        return this.get(key) != null;
    }
}
