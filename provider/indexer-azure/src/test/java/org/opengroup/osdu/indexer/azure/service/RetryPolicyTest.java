package org.opengroup.osdu.indexer.azure.service;

import io.github.resilience4j.retry.RetryConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.http.FetchServiceHttpRequest;
import org.opengroup.osdu.core.common.http.UrlFetchServiceImpl;
import org.opengroup.osdu.core.common.model.http.HttpResponse;

import java.net.URISyntaxException;
import java.util.function.Predicate;

import static org.assertj.core.api.Fail.fail;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@RunWith(MockitoJUnitRunner.class)
public class RetryPolicyTest {

    private static final String json1="{\n" +
            "    \"records\": [\n" +
            "        {\n" +
            "            \"data\": {\n" +
            "                \"Spuddate\": \"atspud\",\n" +
            "                \"UWI\": \"atuwi\",\n" +
            "                \"dlLatLongWGS84latitude\": \"latitude\",\n" +
            "                \"dlLatLongWGS84longitude\": \"longitude\"\n" +
            "            },\n" +
            "            \"meta\": null,\n" +
            "            \"id\": \"opendes:wellbore:a9e2fa21318a49d681894d6251a0dc9c\",\n" +
            "            \"version\": 1617781485040156,\n" +
            "            \"kind\": \"opendes:at:wellbore:1.0.0\",\n" +
            "            \"acl\": {\n" +
            "                \"viewers\": [\n" +
            "                    \"data.test1@opendes.contoso.com\"\n" +
            "                ],\n" +
            "                \"owners\": [\n" +
            "                    \"data.test1@opendes.contoso.com\"\n" +
            "                ]\n" +
            "            },\n" +
            "            \"legal\": {\n" +
            "                \"legaltags\": [\n" +
            "                    \"opendes-test-tag\"\n" +
            "                ],\n" +
            "                \"otherRelevantDataCountries\": [\n" +
            "                    \"BR\"\n" +
            "                ],\n" +
            "                \"status\": \"compliant\"\n" +
            "            },\n" +
            "            \"createUser\": \"a38fdd7b-f209-4552-96cd-126ec2494605\",\n" +
            "            \"createTime\": \"2021-04-07T07:44:57.748Z\"\n" +
            "        }\n" +
            "    ],\n" +
            "    \"notFound\": [\n" +
            "        \"opendes:doc:foo-bar\"\n" +
            "    ],\n" +
            "    \"conversionStatuses\": []\n" +
            "}";

    private static final String json2 ="{\n" +
            "    \"records\": [\n" +
            "        {\n" +
            "            \"data\": {\n" +
            "                \"Spuddate\": \"atspud\",\n" +
            "                \"UWI\": \"atuwi\",\n" +
            "                \"dlLatLongWGS84latitude\": \"latitude\",\n" +
            "                \"dlLatLongWGS84longitude\": \"longitude\"\n" +
            "            },\n" +
            "            \"meta\": null,\n" +
            "            \"id\": \"opendes:wellbore:a9e2fa21318a49d681894d6251a0dc9c\",\n" +
            "            \"version\": 1617781485040156,\n" +
            "            \"kind\": \"opendes:at:wellbore:1.0.0\",\n" +
            "            \"acl\": {\n" +
            "                \"viewers\": [\n" +
            "                    \"data.test1@opendes.contoso.com\"\n" +
            "                ],\n" +
            "                \"owners\": [\n" +
            "                    \"data.test1@opendes.contoso.com\"\n" +
            "                ]\n" +
            "            },\n" +
            "            \"legal\": {\n" +
            "                \"legaltags\": [\n" +
            "                    \"opendes-test-tag\"\n" +
            "                ],\n" +
            "                \"otherRelevantDataCountries\": [\n" +
            "                    \"BR\"\n" +
            "                ],\n" +
            "                \"status\": \"compliant\"\n" +
            "            },\n" +
            "            \"createUser\": \"a38fdd7b-f209-4552-96cd-126ec2494605\",\n" +
            "            \"createTime\": \"2021-04-07T07:44:57.748Z\"\n" +
            "        }\n" +
            "    ],\n" +
            "    \"notFound\": [],\n" +
            "    \"conversionStatuses\": []\n" +
            "}";
    private static final String json3="{\n" +
            " \"records\" :[],\n" +
            " \"conversionStatuses\":[]\n" +
            "}";

    @InjectMocks
    private HttpResponse response;

    private UrlFetchServiceImpl urlFetchService;

    private FetchServiceHttpRequest httpRequest;

    private RetryPolicy retryPolicy;

    @Before
    public void setUp(){
        urlFetchService= mock(UrlFetchServiceImpl.class);
        retryPolicy = new RetryPolicy(urlFetchService);
        httpRequest = mock(FetchServiceHttpRequest.class);
    }

    @Test
    public void number_of_Attempts_must_be_3()
    {
        RetryConfig config = this.retryPolicy.retryConfig();
        int attempts = config.getMaxAttempts();
        int max_attempts =3;
        assert (max_attempts==attempts);
    }


    @Test
    public void retry_should_be_true_for_json1()
    {
        RetryConfig config = this.retryPolicy.retryConfig();
        Predicate<HttpResponse> retry = config.getResultPredicate();
        response.setBody(json1);
        boolean value = retry.test(response);
        assert(value==true);

    }

    @Test
    public void retry_should_be_false_for_json2()
    {
        RetryConfig config = this.retryPolicy.retryConfig();
        Predicate<HttpResponse> retry = config.getResultPredicate();
        response.setBody(json2);
        boolean value = retry.test(response);
        assert(value==false);
    }

    @Test
    public void retry_should_be_false_for_json3()
    {
        RetryConfig config = this.retryPolicy.retryConfig();
        Predicate<HttpResponse> retry = config.getResultPredicate();
        response.setBody(json3);
        boolean value = retry.test(response);
        assert (value==false);
    }

    @Test
    public void urlFetchService_should_be_retried_for_json1() throws URISyntaxException {
        response.setBody(json1);
        when(urlFetchService.sendRequest(httpRequest)).thenReturn(response);
        retryPolicy.retryFunction(httpRequest);
        verify(urlFetchService,times(3)).sendRequest(httpRequest);
    }

    @Test
    public void urlFetchService_shouldNot_be_retried_for_json2() throws URISyntaxException {
        response.setBody(json2);
        when(urlFetchService.sendRequest(httpRequest)).thenReturn(response);
        retryPolicy.retryFunction(httpRequest);
        verify(urlFetchService,atMost(1)).sendRequest(httpRequest);
    }

    @Test
    public void urlFetchService_shouldNot_be_retried_for_json3() throws URISyntaxException {
        response.setBody(json3);
        when(urlFetchService.sendRequest(httpRequest)).thenReturn(response);
        retryPolicy.retryFunction(httpRequest);
        verify(urlFetchService,atMost(1)).sendRequest(httpRequest);
    }

    @Test
    public void retryFunction_shouldReturn_null_ifURISyntaxException() throws URISyntaxException
    {
        when(urlFetchService.sendRequest(httpRequest)).thenThrow(new URISyntaxException("ye","ye"));
        assert(retryPolicy.retryFunction(httpRequest)==null);
    }

}
