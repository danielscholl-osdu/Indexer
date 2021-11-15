package org.opengroup.osdu.indexer.service;

import org.elasticsearch.client.RestHighLevelClient;
import org.opengroup.osdu.core.common.search.IndicesService;

import java.io.IOException;

public interface IndexerIndicesService extends IndicesService {

    boolean isIndexReady(RestHighLevelClient client, String index) throws IOException;
}
