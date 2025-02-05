package org.opengroup.osdu.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class ResponseMock extends ResponseBase {
    private List<Map<String, Object>> results;
    private List<Object> aggregations;
    private long totalCount;
    private String cursor;
}