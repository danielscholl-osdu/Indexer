package org.opengroup.osdu.models.record;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RecordData {

    @JsonProperty("data")
    private Map<String, List<Map<String,Object>>> data;

    public Map<String, List<Map<String, Object>>> getData() {
        return data;
    }

}
