package org.opengroup.osdu.indexer.model;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class SchemaInfo {
    private SchemaIdentity schemaIdentity;
    private String status;
    private String scope;
}
