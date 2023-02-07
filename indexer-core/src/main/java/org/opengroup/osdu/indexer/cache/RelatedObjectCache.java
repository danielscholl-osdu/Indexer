package org.opengroup.osdu.indexer.cache;

import org.opengroup.osdu.core.common.cache.VmCache;
import org.opengroup.osdu.indexer.model.SearchRecord;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RelatedObjectCache extends VmCache<String, Map<String, Object>> {
    public RelatedObjectCache() {
        super(60, 1000);
    }
}
