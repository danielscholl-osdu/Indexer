package org.opengroup.osdu.indexer.azure.service;


import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.opengroup.osdu.core.common.http.FetchServiceHttpRequest;
import org.opengroup.osdu.core.common.http.IUrlFetchService;
import org.opengroup.osdu.core.common.http.UrlFetchServiceImpl;
import org.opengroup.osdu.core.common.model.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import java.net.URISyntaxException;
import java.util.function.Supplier;

@Service
@RequestScope
@Primary
public class UrlFetchServiceAzureImpl extends UrlFetchServiceImpl implements IUrlFetchService {



    Logger logger =  LoggerFactory.getLogger(UrlFetchServiceAzureImpl.class);
    @Override
    public HttpResponse sendRequest(FetchServiceHttpRequest httpRequest) throws URISyntaxException {
        logger.info("inside azure impl muskan;");
        HttpResponse output = this.retryFunction(httpRequest);
        logger.info("response code" + output.getResponseCode());
        return output;
    }

    public HttpResponse retryFunction(FetchServiceHttpRequest request)
    {
        logger.info("inside retry function muskan");
        RetryPolicy retryPolicy= new RetryPolicy();
        RetryConfig config= retryPolicy.retryConfig();
        RetryRegistry registry = RetryRegistry.of(config);
        Retry retry = registry.retry("retryPolicy", config);


        Supplier<HttpResponse> urlFetchServiceSupplier = ()-> {
            try {
                return super.sendRequest(request);
            } catch (URISyntaxException e) {
                e.printStackTrace();
                return null;
            }
        };

        if(urlFetchServiceSupplier!= null) {
            Supplier<HttpResponse> decoratedUrlFetchServiceSupplier = Retry.decorateSupplier(retry, urlFetchServiceSupplier);
            logger.info("url fetch supplier is not null");
            return decoratedUrlFetchServiceSupplier.get();
        }

        return null;
    }

}
