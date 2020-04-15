package org.opengroup.osdu.indexer.cache;

import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.indexer.provider.interfaces.ISchemaCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SchemaCache implements ISchemaCache<String, String>, AutoCloseable {
    private RedisCache<String, String> cache;

    public SchemaCache(@Value("${REDIS_SEARCH_HOST}") final String REDIS_SEARCH_HOST,
                       @Value("${REDIS_SEARCH_PORT}") final String REDIS_SEARCH_PORT,
                       @Value("${SCHEMA_CACHE_EXPIRATION}") final String SCHEMA_CACHE_EXPIRATION) {
        cache = new RedisCache<>(REDIS_SEARCH_HOST, Integer.parseInt(REDIS_SEARCH_PORT),
                Integer.parseInt(SCHEMA_CACHE_EXPIRATION) * 60, String.class, String.class);
    }

    @Override
    public void close() throws Exception {
        this.cache.close();
    }

    @Override
    public void put(String s, String o) {
        this.cache.put(s, o);
    }

    @Override
    public String get(String s) {
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
