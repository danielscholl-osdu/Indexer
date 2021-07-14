// Copyright 2017-2020, Schlumberger
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

package org.opengroup.osdu.indexer.service;

import com.google.api.client.http.HttpMethods;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.http.FetchServiceHttpRequest;
import org.opengroup.osdu.core.common.http.IUrlFetchService;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.HttpResponse;
import org.opengroup.osdu.core.common.provider.interfaces.IRequestInfo;
import org.opengroup.osdu.indexer.config.IndexerConfigurationProperties;
import org.opengroup.osdu.indexer.schema.converter.exeption.SchemaProcessingException;
import org.opengroup.osdu.indexer.schema.converter.interfaces.SchemaToStorageFormat;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Provides implementation of the client service that retrieves schemas from the Schema Service
 */
@Component
public class SchemaProviderImpl implements SchemaService {
    @Inject
    private JaxRsDpsLog log;

    @Inject
    private IUrlFetchService urlFetchService;

    @Inject
    private IndexerConfigurationProperties configurationProperties;

    @Inject
    private IRequestInfo requestInfo;

    @Inject
    private SchemaToStorageFormat schemaToStorageFormat;

    @Inject
    private StorageService storageService;

    @Override
    public String getSchema(String kind) throws URISyntaxException, UnsupportedEncodingException {
        String schemaServiceSchema = getFromSchemaService(kind);
        return Objects.nonNull(schemaServiceSchema) ? schemaServiceSchema : getFromStorageService(kind);
    }

    protected String getFromSchemaService(String kind) throws UnsupportedEncodingException, URISyntaxException {
        HttpResponse response = getSchemaServiceResponse(kind);

        if (response.getResponseCode() == HttpStatus.SC_NOT_FOUND) {
            log.warning("Schema is not found on the Schema Service:" + kind);
            return null;
        }

        return response.getResponseCode() != HttpStatus.SC_OK ? null :
                schemaToStorageFormat.convertToString(response.getBody(), kind);
    }

    protected String getFromStorageService(String kind) throws URISyntaxException, UnsupportedEncodingException {
        String schemaFromStorageService = storageService.getStorageSchema(kind);

        if (schemaFromStorageService != null) {
            return schemaFromStorageService;
        }

        log.warning("Schema is not found on the Storage Service:" + kind);

        return null;
    }

    private HttpResponse getSchemaServiceResponse(String kind) throws UnsupportedEncodingException, URISyntaxException {
        String url = String.format("%s/%s", configurationProperties.getSchemaHost(), URLEncoder.encode(kind, StandardCharsets.UTF_8.toString()));
        FetchServiceHttpRequest request = FetchServiceHttpRequest.builder()
                .httpMethod(HttpMethods.GET)
                .headers(this.requestInfo.getHeadersMap())
                .url(url)
                .build();
        
        return this.urlFetchService.sendRequest(request);
    }
}
