package org.opengroup.osdu.indexer.cache;

import javax.inject.Inject;
import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.core.common.provider.interfaces.IKindsCache;
import org.opengroup.osdu.indexer.config.IndexerConfigurationProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class KindsCache implements IKindsCache<String, Set>, AutoCloseable {
    private RedisCache<String, Set> cache;

    @Inject
    public KindsCache(final IndexerConfigurationProperties properties) {
        cache = new RedisCache<>(properties.getRedisSearchHost(), Integer.parseInt(properties.getRedisSearchPort()),
                properties.getKindsCacheExpiration() * 60,
                properties.getKindsRedisDatabase(), String.class, Set.class);
    }

    @Override
    public void close() throws Exception {
        this.cache.close();
    }

    @Override
    public void put(String s, Set o) {
        this.cache.put(s, o);
    }

    @Override
    public Set get(String s) {
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
