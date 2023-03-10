package org.opengroup.osdu.indexer.cache;

import org.opengroup.osdu.indexer.provider.interfaces.ISchemaCache;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import javax.inject.Inject;

@Component
@RequestScope
public class PartitionSafeFlattenedSchemaCache extends AbstractPartitionSafeCache<String, String> {
    private static final String FLATTENED_SCHEMA = "_flattened";
    @Inject
    private ISchemaCache schemaCache;

    @Override
    public void put(String s, String o) {
        this.schemaCache.put(getKey(s), o);
    }

    @Override
    public String get(String s) {
        return (String)this.schemaCache.get(getKey(s));
    }

    @Override
    public void delete(String s) {
        this.schemaCache.delete(getKey(s));
    }

    @Override
    public void clearAll() {
        this.schemaCache.clearAll();
    }

    private String getKey(String s) {
        return cacheKey(s) + FLATTENED_SCHEMA;
    }
}
