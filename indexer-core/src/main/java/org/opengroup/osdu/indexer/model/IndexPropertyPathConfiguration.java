package org.opengroup.osdu.indexer.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class IndexPropertyPathConfiguration {
    @JsonProperty("Name")
    private String name;

    @JsonProperty("Description")
    private String description;

    @JsonProperty("Code")
    private String code;

    @JsonProperty("Array")
    private boolean array;

    @JsonProperty("Policy")
    private String policy;

    @JsonProperty("UseCase")
    private String useCase;

    @JsonProperty("Paths")
    private List<IndexPropertyPath> paths;
}
