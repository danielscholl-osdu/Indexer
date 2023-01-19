package org.opengroup.osdu.indexer.model.indexproperty;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Path {
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

    public boolean mappedRelatedObject() {
        return !Strings.isNullOrEmpty(relatedObjectID) && !Strings.isNullOrEmpty(relatedObjectKind);
    }

    public boolean isValid() {
        return !Strings.isNullOrEmpty(valuePath); // Add more constraints
    }
}
