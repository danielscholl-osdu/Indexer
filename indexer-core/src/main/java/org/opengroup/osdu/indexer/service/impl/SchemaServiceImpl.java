// Copyright 2017-2019, Schlumberger
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.indexer.service.impl;

import com.google.api.client.http.HttpMethods;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.http.FetchServiceHttpRequest;
import org.opengroup.osdu.core.common.http.IUrlFetchService;
import org.opengroup.osdu.core.common.model.http.HttpResponse;
import org.opengroup.osdu.core.common.provider.interfaces.IRequestInfo;
import org.opengroup.osdu.indexer.service.SchemaService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Provides implementation of the Service that retrieves schemas from the Schema Service
 */
@Component
public class SchemaServiceImpl implements SchemaService {
    @Inject
    private IUrlFetchService urlFetchService;

    @Value("${STORAGE_SCHEMA_HOST}")
    private String STORAGE_SCHEMA_HOST;

    @Inject
    private IRequestInfo requestInfo;

    @Override
    public String getSchema(String kind) throws URISyntaxException, UnsupportedEncodingException {
        // this is temporary implementation that still uses storage service
        String url = String.format("%s/%s", STORAGE_SCHEMA_HOST, URLEncoder.encode(kind, StandardCharsets.UTF_8.toString()));
        FetchServiceHttpRequest request = FetchServiceHttpRequest.builder()
                .httpMethod(HttpMethods.GET)
                .headers(this.requestInfo.getHeadersMap())
                .url(url)
                .build();
        HttpResponse response = this.urlFetchService.sendRequest(request);
        return response.getResponseCode() != HttpStatus.SC_OK ? null : response.getBody();
    }
}
