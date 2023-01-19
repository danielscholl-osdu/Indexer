package org.opengroup.osdu.indexer.model.indexproperty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PropertyConfigurations {
    @JsonProperty("Name")
    private String name;

    @JsonProperty("Description")
    private String description;

    @JsonProperty("Code")
    private String code;

    @JsonProperty("AttributionAuthority")
    private String attributionAuthority;

    @JsonProperty("Configurations")
    private List<PropertyConfiguration> configurations;

    public List<String> getRelatedObjectKinds() {
        if(configurations == null || configurations.isEmpty())
            return new ArrayList<>();

        Set<String> relatedObjectKinds = new HashSet<>();
        for(PropertyConfiguration configuration : configurations) {
            String relatedObjectKind = configuration.getRelatedObjectKind();
            if(!Strings.isNullOrEmpty(relatedObjectKind)) {
                relatedObjectKinds.add(relatedObjectKind);
            }
        }
        return new ArrayList<>(relatedObjectKinds);
    }
}
