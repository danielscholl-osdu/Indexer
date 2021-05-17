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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.http.FetchServiceHttpRequest;
import org.opengroup.osdu.core.common.http.UrlFetchServiceImpl;
import org.opengroup.osdu.core.common.model.http.HttpResponse;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class UrlFetchServiceAzureImplTest {

    @Mock
    private RetryPolicy retryPolicy;
    @Mock
    private UrlFetchServiceImpl urlFetchService;
    @InjectMocks
    private FetchServiceHttpRequest httpRequest;
    @InjectMocks
    private HttpResponse response;
    @InjectMocks
    private UrlFetchServiceAzureImpl urlFetchServiceAzure;

    private static final String JSON1 = "{\n" +
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

    private static final String JSON2 = "{\n" +
            " \"records\" :[],\n" +
            " \"conversionStatuses\":[]\n" +
            "}";

    private static final String url = "https://demo/api/storage/v2/query/records:batch";
    private static final String url2 = "https://demo/api/storage/v2/schemas";

    @Before
    public void setUp() {
        when(this.retryPolicy.retryConfig()).thenReturn(new RetryPolicy().retryConfig());
    }

    @Test
    public void shouldRetry_ForJSON1_when_storageQueryRecordCallIsMade() throws Exception {
        response.setBody(JSON1);
        httpRequest.setUrl(url);

        when(urlFetchServiceAzure.sendRequest(httpRequest)).thenReturn(response);

        urlFetchServiceAzure.sendRequest(httpRequest);
        verify(urlFetchService, atMost(4)).sendRequest(httpRequest);
    }

    @Test
    public void shouldNotRetry_ForJSON2_when_storageQueryRecordCallIsMade() throws Exception {
        response.setBody(JSON2);
        httpRequest.setUrl(url);

        when(urlFetchServiceAzure.sendRequest(httpRequest)).thenReturn(response);

        urlFetchServiceAzure.sendRequest(httpRequest);
        verify(urlFetchService, atMost(2)).sendRequest(httpRequest);
    }


    @Test
    public void retryFunction_shouldNotBeCalled() throws Exception {
        httpRequest.setUrl(url2);

        when(urlFetchService.sendRequest(httpRequest)).thenReturn(response);

        urlFetchServiceAzure.sendRequest(httpRequest);
        verify(urlFetchService, times(1)).sendRequest(httpRequest);
    }

}
