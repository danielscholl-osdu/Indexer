package org.opengroup.osdu.indexer.model;

import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
public class SearchResponse {
    private String cursor;
    private List<SearchRecord> results;
    private int totalCount;
}
