package org.opengroup.osdu.indexer.schema.converter.tags;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PropertyConfiguration {
    @JsonProperty("Configuration")
    private String configuration;
}
