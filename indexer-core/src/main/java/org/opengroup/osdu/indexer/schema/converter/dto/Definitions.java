package org.opengroup.osdu.indexer.schema.converter.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.util.HashMap;
import java.util.Map;

@FieldDefaults(level= AccessLevel.PRIVATE)
public class Definitions {
    Map<String, Definition> items = new HashMap<>();

    public Definition getDefinition(String name) {
        return items.get(name);
    }

    @JsonAnySetter
    public void add(String key, Definition value) {
        items.put(key, value);
    }

    @JsonAnyGetter
    public Map<String, Definition> getProperties() {
        return items;
    }
}
