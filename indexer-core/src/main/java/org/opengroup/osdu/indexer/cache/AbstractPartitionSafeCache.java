package org.opengroup.osdu.indexer.cache;

import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.core.common.provider.interfaces.IRequestInfo;
import org.springframework.web.context.annotation.RequestScope;

import javax.inject.Inject;

@RequestScope
public abstract class AbstractPartitionSafeCache<K, V> implements ICache<K, V> {
    @Inject
    private IRequestInfo requestInfo;

    protected String cacheKey(String s) {
        return this.requestInfo.getPartitionId() + "-" + s;
    }
}
