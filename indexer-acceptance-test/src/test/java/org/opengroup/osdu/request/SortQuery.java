package org.opengroup.osdu.request;

import co.elastic.clients.elasticsearch._types.SortOrder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class SortQuery {
    private List<String> field;
    private List<SortOrder> order;
}
