package org.opengroup.osdu.indexer.model.indexproperty;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PropertyPath {
    @JsonProperty("RelatedObjectsSpec")
    private RelatedObjectsSpec relatedObjectsSpec;

    @JsonProperty("ValueExtraction")
    private ValueExtraction valueExtraction;

    public boolean hasValidRelatedObjectsSpec() {
        return relatedObjectsSpec != null && relatedObjectsSpec.isValid();
    }

    public boolean hasValidValueExtraction() {
        return valueExtraction != null && valueExtraction.isValid();
    }
}
