package org.opengroup.osdu.indexer.cache;

import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.core.common.provider.interfaces.IIndexCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class IndexCache implements IIndexCache<String, Boolean>, AutoCloseable {
    private RedisCache<String, Boolean> cache;

    public IndexCache(@Value("${REDIS_SEARCH_HOST}") final String REDIS_SEARCH_HOST,
                      @Value("${REDIS_SEARCH_PORT}") final String REDIS_SEARCH_PORT,
                      @Value("${INDEX_CACHE_EXPIRATION}") final String INDEX_CACHE_EXPIRATION) {
        cache = new RedisCache<>(REDIS_SEARCH_HOST, Integer.parseInt(REDIS_SEARCH_PORT),
                Integer.parseInt(INDEX_CACHE_EXPIRATION) * 60, String.class, Boolean.class);
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
