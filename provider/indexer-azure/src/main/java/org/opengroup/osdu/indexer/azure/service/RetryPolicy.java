package org.opengroup.osdu.indexer.azure.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.vavr.CheckedFunction0;
import org.opengroup.osdu.core.common.http.FetchServiceHttpRequest;
import org.opengroup.osdu.core.common.http.IUrlFetchService;
import org.opengroup.osdu.core.common.model.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

import static io.github.resilience4j.retry.RetryConfig.custom;
import static java.time.temporal.ChronoUnit.SECONDS;

public class RetryPolicy {

    private static final int attempts =3;
    private static final int waitDurationInMillis = 1000;

    Logger logger =  LoggerFactory.getLogger(RetryPolicy.class);


    public RetryConfig retryConfig()
    {
        IntervalFunction intervalWithExponentialBackoff = IntervalFunction.ofExponentialBackoff();

        RetryConfig config = RetryConfig.<HttpResponse>custom()
                .maxAttempts(attempts)
                .waitDuration(Duration.ofMillis(waitDurationInMillis))
                .retryOnResult(response -> {
                    List<String> notFound = new Gson().fromJson(response.getBody(), List.class);
                    if(notFound.isEmpty()) {
                        return true;
                    }
                    return false;
                })
                .build();
        return config;
    }



}
