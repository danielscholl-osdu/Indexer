package org.opengroup.osdu.indexer.azure.service;

import com.google.gson.*;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.opengroup.osdu.core.common.http.FetchServiceHttpRequest;
import org.opengroup.osdu.core.common.http.UrlFetchServiceImpl;
import org.opengroup.osdu.core.common.model.http.HttpResponse;

import java.net.URISyntaxException;
import java.time.Duration;
import java.util.function.Supplier;

import static io.github.resilience4j.retry.RetryConfig.custom;
import static java.time.temporal.ChronoUnit.SECONDS;

public class RetryPolicy {

    private final int attempts =3;
    private final int waitDurationInMillis = 1000;
    private final String notFound ="notFound";

    private UrlFetchServiceImpl urlFetchService;

    public RetryPolicy()
    {
        urlFetchService = new UrlFetchServiceImpl();
    }

    public RetryPolicy(UrlFetchServiceImpl urlFetchService)
    {
        this.urlFetchService = urlFetchService;
    }

    public RetryConfig retryConfig()
    {
        RetryConfig config = RetryConfig.<HttpResponse>custom()
                .maxAttempts(attempts)
                .waitDuration(Duration.ofMillis(waitDurationInMillis))
                .retryOnResult(response -> retryOnlyOn(response))
                .build();
        return config;
    }

    private boolean retryOnlyOn(HttpResponse response)
    {
        if(response==null || response.getBody().isEmpty())
        {
            return false;
        }
        JsonObject jsonObject = new JsonParser().parse(response.getBody()).getAsJsonObject();
        JsonElement notFoundElement = (JsonArray) jsonObject.get(notFound);
        if(notFoundElement==null)
        {
            return false;
        }
        JsonArray notFoundArray  = null;
        if(notFoundElement.isJsonArray())
        {
            notFoundArray = notFoundElement.getAsJsonArray();
        }
        else
        {
            return false;
        }
        if (notFoundArray.size() == 0 || notFoundArray.isJsonNull()) {
            return false;
        }
        return true;
    }

    public HttpResponse retryFunction(FetchServiceHttpRequest request)
    {
        RetryConfig config= this.retryConfig();
        RetryRegistry registry = RetryRegistry.of(config);
        Retry retry = registry.retry("retryPolicy", config);

        Supplier<HttpResponse> urlFetchServiceSupplier = ()-> {
            try {
                return this.urlFetchService.sendRequest(request);
            } catch (URISyntaxException e) {
                return null;
            }
        };

        if(urlFetchServiceSupplier!= null) {
            Supplier<HttpResponse> decoratedUrlFetchServiceSupplier = Retry.decorateSupplier(retry, urlFetchServiceSupplier);
            return decoratedUrlFetchServiceSupplier.get();
        }
        return null;
    }

}
