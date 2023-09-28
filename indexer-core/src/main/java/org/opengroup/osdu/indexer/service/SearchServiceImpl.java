/*
 * Copyright © Schlumberger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.indexer.service;

import com.google.api.client.http.HttpMethods;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import org.opengroup.osdu.core.common.http.FetchServiceHttpRequest;
import org.opengroup.osdu.core.common.http.IUrlFetchService;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.HttpResponse;
import org.opengroup.osdu.core.common.provider.interfaces.IRequestInfo;
import org.opengroup.osdu.indexer.config.IndexerConfigurationProperties;
import org.opengroup.osdu.indexer.model.SearchRequest;
import org.opengroup.osdu.indexer.model.SearchResponse;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.net.URISyntaxException;

@Component
public class SearchServiceImpl implements SearchService {
    private static final String ERROR_MESSAGE = "Search service: failed to call the search service.";
    private static final String QUERY_PATH = "query";
    private static final String QUERY_WITH_CURSOR_PATH = "query_with_cursor";
    private final Gson gson = new Gson();
    private static final int OK_CODE = 200;

    @Inject
    private IUrlFetchService urlFetchService;
    @Inject
    private IRequestInfo requestInfo;
    @Inject
    private IndexerConfigurationProperties configurationProperties;
    @Inject
    private JaxRsDpsLog jaxRsDpsLog;

    @Override
    public SearchResponse query(SearchRequest searchRequest) throws URISyntaxException {
        return searchRecords(searchRequest, QUERY_PATH);
    }

    @Override
    public SearchResponse queryWithCursor(SearchRequest searchRequest) throws URISyntaxException {
        return searchRecords(searchRequest, QUERY_WITH_CURSOR_PATH);
    }

    private SearchResponse searchRecords(SearchRequest searchRequest, String path) throws URISyntaxException {
        if(Strings.isNullOrEmpty(configurationProperties.getSearchHost())) {
            jaxRsDpsLog.error("SEARCH_HOST", "The environment variable SEARCH_HOST is not setup");
            return new SearchResponse();
        }

        try {
            String body = this.gson.toJson(searchRequest);
            String url = String.format("%s/%s", configurationProperties.getSearchHost(), path);
            FetchServiceHttpRequest request = FetchServiceHttpRequest.builder()
                    .httpMethod(HttpMethods.POST)
                    .url(url)
                    .headers(this.requestInfo.getHeaders())
                    .body(body)
                    .build();
            HttpResponse response = this.urlFetchService.sendRequest(request);

            if (response != null && response.getResponseCode() == OK_CODE) {
                return gson.fromJson(response.getBody(), SearchResponse.class);
            } else {
                if (response != null)
                    jaxRsDpsLog.error(ERROR_MESSAGE + String.format(" responseCode = %d", response.getResponseCode()));
                else
                    jaxRsDpsLog.error(String.format(ERROR_MESSAGE + " The response is null."));
                return new SearchResponse();
            }
        }
        catch(URISyntaxException ex) {
            throw ex;
        }
        catch(Exception ex) {
            jaxRsDpsLog.error(ERROR_MESSAGE, ex);
            throw new URISyntaxException(ex.getMessage(), "Unexpected exception type: " + ex.getClass().getName());
        }
    }
}
