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
public class PropertyConfiguration {
    private final String EXTRACT_FIRST_MATCH_POLICY = "ExtractFirstMatch";

    @JsonProperty("Name")
    private String name;

    @JsonProperty("Array")
    private boolean array;

    @JsonProperty("Policy")
    private String policy;

    @JsonProperty("UseCase")
    private String useCase;

    @JsonProperty("Paths")
    private List<Path> paths;

    public boolean isExtractFirstMatch() {
        return EXTRACT_FIRST_MATCH_POLICY.equals(policy);
    }

    public String getRelatedObjectKind() {
        if(paths == null || paths.isEmpty())
            return null;

        for(Path path : paths) {
            if(path.mappedRelatedObject()) {
                return path.getRelatedObjectKind();
            }
        }
        return null;
    }
}
