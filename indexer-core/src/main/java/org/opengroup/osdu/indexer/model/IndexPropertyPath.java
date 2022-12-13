package org.opengroup.osdu.indexer.model;

import lombok.Data;

import java.util.List;

@Data
public class IndexPropertyPath {
    private List<String> RelatedConditionMatches;

    private String RelatedConditionProperty;

    private String RelatedObjectID;

    private String ValuePath;

    private String RelatedObjectKind;
}
