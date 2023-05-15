package org.opengroup.osdu.indexer.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BulkRequestResult {
    private List<String> failureRecordIds;
    private List<String> retryUpsertRecordIds;
}