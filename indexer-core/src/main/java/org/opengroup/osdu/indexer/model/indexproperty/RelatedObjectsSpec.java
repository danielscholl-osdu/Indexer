package org.opengroup.osdu.indexer.model.indexproperty;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.api.client.util.Strings;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RelatedObjectsSpec {
    @JsonProperty("RelatedObjectID")
    private String relatedObjectID;

    @JsonProperty("RelatedObjectKind")
    private String relatedObjectKind;

    @JsonProperty("RelatedConditionProperty")
    private String relatedConditionProperty;

    @JsonProperty("RelatedConditionMatches")
    private List<String> relatedConditionMatches;

    public boolean isValid() {
        return !Strings.isNullOrEmpty(relatedObjectID) && !Strings.isNullOrEmpty(relatedObjectKind);
    }
}
