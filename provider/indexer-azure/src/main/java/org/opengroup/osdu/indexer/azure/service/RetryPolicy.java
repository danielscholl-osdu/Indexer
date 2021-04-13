package org.opengroup.osdu.indexer.azure.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
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

    private final int attempts =3;
    private final int waitDurationInMillis = 1000;
    private final String notFound ="notFound";

    Logger logger =  LoggerFactory.getLogger(RetryPolicy.class);


    public RetryConfig retryConfig()
    {
        IntervalFunction intervalWithExponentialBackoff = IntervalFunction.ofExponentialBackoff();

        RetryConfig config = RetryConfig.<HttpResponse>custom()
                .maxAttempts(attempts)
                .waitDuration(Duration.ofMillis(waitDurationInMillis))
                .retryOnResult(response -> {
                    JsonObject jsonObject = new JsonParser().parse(response.getBody()).getAsJsonObject();
                    JsonArray notFoundArray = (JsonArray) jsonObject.get(notFound);
                    if (notFoundArray.size() == 0 || notFoundArray.isJsonNull()) {
                        return false;
                    }
                    return true;
                })
                .build();
        return config;
    }



}
