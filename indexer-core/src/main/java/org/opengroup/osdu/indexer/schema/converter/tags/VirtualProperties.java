package org.opengroup.osdu.indexer.schema.converter.tags;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class VirtualProperties {
    private Map<String, VirtualProperty> items = new HashMap<>();

    public VirtualProperty getProperty(String name) {
        return items.get(name);
    }

    @JsonAnySetter
    public void add(String key, VirtualProperty value) {
        items.put(key, value);
    }

    @JsonAnyGetter
    public Map<String, VirtualProperty> getProperties() {
        return items;
    }
}
