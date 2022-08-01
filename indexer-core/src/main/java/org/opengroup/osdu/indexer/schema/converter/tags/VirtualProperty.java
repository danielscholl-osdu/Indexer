package org.opengroup.osdu.indexer.schema.converter.tags;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class VirtualProperty {
    private String type;
    @JsonProperty("priority")
    private List<Priority> priorities;
}
