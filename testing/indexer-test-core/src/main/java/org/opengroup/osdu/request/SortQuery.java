package org.opengroup.osdu.request;

import co.elastic.clients.elasticsearch._types.SortOrder;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SortQuery {
    private List<String> field;
    private List<SortOrder> order;
}
