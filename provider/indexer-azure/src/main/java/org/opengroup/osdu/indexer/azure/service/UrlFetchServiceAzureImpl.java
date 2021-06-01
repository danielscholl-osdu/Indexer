//  Copyright © Microsoft Corporation
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.opengroup.osdu.indexer.azure.service;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.opengroup.osdu.core.common.http.FetchServiceHttpRequest;
import org.opengroup.osdu.core.common.http.IUrlFetchService;
import org.opengroup.osdu.core.common.http.UrlFetchServiceImpl;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import java.net.URISyntaxException;
import java.util.function.Supplier;

/**
 * This class has same function as that of UrlFetchService except in the case of
 * <prefix>/storage/v2/query/records:batch call for which it enables retry
 */

@Service
@RequestScope
@Primary
public class UrlFetchServiceAzureImpl implements IUrlFetchService {

    public static final String STORAGE_QUERY_RECORD_FOR_CONVERSION_HOST_URL = "storage/v2/query/records:batch";

    @Autowired
    private RetryPolicy policy;

    @Autowired
    private UrlFetchServiceImpl urlFetchService;

    @Autowired
    private JaxRsDpsLog logger;
    /**
     * this method invokes retryFunction only for <prefix>/storage/v2/query/records:batch
     * calls otherwise invokes UrlFetchService.sendRequest(FetchServiceHttpRequest request)
     *
     * @param httpRequest
     * @return
     * @throws URISyntaxException
     */
    @Override
    public HttpResponse sendRequest(FetchServiceHttpRequest httpRequest) throws URISyntaxException {
        HttpResponse output;
        if (httpRequest.getUrl().contains(STORAGE_QUERY_RECORD_FOR_CONVERSION_HOST_URL)) {
            output = this.retryFunction(httpRequest);
            if (output != null) {
                return output;
            }
        }
        return this.urlFetchService.sendRequest(httpRequest);
    }

    /**
     * decorates UrlFetchService.sendRequest(FetchServiceHttpRequest request)
     * with retry configurations in RetryPolicy
     *
     * @param request
     * @return null if URISyntaxException is caught else returns HttpResponse
     */
    private HttpResponse retryFunction(FetchServiceHttpRequest request) {
        RetryConfig config = this.policy.retryConfig();
        RetryRegistry registry = RetryRegistry.of(config);
        Retry retry = registry.retry("retryPolicy", config);

        Supplier<HttpResponse> urlFetchServiceSupplier = () -> {
            try {
                return this.urlFetchService.sendRequest(request);
            } catch (URISyntaxException e) {
                logger.error("HttpResponse is null due to URISyntaxException. " + e.getReason());
                return null;
            }
        };
        return (urlFetchServiceSupplier == null) ? null : Retry.decorateSupplier(retry, urlFetchServiceSupplier).get();
    }
}
