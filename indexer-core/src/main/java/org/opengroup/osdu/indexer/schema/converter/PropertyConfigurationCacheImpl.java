package org.opengroup.osdu.indexer.schema.converter;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.opengroup.osdu.indexer.provider.interfaces.ISchemaCache;
import org.opengroup.osdu.indexer.schema.converter.interfaces.IPropertyConfigurationCache;
import org.opengroup.osdu.indexer.schema.converter.tags.PropertyConfiguration;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

@Component
public class PropertyConfigurationCacheImpl implements IPropertyConfigurationCache<String, Map<String, List<PropertyConfiguration>>> {
    private static final String PROPERTY_CONFIGURATIONS = "_property_configurations";
    private static final Type configurationType = new TypeToken<Map<String, List<PropertyConfiguration>>>() {}.getType();
    private static final Gson gson = new Gson();

    @Inject
    private ISchemaCache schemaCache;

    @Override
    public void put(String s, Map<String, List<PropertyConfiguration>> o) {
        if (Strings.isNullOrEmpty(s) || o == null)
            return;

        String key = getCacheKey(s);
        schemaCache.put(key, gson.toJson(o));
    }

    @Override
    public Map<String, List<PropertyConfiguration>> get(String s) {
        if (Strings.isNullOrEmpty(s))
            return null;

        String key = getCacheKey(s);
        String schema = (String) schemaCache.get(key);
        if (!Strings.isNullOrEmpty(schema)) {
            Map<String, List<PropertyConfiguration>> schemaObj = gson.fromJson(schema, configurationType);
            return schemaObj;
        }

        return null;
    }

    @Override
    public void delete(String s) {
        if (Strings.isNullOrEmpty(s))
            return;

        String key = getCacheKey(s);
        String schema = (String) schemaCache.get(key);
        if (!Strings.isNullOrEmpty(schema)) {
            schemaCache.delete(key);
        }
    }

    @Override
    public void clearAll() {
        schemaCache.clearAll();
    }

    private String getCacheKey(String s) {
        return s + PROPERTY_CONFIGURATIONS;
    }
}
