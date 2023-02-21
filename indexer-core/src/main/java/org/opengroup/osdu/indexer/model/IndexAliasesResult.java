package org.opengroup.osdu.indexer.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Data
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IndexAliasesResult {
    private List<String> indicesWithAliases;
    private List<String> indicesWithoutAliases;

    public IndexAliasesResult() {
        indicesWithAliases = new ArrayList<>();
        indicesWithoutAliases = new ArrayList<>();
    }
}
