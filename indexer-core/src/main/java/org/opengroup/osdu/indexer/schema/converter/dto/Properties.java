package org.opengroup.osdu.indexer.schema.converter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level= AccessLevel.PRIVATE)
public class Properties {
    PropertiesData data;
    @JsonProperty("$ref")
    String ref;
}
