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

package org.opengroup.osdu.indexer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.HttpMethods;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gson.Gson;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.opengroup.osdu.core.common.http.FetchServiceHttpRequest;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.HttpResponse;
import org.opengroup.osdu.core.common.model.http.RequestStatus;
import org.opengroup.osdu.core.common.model.indexer.*;
import org.opengroup.osdu.core.common.model.storage.ConversionStatus;
import org.opengroup.osdu.core.common.model.storage.RecordIds;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.http.IUrlFetchService;
import org.opengroup.osdu.core.common.model.search.RecordMetaAttribute;
import org.opengroup.osdu.core.common.provider.interfaces.IRequestInfo;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.indexer.config.IndexerConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.opengroup.osdu.core.common.model.http.DpsHeaders.FRAME_OF_REFERENCE;
import static org.opengroup.osdu.core.common.Constants.SLB_FRAME_OF_REFERENCE_VALUE;

@Component
public class StorageServiceImpl implements StorageService {

    private final Gson gson = new Gson();

    @Inject
    private ObjectMapper objectMapper;
    @Inject
    private IUrlFetchService urlFetchService;
    @Inject
    private JobStatus jobStatus;
    @Inject
    private IRequestInfo requestInfo;
    @Inject
    private JaxRsDpsLog jaxRsDpsLog;
    @Inject
    private IndexerConfigurationProperties configurationProperties;

    @Override
    public Records getStorageRecords(List<String> ids) throws AppException, URISyntaxException {
        List<Records.Entity> valid = new ArrayList<>();
        List<String> notFound = new ArrayList<>();
        List<ConversionStatus> conversionStatuses = new ArrayList<>();
        List<String> missingRetryRecordIds = new ArrayList<>();

        List<List<String>> batch = Lists.partition(ids, configurationProperties.getStorageRecordsBatchSize());
        for (List<String> recordsBatch : batch) {
            Records storageOut = this.getRecords(recordsBatch);
            valid.addAll(storageOut.getRecords());
            notFound.addAll(storageOut.getNotFound());
            conversionStatuses.addAll(storageOut.getConversionStatuses());
            missingRetryRecordIds.addAll(storageOut.getMissingRetryRecords());
        }
        return Records.builder().records(valid).notFound(notFound).conversionStatuses(conversionStatuses).missingRetryRecords(missingRetryRecordIds).build();
    }

    protected Records getRecords(List<String> ids) throws URISyntaxException {
        // e.g. {"records":["test:10"]}
        String body = this.gson.toJson(RecordIds.builder().records(ids).build());

//        Map<String, String> headers = this.requestInfo.getHeadersMap();
        DpsHeaders headers = this.requestInfo.getHeaders();
        headers.put(FRAME_OF_REFERENCE, SLB_FRAME_OF_REFERENCE_VALUE);
        FetchServiceHttpRequest request = FetchServiceHttpRequest
                .builder()
                .httpMethod(HttpMethods.POST)
                .url(configurationProperties.getStorageQueryRecordForConversionHost())
                .headers(headers)
                .body(body).build();
        HttpResponse response = this.urlFetchService.sendRequest(request);
        return this.validateStorageResponse(response, ids);
    }

    private Records validateStorageResponse(HttpResponse response, List<String> ids) {
        String bulkStorageData = response.getBody();

        // retry entire payload -- storage service returned empty response
        if (Strings.isNullOrEmpty(bulkStorageData)) {
            throw new AppException(HttpStatus.SC_NOT_FOUND, "Invalid request", "Storage service returned empty response");
        }

        if (response.getResponseCode() == 500) {
            throw new AppException(RequestStatus.NO_RETRY, "Server error", String.format("Storage service error: %s", response.getBody()));
        }

        Records records = null;
        try {
            records = this.objectMapper.readValue(bulkStorageData, Records.class);
        } catch (JsonProcessingException e) {
            throw new AppException(RequestStatus.INVALID_RECORD, "Invalid request", "Successful Storage service response with wrong json", e);
        }

        // no retry possible, update record status as failed -- storage service cannot locate records
        if (!records.getNotFound().isEmpty()) {
            this.jobStatus.addOrUpdateRecordStatus(records.getNotFound(), IndexingStatus.FAIL, RequestStatus.INVALID_RECORD, "Storage service records not found", String.format("Storage service records not found: %s", String.join(",", records.getNotFound())));
        }

        List<Records.Entity> validRecords = records.getRecords();
        if (validRecords.isEmpty()) {
            // no need to retry, ack the CloudTask message -- nothing to process from RecordChangeMessage batch
            if (response.isSuccessCode()) {
                throw new AppException(RequestStatus.INVALID_RECORD, "Invalid request", "Successful Storage service response with no valid records");
            }

            // retry entire payload -- storage service returned empty valid records with non-success response-code
            jaxRsDpsLog.warning(String.format("unable to proceed, valid storage record not found. | upstream response code: %s | record ids: %s", response.getResponseCode(), String.join(" | ", ids)));
            throw new AppException(HttpStatus.SC_NOT_FOUND, "Invalid request", "Storage service error");
        }

        Map<String, List<String>> conversionStatus = getConversionErrors(records.getConversionStatuses());
        for (Records.Entity storageRecord : validRecords) {
            String recordId = storageRecord.getId();
            if (conversionStatus.get(recordId) == null) {
                continue;
            }
            for (String status : conversionStatus.get(recordId)) {
                this.jobStatus.addOrUpdateRecordStatus(recordId, IndexingStatus.WARN, HttpStatus.SC_BAD_REQUEST, status, String.format("record-id: %s | %s", recordId, status));
            }
        }

        // retry missing records -- storage did not return response for all RecordChangeMessage record-ids
        if (records.getTotalRecordCount() != ids.size()) {
            List<String> missingRecords = this.getMissingRecords(records, ids);
            records.setMissingRetryRecords(missingRecords);
            this.jobStatus.addOrUpdateRecordStatus(missingRecords, IndexingStatus.FAIL, HttpStatus.SC_NOT_FOUND, "Partial response received from Storage service - missing records", String.format("Partial response received from Storage service: %s", String.join(",", missingRecords)));
        }

        return records;
    }

    private List<String> getMissingRecords(Records records, List<String> ids) {
        List<String> validRecordIds = records.getRecords().stream().map(Records.Entity::getId).collect(Collectors.toList());
        List<String> invalidRecordsIds = records.getNotFound();
        List<String> requestedIds = new ArrayList<>(ids);
        requestedIds.removeAll(validRecordIds);
        requestedIds.removeAll(invalidRecordsIds);
        return requestedIds;
    }

    private Map<String, List<String>> getConversionErrors(List<ConversionStatus> conversionStatuses) {
        Map<String, List<String>> errorsByRecordId = new HashMap<>();
        for (ConversionStatus conversionStatus : conversionStatuses) {
            if (Strings.isNullOrEmpty(conversionStatus.getStatus())) continue;
            if (conversionStatus.getStatus().equalsIgnoreCase("ERROR")) {
                List<String> statuses = errorsByRecordId.getOrDefault(conversionStatus.getId(), new LinkedList<>());
                statuses.addAll(conversionStatus.getErrors());
                errorsByRecordId.put(conversionStatus.getId(), statuses);
            }
        }
        return errorsByRecordId;
    }

    @Override
    public RecordQueryResponse getRecordsByKind(RecordReindexRequest reindexRequest) throws URISyntaxException {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put(RecordMetaAttribute.KIND.getValue(), reindexRequest.getKind());
        queryParams.put("limit", configurationProperties.getStorageRecordsByKindBatchSize().toString());
        if (!Strings.isNullOrEmpty(reindexRequest.getCursor())) {
            queryParams.put("cursor", reindexRequest.getCursor());
        }

        if(requestInfo == null)
            throw  new AppException(HttpStatus.SC_NO_CONTENT, "Invalid header", "header can't be null");

        FetchServiceHttpRequest request = FetchServiceHttpRequest.builder()
                .httpMethod(HttpMethods.GET)
                .headers(this.requestInfo.getHeadersMap())
                .url(configurationProperties.getStorageQueryRecordHost())
                .queryParams(queryParams)
                .build();

        HttpResponse response = this.urlFetchService.sendRequest(request);
        return this.gson.fromJson(response.getBody(), RecordQueryResponse.class);
    }

    @Override
    public String getStorageSchema(String kind) throws URISyntaxException, UnsupportedEncodingException {
        String url = String.format("%s/%s", configurationProperties.getStorageSchemaHost(), URLEncoder.encode(kind, StandardCharsets.UTF_8.toString()));
        FetchServiceHttpRequest request = FetchServiceHttpRequest.builder()
                .httpMethod(HttpMethods.GET)
                .headers(this.requestInfo.getHeadersMap())
                .url(url)
                .build();
        HttpResponse response = this.urlFetchService.sendRequest(request);
        return response.getResponseCode() != HttpStatus.SC_OK ? null : response.getBody();
    }

    @Override
    public List<String> getAllKinds() throws URISyntaxException {
        String url = configurationProperties.getStorageQueryKindsHost();
        FetchServiceHttpRequest request = FetchServiceHttpRequest.builder()
            .httpMethod(HttpMethods.GET)
            .headers(this.requestInfo.getHeadersMap())
            .url(url)
            .build();
        HttpResponse response = this.urlFetchService.sendRequest(request);
        JsonObject asJsonObject = new JsonParser().parse(response.getBody()).getAsJsonObject();
        JsonElement results = asJsonObject.get("results");
        return response.getResponseCode() != HttpStatus.SC_OK ? null : this.gson.fromJson(results,List.class);
    }
}
