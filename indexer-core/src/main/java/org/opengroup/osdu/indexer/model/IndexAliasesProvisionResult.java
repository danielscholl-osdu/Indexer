package org.opengroup.osdu.indexer.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IndexAliasesProvisionResult {
    private List<String> indicesWithAliasesCreated;
    private int indicesWithAliasesCount;
    private List<String> indicesWithoutAliasesCreated;
    private int indicesWithoutAliasesCount;
}
