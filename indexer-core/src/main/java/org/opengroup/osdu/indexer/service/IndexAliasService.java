package org.opengroup.osdu.indexer.service;

import org.elasticsearch.client.RestHighLevelClient;
import org.opengroup.osdu.indexer.model.IndexAliasesProvisionResult;

public interface IndexAliasService {
    IndexAliasesProvisionResult createIndexAliasesForAll();
    boolean createIndexAlias(RestHighLevelClient restClient, String kind);
}
