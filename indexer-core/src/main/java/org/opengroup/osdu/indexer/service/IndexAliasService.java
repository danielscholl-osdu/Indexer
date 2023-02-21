package org.opengroup.osdu.indexer.service;

import org.elasticsearch.client.RestHighLevelClient;
import org.opengroup.osdu.indexer.model.IndexAliasesResult;

public interface IndexAliasService {
    IndexAliasesResult createIndexAliasesForAll();
    boolean createIndexAlias(RestHighLevelClient restClient, String kind);
}
