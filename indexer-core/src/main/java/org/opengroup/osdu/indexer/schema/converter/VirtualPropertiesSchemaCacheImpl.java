package org.opengroup.osdu.indexer.schema.converter;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import org.opengroup.osdu.indexer.provider.interfaces.ISchemaCache;
import org.opengroup.osdu.indexer.schema.converter.interfaces.IVirtualPropertiesSchemaCache;
import org.opengroup.osdu.indexer.schema.converter.tags.VirtualProperties;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class VirtualPropertiesSchemaCacheImpl implements IVirtualPropertiesSchemaCache<String, VirtualProperties> {
    private static final String VIRTUAL_PROPERTIES_SCHEMA = "_virtual_properties";
    private final Gson gson = new Gson();

    @Inject
    private ISchemaCache schemaCache;

    @Override
    public void put(String s, VirtualProperties o) {
        if(Strings.isNullOrEmpty(s) || o == null)
            return;

        String key = getCacheKey(s);
        schemaCache.put(key, gson.toJson(o));
    }

    @Override
    public VirtualProperties get(String s) {
        if(Strings.isNullOrEmpty(s))
            return null;

        String key = getCacheKey(s);
        String schema = (String)schemaCache.get(key);
        if(!Strings.isNullOrEmpty(schema)) {
            VirtualProperties schemaObj = gson.fromJson(schema, VirtualProperties.class);
            return schemaObj;
        }

        return null;
    }

    @Override
    public void delete(String s) {
        if(Strings.isNullOrEmpty(s))
            return;

        String key = getCacheKey(s);
        String schema = (String)schemaCache.get(key);
        if(!Strings.isNullOrEmpty(schema)) {
            schemaCache.delete(key);
        }
    }

    @Override
    public void clearAll() {
        schemaCache.clearAll();
    }

    private String getCacheKey(String s) {
        return s + VIRTUAL_PROPERTIES_SCHEMA;
    }
}
