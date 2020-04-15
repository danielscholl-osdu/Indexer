package org.opengroup.osdu.indexer.cache;

import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.core.common.model.search.IdToken;
import org.opengroup.osdu.core.common.provider.interfaces.IJwtCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtCache implements IJwtCache<String, IdToken>, AutoCloseable {
    RedisCache<String, IdToken> cache;

    // google service account id_token can be requested only for 1 hr
    private final static int EXPIRED_AFTER = 59;

    public JwtCache(@Value("${REDIS_SEARCH_HOST}") final String REDIS_SEARCH_HOST, @Value("${REDIS_SEARCH_PORT}") final String REDIS_SEARCH_PORT) {
        cache = new RedisCache<>(REDIS_SEARCH_HOST, Integer.parseInt(REDIS_SEARCH_PORT),
                EXPIRED_AFTER * 60, String.class, IdToken.class);
    }

    @Override
    public void close() throws Exception {
        this.cache.close();
    }

    @Override
    public void put(String s, IdToken o) {
        this.cache.put(s, o);
    }

    @Override
    public IdToken get(String s) {
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
