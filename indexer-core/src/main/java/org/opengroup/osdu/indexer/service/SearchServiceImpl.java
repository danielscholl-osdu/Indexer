package org.opengroup.osdu.indexer.service;

import com.google.api.client.http.HttpMethods;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import org.opengroup.osdu.core.common.http.FetchServiceHttpRequest;
import org.opengroup.osdu.core.common.http.IUrlFetchService;
import org.opengroup.osdu.core.common.provider.interfaces.IRequestInfo;
import org.opengroup.osdu.core.common.model.http.HttpResponse;
import org.opengroup.osdu.indexer.config.IndexerConfigurationProperties;
import org.opengroup.osdu.indexer.model.SearchRequest;
import org.opengroup.osdu.indexer.model.SearchResponse;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.net.URISyntaxException;

@Component
public class SearchServiceImpl implements SearchService {
    private static final String QUERY_PATH = "query";
    private static final String QUERY_WITH_CURSOR_PATH = "query_with_cursor";
    private final Gson gson = new Gson();

    @Inject
    private IUrlFetchService urlFetchService;
    @Inject
    private IRequestInfo requestInfo;
    @Inject
    private IndexerConfigurationProperties configurationProperties;

    @Override
    public SearchResponse query(SearchRequest searchRequest) throws URISyntaxException {
        return searchRecords(searchRequest, QUERY_PATH);
    }

    @Override
    public SearchResponse queryWithCursor(SearchRequest searchRequest) throws URISyntaxException {
        return searchRecords(searchRequest, QUERY_WITH_CURSOR_PATH);
    }

    private SearchResponse searchRecords(SearchRequest searchRequest, String path) throws URISyntaxException {
        // The following statements should be removed later
        if(Strings.isNullOrEmpty(configurationProperties.getSearchHost()))
            throw new URISyntaxException("SEARCH_HOST", "The environment variable SEARCH_HOST is not setup");

        String body = this.gson.toJson(searchRequest);
        String url = String.format("%s/%s", configurationProperties.getSearchHost(), path);
        FetchServiceHttpRequest request = FetchServiceHttpRequest.builder()
                .httpMethod(HttpMethods.POST)
                .url(url)
                .headers(this.requestInfo.getHeaders())
                .body(body)
                .build();
        HttpResponse response = this.urlFetchService.sendRequest(request);
        return gson.fromJson(response.getBody(), SearchResponse.class);
    }
}
