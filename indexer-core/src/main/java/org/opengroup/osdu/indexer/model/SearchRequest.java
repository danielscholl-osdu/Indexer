package org.opengroup.osdu.indexer.model;

import lombok.Data;
import lombok.ToString;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@ToString
public class SearchRequest {
    @NotNull(message = "Kind is missing")
    private Object kind;
    private String query;
    private int limit;
    private int offset;
    private String cursor;
    private List<String> returnedFields;
    private boolean trackTotalCount = true;
}
