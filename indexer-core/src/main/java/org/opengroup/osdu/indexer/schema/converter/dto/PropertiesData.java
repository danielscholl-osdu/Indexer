package org.opengroup.osdu.indexer.schema.converter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@FieldDefaults(level= AccessLevel.PRIVATE)
public class PropertiesData {
    List<AllOfItem> allOf;
    @JsonProperty("$ref")
    String ref;
}
