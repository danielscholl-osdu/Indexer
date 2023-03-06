package org.opengroup.osdu.indexer.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SchemaIdentity {
    String authority;
    String source;
    private String entityType;
    private int schemaVersionMajor;
    private int schemaVersionMinor;
    private int schemaVersionPatch;
    private String id;
}
