package org.opengroup.osdu.indexer.cache;

import com.google.common.base.Strings;
import org.opengroup.osdu.core.common.provider.interfaces.IRequestInfo;
import org.opengroup.osdu.indexer.provider.interfaces.ISchemaCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import javax.inject.Inject;

@Component
@RequestScope
public class PartitionSafeSchemaCache {
    private static final String FLATTENED_SCHEMA = "_flattened";

    @Inject
    private ISchemaCache schemaCache;
    @Autowired
    private IRequestInfo requestInfo;

    public void putSchema(String s, String o) {
        this.schemaCache.put(cacheKey(s), o);
    }

    public String getSchema(String s) {
        return (String) this.schemaCache.get(cacheKey(s));
    }

    public void putFlattenedSchema(String s, String o) {
        this.schemaCache.put(flattenedCacheKey(s), o);
    }

    public String getFlattenedSchema(String s) {
        return (String) this.schemaCache.get(flattenedCacheKey(s));
    }

    public void delete(String s) {
        if (!Strings.isNullOrEmpty(this.getSchema(s))) {
            this.schemaCache.delete(cacheKey(s));
        }

        if (!Strings.isNullOrEmpty(this.getFlattenedSchema(s))) {
            this.schemaCache.delete(flattenedCacheKey(s));
        }
    }

    public void clearAll() {
        this.schemaCache.clearAll();
    }

    private String cacheKey(String s) {
        return this.requestInfo.getPartitionId() + "-" + s;
    }

    private String flattenedCacheKey(String s) {
        return this.requestInfo.getPartitionId() + "-" + s + FLATTENED_SCHEMA;
    }
}
