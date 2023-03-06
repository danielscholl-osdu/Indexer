package org.opengroup.osdu.indexer.model;

import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
public class SchemaInfoResponse {
    private List<SchemaInfo> schemaInfos;
    private int offset;
    private int count;
    private int totalCount;
}
