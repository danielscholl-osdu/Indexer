package org.opengroup.osdu.indexer.cache;

import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.core.common.provider.interfaces.IKindsCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class KindsCache implements IKindsCache<String, Set>, AutoCloseable {
    private RedisCache<String, Set> cache;

    public KindsCache(@Value("${REDIS_SEARCH_HOST}") final String REDIS_SEARCH_HOST,
                      @Value("${REDIS_SEARCH_PORT}") final String REDIS_SEARCH_PORT,
                      @Value("${KINDS_CACHE_EXPIRATION}") final String KINDS_CACHE_EXPIRATION,
                      @Value("${KINDS_REDIS_DATABASE}") final String KINDS_REDIS_DATABASE) {
        cache = new RedisCache<>(REDIS_SEARCH_HOST, Integer.parseInt(REDIS_SEARCH_PORT),
                Integer.parseInt(KINDS_CACHE_EXPIRATION) * 60,
                Integer.parseInt(KINDS_REDIS_DATABASE), String.class, Set.class);
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
