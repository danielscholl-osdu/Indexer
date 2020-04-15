package org.opengroup.osdu.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.elasticsearch.search.sort.SortOrder;

import java.util.List;

@Data
@NoArgsConstructor
public class SortQuery {
    private List<String> field;
    private List<SortOrder> order;
}
