package org.opengroup.osdu.indexer.azure.service;

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
                .retryOnResult(response -> response.getResponseCode() == 404)
                .build();
        return config;

//        .retryOnResult(response -> response.getStatus() == 500)
//            .retryOnException(e -> e instanceof WebServiceException)
//            .retryExceptions(IOException.class, TimeoutException.class)
//            .ignoreExceptions(BusinessException.class, OtherBusinessException.class)
//            .failAfterMaxAttempts(true)
    }



}
