package org.opengroup.osdu.indexer.cache;

import javax.inject.Inject;
import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.core.common.model.search.ClusterSettings;
import org.opengroup.osdu.core.common.provider.interfaces.IIndexCache;
import org.opengroup.osdu.indexer.config.IndexerConfigurationProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class IndexCache implements IIndexCache<String, Boolean>, AutoCloseable {
    private RedisCache<String, Boolean> cache;

    @Inject
    public IndexCache(final IndexerConfigurationProperties properties) {
        cache = new RedisCache<>(properties.getRedisSearchHost(), Integer.parseInt(properties.getRedisSearchPort()),
                properties.getElasticCacheExpiration() * 60, String.class, Boolean.class);
    }

    @Override
    public void close() throws Exception {
        this.cache.close();
    }

    @Override
    public void put(String s, Boolean o) {
        this.cache.put(s, o);
    }

    @Override
    public Boolean get(String s) {
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
