package org.opengroup.osdu.indexer.schema.converter;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import org.opengroup.osdu.indexer.model.indexproperty.PropertyConfigurations;
import org.opengroup.osdu.indexer.provider.interfaces.ISchemaCache;
import org.opengroup.osdu.indexer.schema.converter.interfaces.IPropertyConfigurationsCache;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class PropertyConfigurationsCacheImpl implements IPropertyConfigurationsCache<String, PropertyConfigurations> {
    private static final String PROPERTY_CONFIGURATIONS = "_property_configurations";
    private static final Gson gson = new Gson();

    @Inject
    private ISchemaCache schemaCache;

    @Override
    public void put(String s, PropertyConfigurations o) {
        if (Strings.isNullOrEmpty(s) || o == null)
            return;

        String key = getCacheKey(s);
        schemaCache.put(key, gson.toJson(o));
    }

    @Override
    public PropertyConfigurations get(String s) {
        if (Strings.isNullOrEmpty(s))
            return null;

        String key = getCacheKey(s);
        String schema = (String) schemaCache.get(key);
        if (!Strings.isNullOrEmpty(schema)) {
           return gson.fromJson(schema, PropertyConfigurations.class);
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
