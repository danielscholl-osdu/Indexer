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
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.client.RestHighLevelClient;
import org.opengroup.osdu.core.common.http.FetchServiceHttpRequest;
import org.opengroup.osdu.core.common.http.IUrlFetchService;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.HttpResponse;
import org.opengroup.osdu.core.common.model.indexer.SchemaInfo;
import org.opengroup.osdu.core.common.model.indexer.SchemaOperationType;
import org.opengroup.osdu.core.common.provider.interfaces.IRequestInfo;
import org.opengroup.osdu.indexer.config.IndexerConfigurationProperties;
import org.opengroup.osdu.indexer.schema.converter.interfaces.SchemaToStorageFormat;
import org.opengroup.osdu.indexer.util.ElasticClientHandler;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @Inject
    private IndexSchemaService indexSchemaService;

    @Inject
    private ElasticClientHandler elasticClientHandler;

    @Override
    public String getSchema(String kind) throws URISyntaxException, UnsupportedEncodingException {
        String schemaServiceSchema = getFromSchemaService(kind);
        return Objects.nonNull(schemaServiceSchema) ? schemaServiceSchema : getFromStorageService(kind);
    }

    @Override
    public void processSchemaMessages(List<SchemaInfo> schemaInfos) throws IOException {
        Map<String, SchemaOperationType> messages = new HashMap<>();
        Map<String, SchemaOperationType> createSchemaMessages = SchemaInfo.getCreateSchemaEvents(schemaInfos);
        if (createSchemaMessages != null) {
            messages.putAll(createSchemaMessages);
        }

        Map<String, SchemaOperationType> updateSchemaMessages = SchemaInfo.getUpdateSchemaEvents(schemaInfos);
        if (updateSchemaMessages != null) {
            messages.putAll(updateSchemaMessages);
        }

        if (messages.isEmpty()) {
            return;
        }

        try (RestHighLevelClient restClient = this.elasticClientHandler.createRestClient()) {
            messages.entrySet().forEach(msg -> {
                try {
                    this.indexSchemaService.processSchemaUpsertEvent(restClient, msg.getKey());
                } catch (IOException | ElasticsearchStatusException | URISyntaxException e) {
                    throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "unable to process schema update", e.getMessage());
                }
            });
        }
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
