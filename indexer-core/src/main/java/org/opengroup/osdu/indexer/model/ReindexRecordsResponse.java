package org.opengroup.osdu.indexer.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ReindexRecordsResponse {
    private List<String> reIndexedRecords;
    private List<String> notFoundRecords;
}
