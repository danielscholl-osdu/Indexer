package org.opengroup.osdu.indexer.cache.partitionsafe;

import org.opengroup.osdu.indexer.schema.converter.interfaces.IVirtualPropertiesSchemaCache;
import org.opengroup.osdu.indexer.schema.converter.tags.VirtualProperties;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;

@Component
public class VirtualPropertiesSchemaCache extends AbstractPartitionSafeCache<String, VirtualProperties> {
    @Inject
    private IVirtualPropertiesSchemaCache virtualPropertiesSchemaCache;

    @Override
    public void put(String s, VirtualProperties o) {
        this.virtualPropertiesSchemaCache.put(cacheKey(s), o);
    }

    @Override
    public VirtualProperties get(String s) {
        return this.virtualPropertiesSchemaCache.get(cacheKey(s));
    }

    @Override
    public void delete(String s) {
        this.virtualPropertiesSchemaCache.delete(cacheKey(s));
    }

    @Override
    public void clearAll() {
        this.virtualPropertiesSchemaCache.clearAll();
    }
}
