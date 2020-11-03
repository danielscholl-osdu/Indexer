package org.opengroup.osdu.indexer.schema.converter.dto;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level= AccessLevel.PRIVATE)
public class SchemaRoot {
    Definitions definitions;
    Properties properties;
}
