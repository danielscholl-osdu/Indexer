package org.opengroup.osdu.indexer.schema.converter;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import org.opengroup.osdu.indexer.provider.interfaces.ISchemaCache;
import org.opengroup.osdu.indexer.schema.converter.interfaces.IVirtualPropertiesSchemaCache;
import org.opengroup.osdu.indexer.schema.converter.tags.VirtualProperties;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@Component
public class VirtualPropertiesSchemaCacheImpl implements IVirtualPropertiesSchemaCache<String, VirtualProperties> {
    private static final String VIRTUAL_PROPERTIES_SCHEMA = "_virtual_properties";
    private final Gson gson = new Gson();

    Map<String, VirtualProperties> virtualPropertiesSchema = new HashMap<>();

    @Inject
    ISchemaCache schemaCache;

    @Override
    public void put(String s, VirtualProperties o) {
        String key = getCacheKey(s);
        virtualPropertiesSchema.put(key, o);
        schemaCache.put(key, gson.toJson(o));
    }

    @Override
    public VirtualProperties get(String s) {
        String key = getCacheKey(s);
        if(virtualPropertiesSchema.containsKey(key))
            return virtualPropertiesSchema.get(key);

        String schema = (String)schemaCache.get(key);
        if(!Strings.isNullOrEmpty(schema)) {
            VirtualProperties schemaObj = gson.fromJson(schema, VirtualProperties.class);
            if(schemaObj != null) {
                virtualPropertiesSchema.put(key, schemaObj);
                return schemaObj;
            }
        }

        return null;
    }

    @Override
    public void delete(String s) {
        String key = getCacheKey(s);
        String schema = (String)schemaCache.get(key);
        if(!Strings.isNullOrEmpty(schema)) {
            schemaCache.delete(key);
        }

        if(virtualPropertiesSchema.containsKey(key)) {
            virtualPropertiesSchema.remove(key);
        }
    }

    @Override
    public void clearAll() {
        virtualPropertiesSchema.clear();
        schemaCache.clearAll();
    }

    private String getCacheKey(String s) {
        return s + VIRTUAL_PROPERTIES_SCHEMA;
    }
}
