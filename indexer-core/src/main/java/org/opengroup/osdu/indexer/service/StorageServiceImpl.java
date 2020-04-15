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

import com.google.api.client.http.HttpMethods;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import lombok.extern.java.Log;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.opengroup.osdu.core.common.model.http.DpsHeaders.FRAME_OF_REFERENCE;
import static org.opengroup.osdu.core.common.Constants.SLB_FRAME_OF_REFERENCE_VALUE;

@Log
@Component
public class StorageServiceImpl implements StorageService {

    private final Gson gson = new Gson();

    @Inject
    private IUrlFetchService urlFetchService;
    @Inject
    private JobStatus jobStatus;
    @Inject
    private IRequestInfo requestInfo;
    @Inject
    private JaxRsDpsLog jaxRsDpsLog;

    @Value("${STORAGE_SCHEMA_HOST}")
    private String STORAGE_SCHEMA_HOST;

    @Value("${STORAGE_QUERY_RECORD_HOST}")
    private String STORAGE_QUERY_RECORD_HOST;

    @Value("${STORAGE_QUERY_RECORD_FOR_CONVERSION_HOST}")
    private String STORAGE_QUERY_RECORD_FOR_CONVERSION_HOST;

    @Value("${STORAGE_RECORDS_BATCH_SIZE}")
    private String STORAGE_RECORDS_BATCH_SIZE;

    @Override
    public Records getStorageRecords(List<String> ids) throws AppException, URISyntaxException {
        List<Records.Entity> valid = new ArrayList<>();
        List<String> notFound = new ArrayList<>();
        List<ConversionStatus> conversionStatuses = new ArrayList<>();

        List<List<String>> batch = Lists.partition(ids, Integer.parseInt(STORAGE_RECORDS_BATCH_SIZE));
        for (List<String> recordsBatch : batch) {
            Records storageOut = this.getRecords(recordsBatch);
            valid.addAll(storageOut.getRecords());
            notFound.addAll(storageOut.getNotFound());
            conversionStatuses.addAll(storageOut.getConversionStatuses());
        }
        return Records.builder().records(valid).notFound(notFound).conversionStatuses(conversionStatuses).build();
    }

    private Records getRecords(List<String> ids) throws URISyntaxException {
        // e.g. {"records":["test:10"]}
        String body = this.gson.toJson(RecordIds.builder().records(ids).build());

//        Map<String, String> headers = this.requestInfo.getHeadersMap();
        DpsHeaders headers = this.requestInfo.getHeaders();
        headers.put(FRAME_OF_REFERENCE, SLB_FRAME_OF_REFERENCE_VALUE);
        HttpResponse response = this.urlFetchService.sendRequest(HttpMethods.POST, STORAGE_QUERY_RECORD_FOR_CONVERSION_HOST, headers, null, body);
        String dataFromStorage = response.getBody();
        if (Strings.isNullOrEmpty(dataFromStorage)) {
            throw new AppException(HttpStatus.SC_NOT_FOUND, "Invalid request", "Storage service cannot locate records");
        }

        Type recordsListType = new TypeToken<Records>() {}.getType();
        Records records = this.gson.fromJson(dataFromStorage, recordsListType);

        // update status for invalid records from storage
        if (records.getNotFound() != null && !records.getNotFound().isEmpty()) {
            this.jobStatus.addOrUpdateRecordStatus(records.getNotFound(), IndexingStatus.FAIL, RequestStatus.INVALID_RECORD, "invalid storage records", String.format("invalid records: %s", String.join(",", records.getNotFound())));
        }

        // don't proceed if there is nothing to process
        List<Records.Entity> validRecords = records.getRecords();
        if (validRecords == null || validRecords.isEmpty()) {
            if (response.isSuccessCode()) {
                throw new AppException(RequestStatus.INVALID_RECORD, "Invalid request", "Storage service returned retry or invalid records");
            }

            // TODO: returned actual code from storage service
            jaxRsDpsLog.warning(String.format("unable to proceed, valid storage record not found. | upstream response code: %s | record ids: %s", response.getResponseCode(), String.join(" | ", ids)));
            throw new AppException(HttpStatus.SC_NOT_FOUND, "Invalid request", "Storage service cannot locate valid records");
        }

        return records;
    }

    @Override
    public RecordQueryResponse getRecordsByKind(RecordReindexRequest request) throws URISyntaxException {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put(RecordMetaAttribute.KIND.getValue(), request.getKind());
        queryParams.put("limit", STORAGE_RECORDS_BATCH_SIZE);
        if (!Strings.isNullOrEmpty(request.getCursor())) {
            queryParams.put("cursor", request.getCursor());
        }

        if(requestInfo == null)
            throw  new AppException(HttpStatus.SC_NO_CONTENT, "Invalid header", "header can't be null");

        HttpResponse response = this.urlFetchService.sendRequest(HttpMethods.GET, STORAGE_QUERY_RECORD_HOST, this.requestInfo.getHeaders(), queryParams, null);
        return this.gson.fromJson(response.getBody(), RecordQueryResponse.class);
    }

    @Override
    public String getStorageSchema(String kind) throws URISyntaxException, UnsupportedEncodingException {
        String url = String.format("%s/%s", STORAGE_SCHEMA_HOST, URLEncoder.encode(kind, "UTF-8"));
        HttpResponse response = this.urlFetchService.sendRequest(HttpMethods.GET, url, this.requestInfo.getHeaders(), null, null);
        if (response.getResponseCode() != HttpStatus.SC_OK) return null;
        return response.getBody();
    }
}