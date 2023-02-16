package org.opengroup.osdu.indexer.model.indexproperty;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RelatedCondition {
    protected static final String ARRAY_SYMBOL = "[]";

    @JsonProperty("RelatedObjectKind")
    protected String relatedObjectKind;

    @JsonProperty("RelatedConditionProperty")
    protected String relatedConditionProperty;

    @JsonProperty("RelatedConditionMatches")
    protected List<String> relatedConditionMatches;
}
