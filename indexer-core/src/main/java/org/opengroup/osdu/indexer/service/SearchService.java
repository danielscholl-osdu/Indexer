package org.opengroup.osdu.indexer.service;

import org.opengroup.osdu.indexer.model.SearchRequest;
import org.opengroup.osdu.indexer.model.SearchResponse;

import java.net.URISyntaxException;

/**
 * Interface to consume schemas from the Schema Service
 */
public interface SearchService {
    /**
     *
     * @param searchRequest
     * @return
     * @throws URISyntaxException
     */
    SearchResponse query(SearchRequest searchRequest) throws URISyntaxException;

    /**
     *
     * @param searchRequest
     * @return
     * @throws URISyntaxException
     */
    SearchResponse queryWithCursor(SearchRequest searchRequest) throws URISyntaxException;
}
