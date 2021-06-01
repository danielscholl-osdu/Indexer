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

import io.github.resilience4j.retry.RetryConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.http.FetchServiceHttpRequest;
import org.opengroup.osdu.core.common.http.UrlFetchServiceImpl;
import org.opengroup.osdu.core.common.model.http.HttpResponse;

import java.util.function.Predicate;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class RetryPolicyTest {

    private static final String JSON_RESPONSE_WITH_NOT_FOUND = "{\n" +
            "    \"records\": [\n" +
            "        {\n" +
            "            \"data\": {\n" +
            "                \"Spuddate\": \"atspud\",\n" +
            "                \"UWI\": \"atuwi\",\n" +
            "                \"latitude\": \"latitude\",\n" +
            "                \"longitude\": \"longitude\"\n" +
            "            },\n" +
            "            \"meta\": null,\n" +
            "            \"id\": \"demo\",\n" +
            "            \"version\": demo,\n" +
            "            \"kind\": \"demo\",\n" +
            "            \"acl\": {\n" +
            "                \"viewers\": [\n" +
            "                    \"demo\"\n" +
            "                ],\n" +
            "                \"owners\": [\n" +
            "                    \"demo\"\n" +
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
            "            \"createUser\": \"demo\",\n" +
            "            \"createTime\": \"demo\"\n" +
            "        }\n" +
            "    ],\n" +
            "    \"notFound\": [\n" +
            "        \"demo\"\n" +
            "    ],\n" +
            "    \"conversionStatuses\": []\n" +
            "}";

    private static final String JSON_RESPONSE1_WITHOUT_NOT_FOUND = "{\n" +
            "    \"records\": [\n" +
            "        {\n" +
            "            \"data\": {\n" +
            "                \"Spuddate\": \"atspud\",\n" +
            "                \"UWI\": \"atuwi\",\n" +
            "                \"latitude\": \"latitude\",\n" +
            "                \"longitude\": \"longitude\"\n" +
            "            },\n" +
            "            \"meta\": null,\n" +
            "            \"id\": \"demo\",\n" +
            "            \"version\": demo,\n" +
            "            \"kind\": \"demo\",\n" +
            "            \"acl\": {\n" +
            "                \"viewers\": [\n" +
            "                    \"demo\"\n" +
            "                ],\n" +
            "                \"owners\": [\n" +
            "                    \"demo\"\n" +
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
            "            \"createUser\": \"demo\",\n" +
            "            \"createTime\": \"demo\"\n" +
            "        }\n" +
            "    ],\n" +
            "    \"notFound\": [],\n" +
            "    \"conversionStatuses\": []\n" +
            "}";

    private static final String JSON_RESPONSE2_WITHOUT_NOT_FOUND = "{\n" +
            " \"records\" :[],\n" +
            " \"conversionStatuses\":[]\n" +
            "}";

    @Mock
    private UrlFetchServiceImpl urlFetchService;
    @Mock
    private FetchServiceHttpRequest httpRequest;
    @InjectMocks
    private HttpResponse response;
    @InjectMocks
    private RetryPolicy retryPolicy;


    @Test
    public void retry_should_be_true_for_json1() {
        RetryConfig config = this.retryPolicy.retryConfig();
        Predicate<HttpResponse> retry = config.getResultPredicate();
        response.setBody(JSON_RESPONSE_WITH_NOT_FOUND);
        assert retry != null;
        boolean value = retry.test(response);

        assertTrue(value);
    }

    @Test
    public void retry_should_be_false_for_json2() {
        RetryConfig config = this.retryPolicy.retryConfig();
        Predicate<HttpResponse> retry = config.getResultPredicate();
        response.setBody(JSON_RESPONSE1_WITHOUT_NOT_FOUND);
        boolean value = retry.test(response);

        assertFalse(value);
    }

    @Test
    public void retry_should_be_false_for_json3() {
        RetryConfig config = this.retryPolicy.retryConfig();
        Predicate<HttpResponse> retry = config.getResultPredicate();
        response.setBody(JSON_RESPONSE2_WITHOUT_NOT_FOUND);
        boolean value = retry.test(response);

        assertFalse(value);
    }

}
