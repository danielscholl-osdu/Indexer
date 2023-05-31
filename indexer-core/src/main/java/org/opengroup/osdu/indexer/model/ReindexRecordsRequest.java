package org.opengroup.osdu.indexer.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Data
@AllArgsConstructor
public class ReindexRecordsRequest {
    @NotNull
    @Size(min = 1)
    private List<@NotBlank String> recordIds;
}
