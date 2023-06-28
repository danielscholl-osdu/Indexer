package org.opengroup.osdu.indexer.azure.cache;

import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.indexer.cache.IRecordChangeInfoCache;
import org.opengroup.osdu.indexer.model.RecordChangeInfo;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.inject.Named;

@Component
@ConditionalOnProperty(value = "runtime.env.local", havingValue = "false", matchIfMissing = true)
public class RecordChangeInfoRedisCache extends RedisCache<String, RecordChangeInfo> implements IRecordChangeInfoCache {
    public RecordChangeInfoRedisCache(final @Named("REDIS_HOST") String host,
                                   final @Named("REDIS_PORT") int port,
                                   final @Named("REDIS_PASSWORD") String password,
                                   final @Named("RECORDS_REDIS_TTL") int timeout,
                                   @Qualifier("") @Value("${redis.database}") final int database) {
        super(host, port, password, timeout, database, String.class, RecordChangeInfo.class);
    }
}
