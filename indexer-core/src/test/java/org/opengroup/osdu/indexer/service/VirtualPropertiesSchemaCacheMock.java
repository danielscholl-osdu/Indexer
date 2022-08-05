package org.opengroup.osdu.indexer.service;

import org.opengroup.osdu.indexer.schema.converter.interfaces.IVirtualPropertiesSchemaCache;
import org.opengroup.osdu.indexer.schema.converter.tags.VirtualProperties;

import java.util.HashMap;
import java.util.Map;

public class VirtualPropertiesSchemaCacheMock implements IVirtualPropertiesSchemaCache<String, VirtualProperties> {
    private Map<String, VirtualProperties> cache = new HashMap<>();

    @Override
    public void put(String s, VirtualProperties o) {
        cache.put(s, o);
    }

    @Override
    public VirtualProperties get(String s) {
        if(cache.containsKey(s))
            return cache.get(s);
        return null;
    }

    @Override
    public void delete(String s) {
        cache.remove(s);
    }

    @Override
    public void clearAll() {
        cache.clear();
    }
}
