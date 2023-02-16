package org.opengroup.osdu.indexer.model.indexproperty;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PropertyConfiguration {
    private final String EXTRACT_FIRST_MATCH_POLICY = "ExtractFirstMatch";
    private final String EXTRACT_ALL_MATCHES_POLICY = "ExtractAllMatches";

    @JsonProperty("Name")
    private String name;

    @JsonProperty("Policy")
    private String policy;

    @JsonProperty("UseCase")
    private String useCase;

    @JsonProperty("Paths")
    private List<PropertyPath> paths;

    public boolean isExtractFirstMatch() {
        return EXTRACT_FIRST_MATCH_POLICY.equalsIgnoreCase(policy);
    }

    public boolean isExtractAllMatches() {
        return EXTRACT_ALL_MATCHES_POLICY.equalsIgnoreCase(policy);
    }

    public boolean hasValidPolicy() {
        return isExtractFirstMatch() || isExtractAllMatches();
    }

    public String getRelatedObjectKind() {
        if(paths == null || paths.isEmpty())
            return null;

        for(PropertyPath path : paths) {
            if(path.hasValidRelatedObjectsSpec()) {
                return path.getRelatedObjectsSpec().getRelatedObjectKind();
            }
        }
        return null;
    }
}
