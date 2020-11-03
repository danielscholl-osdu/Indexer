package org.opengroup.osdu.indexer.schema.converter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level= AccessLevel.PRIVATE)
public class TypeProperty {
    String type;
    String pattern;
    String format;
    @JsonProperty("$ref")
    String ref;
    Items items;
}
