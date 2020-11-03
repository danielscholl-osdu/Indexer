package org.opengroup.osdu.indexer.schema.converter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.Map;

@Data
@FieldDefaults(level= AccessLevel.PRIVATE)
public class AllOfItem {
    @JsonProperty("$ref")
    String ref;
    String type;
    Map<String, TypeProperty> properties;
}