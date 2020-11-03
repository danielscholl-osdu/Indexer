package org.opengroup.osdu.indexer.schema.converter.dto;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.Map;

@Data
@FieldDefaults(level= AccessLevel.PRIVATE)
public class Definition {
    String type;
    Map<String, TypeProperty> properties;
}
