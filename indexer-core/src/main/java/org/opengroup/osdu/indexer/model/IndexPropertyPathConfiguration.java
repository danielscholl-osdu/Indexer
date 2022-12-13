package org.opengroup.osdu.indexer.model;

import lombok.Data;

import java.util.List;

@Data
public class IndexPropertyPathConfiguration {
    private String Name;

    private String Description;

    private String code;

    private boolean Array;

    private String Policy;

    private List<IndexPropertyPath> Paths;
}
