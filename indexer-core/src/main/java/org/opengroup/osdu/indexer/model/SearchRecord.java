package org.opengroup.osdu.indexer.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.ToString;

import java.util.Map;

@Data
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchRecord {
    private String id;
    private String kind;
    private Map<String, Object> data;
}
