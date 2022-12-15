package org.opengroup.osdu.indexer.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class IndexPropertyPath {
    @JsonProperty("RelatedConditionMatches")
    private List<String> relatedConditionMatches;

    @JsonProperty("RelatedConditionProperty")
    private String relatedConditionProperty;

    @JsonProperty("RelatedObjectID")
    private String relatedObjectID;

    @JsonProperty("ValuePath")
    private String valuePath;

    @JsonProperty("RelatedObjectKind")
    private String relatedObjectKind;
}
