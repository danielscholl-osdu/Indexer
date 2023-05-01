/*
 * Copyright © Schlumberger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.indexer.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.SneakyThrows;
import org.apache.commons.lang.NotImplementedException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.indexer.OperationType;
import org.opengroup.osdu.core.common.model.indexer.RecordInfo;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.core.common.model.storage.Schema;
import org.opengroup.osdu.core.common.provider.interfaces.IRequestInfo;
import org.opengroup.osdu.indexer.cache.*;
import org.opengroup.osdu.indexer.config.IndexerConfigurationProperties;
import org.opengroup.osdu.indexer.model.*;
import org.opengroup.osdu.indexer.model.indexproperty.PropertyConfigurations;
import org.opengroup.osdu.indexer.model.indexproperty.RelatedObjectsSpec;
import org.opengroup.osdu.indexer.model.indexproperty.ValueExtraction;
import org.opengroup.osdu.indexer.util.IndexerQueueTaskBuilder;
import org.opengroup.osdu.indexer.util.PropertyUtil;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(SpringRunner.class)
public class PropertyConfigurationsServiceImplTest {
    private final Gson gson = new Gson();

    @InjectMocks
    private PropertyConfigurationsServiceImpl sut;

    @Mock
    private IndexerConfigurationProperties configurationProperties;
    @Mock
    private PartitionSafePropertyConfigurationsCache propertyConfigurationCache;
    @Mock
    private PartitionSafePropertyConfigurationsEnabledCache propertyConfigurationsEnabledCache;
    @Mock
    private PartitionSafeParentChildRelationshipSpecsCache parentChildRelationshipSpecsCache;
    @Mock
    private PartitionSafeKindCache kindCache;
    @Mock
    private IRelatedObjectCache relatedObjectCache;
    @Mock
    private IRecordChangeInfoCache recordChangeInfoCache;
    @Mock
    private SearchService searchService;
    @Mock
    private SchemaService schemaService;
    @Mock
    private IndexerQueueTaskBuilder indexerQueueTaskBuilder;
    @Mock
    private IRequestInfo requestInfo;
    @Mock
    private JaxRsDpsLog jaxRsDpsLog;

    private final String propertyConfigurationKind = "osdu:wks:reference-data--IndexPropertyPathConfiguration:*";
    private String childKind;
    private String childId;
    private String parentKind;
    private String parentId;

    @Test
    public void isPropertyConfigurationsEnabled_invalid_kind() {
        Assert.assertFalse(sut.isPropertyConfigurationsEnabled(null));
        Assert.assertFalse(sut.isPropertyConfigurationsEnabled(""));
        Assert.assertFalse(sut.isPropertyConfigurationsEnabled("anyAuth:anySource:anyEntity"));
    }

    @Test
    public void isPropertyConfigurationsEnabled_with_value_true_in_cache() {
        String kind = "anyAuth:anySource:anyEntity:1.";
        when(this.propertyConfigurationsEnabledCache.get(any())).thenReturn(true);
        Assert.assertTrue(sut.isPropertyConfigurationsEnabled(kind));
        verify(this.propertyConfigurationsEnabledCache, times(0)).put(any(), any());
    }

    @Test
    public void isPropertyConfigurationsEnabled_with_value_false_in_cache() {
        String kind = "anyAuth:anySource:anyEntity:1.";
        when(this.propertyConfigurationsEnabledCache.get(any())).thenReturn(false);
        Assert.assertFalse(sut.isPropertyConfigurationsEnabled(kind));
        verify(this.propertyConfigurationsEnabledCache, times(0)).put(any(), any());
    }

    @Test
    public void isPropertyConfigurationsEnabled_with_result_from_search() throws URISyntaxException {
        String kind = "anyAuth:anySource:anyEntity:1.";
        SearchResponse response = new SearchResponse();
        response.setResults(Arrays.asList(new SearchRecord()));
        when(this.propertyConfigurationsEnabledCache.get(any())).thenReturn(null);
        when(this.searchService.query(any())).thenReturn(response);
        Assert.assertTrue(sut.isPropertyConfigurationsEnabled(kind));
        verify(this.propertyConfigurationsEnabledCache, times(1)).put(any(), any());
    }

    @Test
    public void isPropertyConfigurationsEnabled_without_result_from_search() throws URISyntaxException {
        String kind = "anyAuth:anySource:anyEntity:1.";
        SearchResponse response = new SearchResponse();
        when(this.propertyConfigurationsEnabledCache.get(any())).thenReturn(null);
        when(this.searchService.query(any())).thenReturn(response);
        Assert.assertFalse(sut.isPropertyConfigurationsEnabled(kind));
        verify(this.propertyConfigurationsEnabledCache, times(1)).put(any(), any());
    }

    @Test
    public void getPropertyConfigurations_invalid_kind() {
        Assert.assertNull(sut.getPropertyConfigurations(null));
        Assert.assertNull(sut.getPropertyConfigurations(""));
        Assert.assertNull(sut.getPropertyConfigurations("anyAuth:anySource:anyEntity"));
    }

    @Test
    public void getPropertyConfigurations_with_configuration_in_cache() {
        String code = "anyAuth:anySource:anyEntity:1.";
        String kind = "anyAuth:anySource:anyEntity:1.0.0";
        PropertyConfigurations configuration = new PropertyConfigurations();
        configuration.setCode(code);
        when(this.propertyConfigurationCache.get(eq(code))).thenReturn(configuration);
        PropertyConfigurations configuration2 = sut.getPropertyConfigurations(kind);

        Assert.assertNotNull(configuration2);
        Assert.assertEquals(code, configuration2.getCode());
    }

    @Test
    public void getPropertyConfigurations_with_empty_configuration_in_cache() {
        String code = "anyAuth:anySource:anyEntity:1.";
        String kind = "anyAuth:anySource:anyEntity:1.0.0";
        PropertyConfigurations configuration = new PropertyConfigurations();
        when(this.propertyConfigurationCache.get(eq(code))).thenReturn(configuration);
        PropertyConfigurations configuration2 = sut.getPropertyConfigurations(kind);

        Assert.assertNull(configuration2);
    }

    @Test
    public void getPropertyConfigurations_with_result_from_search() throws URISyntaxException {
        Map<String, Object> data = this.getDataMap("well_configuration_record.json");
        SearchRecord searchRecord = new SearchRecord();
        searchRecord.setData(data);
        List<SearchRecord> results = Arrays.asList(searchRecord);
        SearchResponse searchResponse = new SearchResponse();
        searchResponse.setResults(results);
        searchResponse.setTotalCount(results.size());
        when(this.searchService.queryWithCursor(any())).thenReturn(searchResponse);

        String kind = "osdu:wks:master-data--Well:1.0.0";
        String code = "osdu:wks:master-data--Well:1.";
        PropertyConfigurations configuration = sut.getPropertyConfigurations(kind);

        ArgumentCaptor<PropertyConfigurations> argumentCaptor = ArgumentCaptor.forClass(PropertyConfigurations.class);
        verify(this.propertyConfigurationCache, times(1)).put(any(), argumentCaptor.capture());
        Assert.assertNotNull(configuration);
        Assert.assertEquals(code, configuration.getCode());
        Assert.assertEquals(code, argumentCaptor.getValue().getCode());
    }

    @Test
    public void getPropertyConfigurations_without_result_from_search() throws URISyntaxException {
        when(this.searchService.queryWithCursor(any())).thenReturn(new SearchResponse());

        String kind = "osdu:wks:master-data--Well:1.0.0";
        PropertyConfigurations configuration = sut.getPropertyConfigurations(kind);

        ArgumentCaptor<PropertyConfigurations> argumentCaptor = ArgumentCaptor.forClass(PropertyConfigurations.class);
        verify(this.propertyConfigurationCache, times(1)).put(any(), argumentCaptor.capture());
        Assert.assertNull(configuration);
        Assert.assertNull(argumentCaptor.getValue().getCode());
    }

    @Test
    public void getExtendedProperties() {
        //TODO
    }

    @Test
    public void getExtendedSchemaItems() throws JsonProcessingException {
        PropertyConfigurations propertyConfigurations = getConfigurations("well_configuration_record.json");
        //TODO
    }

    @Test
    public void resolveConcreteKind_with_concreteKind() {
        String kind = "osdu:wks:master-data--Well:1.0.0";
        Assert.assertEquals(kind, sut.resolveConcreteKind(kind));
    }

    @Test
    public void resolveConcreteKind_with_null_empty_kind() {
        Assert.assertTrue(Strings.isNullOrEmpty(sut.resolveConcreteKind(null)));
        Assert.assertTrue(Strings.isNullOrEmpty(sut.resolveConcreteKind("")));
    }

    @Test
    public void resolveConcreteKind_with_value_in_cache() {
        String kind = "osdu:wks:master-data--Well:1.";
        String expectedKind = kind + "2.3";

        when(this.kindCache.get(any())).thenReturn(expectedKind);
        Assert.assertEquals(expectedKind, sut.resolveConcreteKind(kind));
    }

    @Test
    public void resolveConcreteKind_with_result_from_schemaService() throws UnsupportedEncodingException, URISyntaxException {
        String kind = "osdu:wks:master-data--Well:1.";
        String expectedKind = kind + "2.3";

        SchemaIdentity schemaIdentity = new SchemaIdentity();
        schemaIdentity.setAuthority("osdu");
        schemaIdentity.setSource("wks");
        schemaIdentity.setEntityType("master-data--Well");
        schemaIdentity.setSchemaVersionMajor(1);
        schemaIdentity.setSchemaVersionMinor(2);
        schemaIdentity.setSchemaVersionPatch(3);
        SchemaInfo schemaInfo = new SchemaInfo();
        schemaInfo.setSchemaIdentity(schemaIdentity);
        List<SchemaInfo> schemaInfos = Arrays.asList(schemaInfo);
        SchemaInfoResponse response = new SchemaInfoResponse();
        response.setSchemaInfos(schemaInfos);
        response.setTotalCount(schemaInfos.size());
        when(this.schemaService.getSchemaInfos(any(), any(), any(), any(), eq(null), eq(null), eq(true))).thenReturn(response);
        String latestKind = sut.resolveConcreteKind(kind);

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(this.kindCache, times(1)).put(any(), argumentCaptor.capture());
        Assert.assertEquals(expectedKind, latestKind);
        Assert.assertEquals(expectedKind, argumentCaptor.getValue());
    }

    @Test
    public void resolveConcreteKind_without_result_from_schemaService() throws UnsupportedEncodingException, URISyntaxException {
        String kind = "osdu:wks:master-data--Well:1.";

        SchemaInfoResponse response = new SchemaInfoResponse();
        when(this.schemaService.getSchemaInfos(any(), any(), any(), any(), eq(null), eq(null), eq(true))).thenReturn(response);
        String latestKind = sut.resolveConcreteKind(kind);

        verify(this.kindCache, times(0)).put(any(), any());
        Assert.assertNull(latestKind);
    }

    @Test
    public void cacheDataRecord_create_record() throws URISyntaxException {
        ArgumentCaptor<RecordChangeInfo> recordInfoArgumentCaptor = ArgumentCaptor.forClass(RecordChangeInfo.class);
        ArgumentCaptor<Map<String, Object>> dataMapArgumentCaptor = ArgumentCaptor.forClass(Map.class);
        String recordId = "anyId";
        String kind = "anyKind";
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("p1", "v1");

        when(this.searchService.query(any())).thenReturn(new SearchResponse());

        this.sut.cacheDataRecord(recordId, kind, dataMap);

        verify(this.recordChangeInfoCache, times(1)).put(any(), recordInfoArgumentCaptor.capture());
        verify(this.relatedObjectCache, times(1)).put(any(), dataMapArgumentCaptor.capture());

        Assert.assertEquals(OperationType.create.getValue(), recordInfoArgumentCaptor.getValue().getRecordInfo().getOp());
        Assert.assertEquals(1, dataMapArgumentCaptor.getValue().size());
    }

    @Test
    public void cacheDataRecord_update_record() throws URISyntaxException {
        ArgumentCaptor<RecordChangeInfo> recordInfoArgumentCaptor = ArgumentCaptor.forClass(RecordChangeInfo.class);
        ArgumentCaptor<Map<String, Object>> dataMapArgumentCaptor = ArgumentCaptor.forClass(Map.class);
        String recordId = "anyId";
        String kind = "anyKind";
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("p1", "v1");
        Map<String, Object> previousDataMap = new HashMap<>();
        previousDataMap.put("p1", "v10");
        previousDataMap.put("p2", "v2");

        SearchRecord searchRecord = new SearchRecord();
        searchRecord.setKind(kind);
        searchRecord.setId(recordId);
        searchRecord.setData(previousDataMap);
        SearchResponse searchResponse = new SearchResponse();
        searchResponse.setResults(Arrays.asList(searchRecord));
        searchResponse.setTotalCount(1);
        when(this.searchService.query(any())).thenReturn(searchResponse);

        this.sut.cacheDataRecord(recordId, kind, dataMap);

        verify(this.recordChangeInfoCache, times(1)).put(any(), recordInfoArgumentCaptor.capture());
        verify(this.relatedObjectCache, times(2)).put(any(), dataMapArgumentCaptor.capture());

        RecordChangeInfo changedInfo = recordInfoArgumentCaptor.getValue();
        Assert.assertEquals(OperationType.update.getValue(), changedInfo.getRecordInfo().getOp());
        Assert.assertEquals(2, changedInfo.getUpdatedProperties().size());
        Assert.assertTrue(changedInfo.getUpdatedProperties().contains("p1"));
        Assert.assertTrue(changedInfo.getUpdatedProperties().contains("p2"));
        Assert.assertEquals(1, dataMapArgumentCaptor.getValue().size());
        Assert.assertEquals("v1", dataMapArgumentCaptor.getValue().get("p1"));
    }

    @Test
    public void cacheDataRecord_update_record_merge_previous_UpdateChangedInfo() throws URISyntaxException {
        ArgumentCaptor<RecordChangeInfo> recordInfoArgumentCaptor = ArgumentCaptor.forClass(RecordChangeInfo.class);
        ArgumentCaptor<Map<String, Object>> dataMapArgumentCaptor = ArgumentCaptor.forClass(Map.class);
        String recordId = "anyId";
        String kind = "anyKind";
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("p1", "v1");
        Map<String, Object> previousDataMap = new HashMap<>();
        previousDataMap.put("p1", "v1");
        previousDataMap.put("p2", "v2");

        RecordChangeInfo previousChangedInfo = new RecordChangeInfo();
        RecordInfo recordInfo = new RecordInfo();
        recordInfo.setId(recordId);
        recordInfo.setKind(kind);
        recordInfo.setOp(OperationType.update.getValue());
        previousChangedInfo.setRecordInfo(recordInfo);
        previousChangedInfo.setUpdatedProperties(Arrays.asList("p1"));

        SearchRecord searchRecord = new SearchRecord();
        searchRecord.setKind(kind);
        searchRecord.setId(recordId);
        searchRecord.setData(previousDataMap);
        SearchResponse searchResponse = new SearchResponse();
        searchResponse.setResults(Arrays.asList(searchRecord));
        searchResponse.setTotalCount(1);

        when(this.searchService.query(any())).thenReturn(searchResponse);
        when(this.recordChangeInfoCache.get(any())).thenReturn(previousChangedInfo);

        this.sut.cacheDataRecord(recordId, kind, dataMap);

        verify(this.recordChangeInfoCache, times(1)).put(any(), recordInfoArgumentCaptor.capture());
        verify(this.relatedObjectCache, times(2)).put(any(), dataMapArgumentCaptor.capture());

        RecordChangeInfo changedInfo = recordInfoArgumentCaptor.getValue();
        Assert.assertEquals(OperationType.update.getValue(), changedInfo.getRecordInfo().getOp());
        Assert.assertEquals(2, changedInfo.getUpdatedProperties().size());
        Assert.assertTrue(changedInfo.getUpdatedProperties().contains("p1"));
        Assert.assertTrue(changedInfo.getUpdatedProperties().contains("p2"));
        Assert.assertEquals(1, dataMapArgumentCaptor.getValue().size());
        Assert.assertEquals("v1", dataMapArgumentCaptor.getValue().get("p1"));
    }

    @Test
    public void cacheDataRecord_update_record_merge_previous_CreateChangedInfo() throws URISyntaxException {
        ArgumentCaptor<RecordChangeInfo> recordInfoArgumentCaptor = ArgumentCaptor.forClass(RecordChangeInfo.class);
        ArgumentCaptor<Map<String, Object>> dataMapArgumentCaptor = ArgumentCaptor.forClass(Map.class);
        String recordId = "anyId";
        String kind = "anyKind";
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("p1", "v1");
        Map<String, Object> previousDataMap = new HashMap<>();
        previousDataMap.put("p1", "v1");
        previousDataMap.put("p2", "v2");

        RecordChangeInfo previousChangedInfo = new RecordChangeInfo();
        RecordInfo recordInfo = new RecordInfo();
        recordInfo.setId(recordId);
        recordInfo.setKind(kind);
        recordInfo.setOp(OperationType.create.getValue());
        previousChangedInfo.setRecordInfo(recordInfo);

        SearchRecord searchRecord = new SearchRecord();
        searchRecord.setKind(kind);
        searchRecord.setId(recordId);
        searchRecord.setData(previousDataMap);
        SearchResponse searchResponse = new SearchResponse();
        searchResponse.setResults(Arrays.asList(searchRecord));
        searchResponse.setTotalCount(1);

        when(this.searchService.query(any())).thenReturn(searchResponse);
        when(this.recordChangeInfoCache.get(any())).thenReturn(previousChangedInfo);

        this.sut.cacheDataRecord(recordId, kind, dataMap);

        verify(this.recordChangeInfoCache, times(1)).put(any(), recordInfoArgumentCaptor.capture());
        verify(this.relatedObjectCache, times(2)).put(any(), dataMapArgumentCaptor.capture());

        RecordChangeInfo changedInfo = recordInfoArgumentCaptor.getValue();
        Assert.assertEquals(OperationType.create.getValue(), changedInfo.getRecordInfo().getOp());
        Assert.assertNull(changedInfo.getUpdatedProperties());
        Assert.assertEquals(1, dataMapArgumentCaptor.getValue().size());
        Assert.assertEquals("v1", dataMapArgumentCaptor.getValue().get("p1"));
    }

    @Test
    public void updateAssociatedRecords_updateAssociatedParentRecords_for_created_childRecord() throws URISyntaxException {
        updateAssociatedRecords_updateAssociatedParentRecords_for_created_delete(OperationType.create);
    }

    @Test
    public void updateAssociatedRecords_updateAssociatedParentRecords_for_deleted_childRecord() throws URISyntaxException {
        updateAssociatedRecords_updateAssociatedParentRecords_for_created_delete(OperationType.delete);
    }

    private void updateAssociatedRecords_updateAssociatedParentRecords_for_created_delete(OperationType operationType) throws URISyntaxException {
        updateAssociatedRecords_updateAssociatedParentRecords_baseSetup();

        // Test
        RecordChangedMessages recordChangedMessages = new RecordChangedMessages();
        recordChangedMessages.setAttributes(new HashMap<>());
        Map<String, List<String>> upsertKindIds = new HashMap<>();
        Map<String, List<String>> deleteKindIds = new HashMap<>();
        if(operationType == OperationType.create)
            upsertKindIds.put(childKind, Arrays.asList(childId));
        else
            deleteKindIds.put(childKind, Arrays.asList(childId));
        this.sut.updateAssociatedRecords(recordChangedMessages, upsertKindIds, deleteKindIds);

        // Verify
        ArgumentCaptor<String> payloadArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(this.indexerQueueTaskBuilder,times(1)).createWorkerTask(payloadArgumentCaptor.capture(), any(), any());

        RecordChangedMessages newMessages = gson.fromJson(payloadArgumentCaptor.getValue(), RecordChangedMessages.class);
        Type type = new TypeToken<List<RecordInfo>>() {}.getType();
        List<RecordInfo> infoList = gson.fromJson(newMessages.getData(), type);
        Assert.assertEquals(childKind, newMessages.getAttributes().get(Constants.ANCESTRY_KINDS));
        Assert.assertEquals(1, infoList.size());
        Assert.assertEquals(parentKind, infoList.get(0).getKind());
        Assert.assertEquals(parentId, infoList.get(0).getId());
    }

    @Test
    public void updateAssociatedRecords_updateAssociatedParentRecords_for_updated_childRecord_with_extendedPropertyChanged() throws URISyntaxException {
        updateAssociatedRecords_updateAssociatedParentRecords_baseSetup();

        RecordChangeInfo recordChangeInfo = new RecordChangeInfo();
        RecordInfo recordInfo = new RecordInfo();
        recordInfo.setKind(childKind);
        recordInfo.setId(childId);
        recordInfo.setOp(OperationType.update.getValue());
        recordChangeInfo.setRecordInfo(recordInfo);
        recordChangeInfo.setUpdatedProperties(Arrays.asList("Curves[].Mnemonic", "Name"));
        when(this.recordChangeInfoCache.get(any())).thenReturn(recordChangeInfo);

        // Test
        RecordChangedMessages recordChangedMessages = new RecordChangedMessages();
        recordChangedMessages.setAttributes(new HashMap<>());
        Map<String, List<String>> upsertKindIds = new HashMap<>();
        Map<String, List<String>> deleteKindIds = new HashMap<>();
        upsertKindIds.put(childKind, Arrays.asList(childId));
        this.sut.updateAssociatedRecords(recordChangedMessages, upsertKindIds, deleteKindIds);

        // Verify
        ArgumentCaptor<String> payloadArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(this.indexerQueueTaskBuilder,times(1)).createWorkerTask(payloadArgumentCaptor.capture(), any(), any());

        RecordChangedMessages newMessages = gson.fromJson(payloadArgumentCaptor.getValue(), RecordChangedMessages.class);
        Type type = new TypeToken<List<RecordInfo>>() {}.getType();
        List<RecordInfo> infoList = gson.fromJson(newMessages.getData(), type);
        Assert.assertEquals(childKind, newMessages.getAttributes().get(Constants.ANCESTRY_KINDS));
        Assert.assertEquals(1, infoList.size());
        Assert.assertEquals(parentKind, infoList.get(0).getKind());
        Assert.assertEquals(parentId, infoList.get(0).getId());
    }

    @Test
    public void updateAssociatedRecords_updateAssociatedParentRecords_for_updated_childRecord_without_extendedPropertyChanged() throws URISyntaxException {
        updateAssociatedRecords_updateAssociatedParentRecords_baseSetup();

        RecordChangeInfo recordChangeInfo = new RecordChangeInfo();
        RecordInfo recordInfo = new RecordInfo();
        recordInfo.setKind(childKind);
        recordInfo.setId(childId);
        recordInfo.setOp(OperationType.update.getValue());
        recordChangeInfo.setRecordInfo(recordInfo);
        recordChangeInfo.setUpdatedProperties(Arrays.asList("Name"));
        when(this.recordChangeInfoCache.get(any())).thenReturn(recordChangeInfo);

        // Test
        RecordChangedMessages recordChangedMessages = new RecordChangedMessages();
        recordChangedMessages.setAttributes(new HashMap<>());
        Map<String, List<String>> upsertKindIds = new HashMap<>();
        Map<String, List<String>> deleteKindIds = new HashMap<>();
        upsertKindIds.put(childKind, Arrays.asList(childId));
        this.sut.updateAssociatedRecords(recordChangedMessages, upsertKindIds, deleteKindIds);

        // Verify
        verify(this.indexerQueueTaskBuilder,times(0)).createWorkerTask(any(), any(), any());
    }

    @Test
    public void updateAssociatedRecords_updateAssociatedParentRecords_circularIndexing() throws URISyntaxException {
        updateAssociatedRecords_updateAssociatedParentRecords_baseSetup();

        // Test
        RecordChangedMessages recordChangedMessages = new RecordChangedMessages();
        Map<String, String> attributes = new HashMap<>();
        attributes.put(Constants.ANCESTRY_KINDS, parentKind);
        recordChangedMessages.setAttributes(attributes);
        Map<String, List<String>> upsertKindIds = new HashMap<>();
        Map<String, List<String>> deleteKindIds = new HashMap<>();
        upsertKindIds.put(childKind, Arrays.asList(childId));
        this.sut.updateAssociatedRecords(recordChangedMessages, upsertKindIds, deleteKindIds);

        // Verify
        verify(this.indexerQueueTaskBuilder,times(0)).createWorkerTask(any(), any(), any());
    }

    private void updateAssociatedRecords_updateAssociatedParentRecords_baseSetup() throws URISyntaxException {
        childKind = "osdu:wks:work-product-component--WellLog:1.0.0";
        childId = "anyChildId";
        parentKind = "osdu:wks:master-data--Wellbore:1.0.0";
        parentId = "anyParentId";

        // Setup search response for searchService.queryWithCursor(...)
        when(this.searchService.queryWithCursor(any())).thenAnswer(invocation -> {
            SearchRequest searchRequest = invocation.getArgument(0);
            SearchResponse searchResponse = new SearchResponse();
            if (searchRequest.getKind().toString().equals(propertyConfigurationKind)) {
                if (searchRequest.getQuery().contains("nested")) {
                    // Return of getParentChildRelatedObjectsSpecs(...)
                    Map<String, Object> dataMap = getDataMap("wellbore_configuration_record.json");
                    SearchRecord searchRecord = new SearchRecord();
                    searchRecord.setData(dataMap);
                    searchResponse.setResults(Arrays.asList(searchRecord));
                } else {
                    // search ChildToParent.
                    // NO result
                }
            } else {
                if(searchRequest.getKind().toString().equals(childKind)) {
                    // Return of searchUniqueParentIds(...)
                    SearchRecord searchRecord = new SearchRecord();
                    Map<String, Object> childDataMap = new HashMap<>();
                    childDataMap.put("WellboreID", parentId);
                    searchRecord.setKind(childKind);
                    searchRecord.setData(childDataMap);
                    searchResponse.setResults(Arrays.asList(searchRecord));
                }
                else if(searchRequest.getKind().toString().equals("osdu:wks:master-data--Wellbore:1.*")) {
                    // Return of searchKindIds(...)
                    SearchRecord searchRecord = new SearchRecord();
                    searchRecord.setKind(parentKind);
                    searchRecord.setId(parentId);
                    searchResponse.setResults(Arrays.asList(searchRecord));
                }
            }
            return searchResponse;
        });

        // setup headers
        DpsHeaders dpsHeaders = new DpsHeaders();
        dpsHeaders.put(DpsHeaders.AUTHORIZATION, "testAuth");
        dpsHeaders.put(DpsHeaders.DATA_PARTITION_ID, "opendes");
        dpsHeaders.put(DpsHeaders.CORRELATION_ID, "123");
        when(this.requestInfo.getHeadersWithDwdAuthZ()).thenReturn(dpsHeaders);
    }

    @Test
    public void updateAssociatedRecords_updateAssociatedChildrenRecords_for_created_parentRecord() throws URISyntaxException {
        updateAssociatedRecords_updateAssociatedChildrenRecords_for_created_delete(OperationType.create);
    }

    @Test
    public void updateAssociatedRecords_updateAssociatedChildrenRecords_for_deleted_parentRecord() throws URISyntaxException {
        updateAssociatedRecords_updateAssociatedChildrenRecords_for_created_delete(OperationType.delete);
    }

    private void updateAssociatedRecords_updateAssociatedChildrenRecords_for_created_delete(OperationType op) throws URISyntaxException {
        updateAssociatedRecords_updateAssociatedChildrenRecords_baseSetup();

        // Test
        RecordChangedMessages recordChangedMessages = new RecordChangedMessages();
        recordChangedMessages.setAttributes(new HashMap<>());
        Map<String, List<String>> upsertKindIds = new HashMap<>();
        Map<String, List<String>> deleteKindIds = new HashMap<>();
        if(op == OperationType.create)
            upsertKindIds.put(parentKind, Arrays.asList(parentId));
        else
            deleteKindIds.put(parentKind, Arrays.asList(parentId));
        this.sut.updateAssociatedRecords(recordChangedMessages, upsertKindIds, deleteKindIds);

        // Verify
        ArgumentCaptor<String> payloadArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(this.indexerQueueTaskBuilder,times(1)).createWorkerTask(payloadArgumentCaptor.capture(), any(), any());

        RecordChangedMessages newMessages = gson.fromJson(payloadArgumentCaptor.getValue(), RecordChangedMessages.class);
        Type type = new TypeToken<List<RecordInfo>>() {}.getType();
        List<RecordInfo> infoList = gson.fromJson(newMessages.getData(), type);
        Assert.assertEquals(parentKind, newMessages.getAttributes().get(Constants.ANCESTRY_KINDS));
        Assert.assertEquals(1, infoList.size());
        Assert.assertEquals(childKind, infoList.get(0).getKind());
        Assert.assertEquals(childId, infoList.get(0).getId());
    }

    @Test
    public void updateAssociatedRecords_updateAssociatedChildrenRecords_for_updated_parentRecord_with_extendedPropertyChanged() throws URISyntaxException {
        updateAssociatedRecords_updateAssociatedChildrenRecords_baseSetup();

        RecordChangeInfo recordChangeInfo = new RecordChangeInfo();
        RecordInfo recordInfo = new RecordInfo();
        recordInfo.setKind(parentKind);
        recordInfo.setId(parentId);
        recordInfo.setOp(OperationType.update.getValue());
        recordChangeInfo.setRecordInfo(recordInfo);
        recordChangeInfo.setUpdatedProperties(Arrays.asList("GeoPoliticalEntityName"));
        when(this.recordChangeInfoCache.get(any())).thenReturn(recordChangeInfo);

        // Test
        RecordChangedMessages recordChangedMessages = new RecordChangedMessages();
        recordChangedMessages.setAttributes(new HashMap<>());
        Map<String, List<String>> upsertKindIds = new HashMap<>();
        Map<String, List<String>> deleteKindIds = new HashMap<>();
        upsertKindIds.put(parentKind, Arrays.asList(parentId));
        this.sut.updateAssociatedRecords(recordChangedMessages, upsertKindIds, deleteKindIds);

        // Verify
        ArgumentCaptor<String> payloadArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(this.indexerQueueTaskBuilder,times(1)).createWorkerTask(payloadArgumentCaptor.capture(), any(), any());

        RecordChangedMessages newMessages = gson.fromJson(payloadArgumentCaptor.getValue(), RecordChangedMessages.class);
        Type type = new TypeToken<List<RecordInfo>>() {}.getType();
        List<RecordInfo> infoList = gson.fromJson(newMessages.getData(), type);
        Assert.assertEquals(parentKind, newMessages.getAttributes().get(Constants.ANCESTRY_KINDS));
        Assert.assertEquals(1, infoList.size());
        Assert.assertEquals(childKind, infoList.get(0).getKind());
        Assert.assertEquals(childId, infoList.get(0).getId());
    }

    @Test
    public void updateAssociatedRecords_updateAssociatedChildrenRecords_for_updated_parentRecord_without_extendedPropertyChanged() throws URISyntaxException {
        updateAssociatedRecords_updateAssociatedChildrenRecords_baseSetup();

        RecordChangeInfo recordChangeInfo = new RecordChangeInfo();
        RecordInfo recordInfo = new RecordInfo();
        recordInfo.setKind(parentKind);
        recordInfo.setId(parentId);
        recordInfo.setOp(OperationType.update.getValue());
        recordChangeInfo.setRecordInfo(recordInfo);
        recordChangeInfo.setUpdatedProperties(Arrays.asList("abc"));
        when(this.recordChangeInfoCache.get(any())).thenReturn(recordChangeInfo);

        // Test
        RecordChangedMessages recordChangedMessages = new RecordChangedMessages();
        recordChangedMessages.setAttributes(new HashMap<>());
        Map<String, List<String>> upsertKindIds = new HashMap<>();
        Map<String, List<String>> deleteKindIds = new HashMap<>();
        upsertKindIds.put(parentKind, Arrays.asList(parentId));
        this.sut.updateAssociatedRecords(recordChangedMessages, upsertKindIds, deleteKindIds);

        // Verify
        verify(this.indexerQueueTaskBuilder,times(0)).createWorkerTask(any(), any(), any());
    }

    @Test
    public void updateAssociatedRecords_updateAssociatedChildrenRecords_circularIndexing() throws URISyntaxException {
        updateAssociatedRecords_updateAssociatedChildrenRecords_baseSetup();

        // Test
        RecordChangedMessages recordChangedMessages = new RecordChangedMessages();
        Map<String, String> attributes = new HashMap<>();
        attributes.put(Constants.ANCESTRY_KINDS, childKind);
        recordChangedMessages.setAttributes(attributes);
        Map<String, List<String>> upsertKindIds = new HashMap<>();
        Map<String, List<String>> deleteKindIds = new HashMap<>();
        upsertKindIds.put(parentKind, Arrays.asList(parentId));
        this.sut.updateAssociatedRecords(recordChangedMessages, upsertKindIds, deleteKindIds);

        // Verify
        verify(this.indexerQueueTaskBuilder,times(0)).createWorkerTask(any(), any(), any());
    }

    private void updateAssociatedRecords_updateAssociatedChildrenRecords_baseSetup() throws URISyntaxException {
        childKind = "osdu:wks:master-data--Well:1.0.0";
        childId = "anyChildId";
        parentKind = "osdu:wks:master-data--GeoPoliticalEntity:1.0.0";
        parentId = "anyParentId";

        // Setup search response for searchService.queryWithCursor(...)
        when(this.searchService.queryWithCursor(any())).thenAnswer(invocation -> {
            SearchRequest searchRequest = invocation.getArgument(0);
            SearchResponse searchResponse = new SearchResponse();
            if (searchRequest.getKind().toString().equals(propertyConfigurationKind)) {
                if (!searchRequest.getQuery().contains("nested")) {
                    // Return of getParentChildRelatedObjectsSpecs(...)
                    Map<String, Object> dataMap = getDataMap("well_configuration_record.json");
                    SearchRecord searchRecord = new SearchRecord();
                    searchRecord.setData(dataMap);
                    searchResponse.setResults(Arrays.asList(searchRecord));
                } else {
                    // search ChildToParent
                    // No result
                }
            } else {
                String kind = "*:*:*:*,-"+parentKind;
                if(searchRequest.getKind().toString().equals(kind)) {
                    // Return of searchUniqueParentIds(...)
                    SearchRecord searchRecord = new SearchRecord();
                    Map<String, Object> childDataMap = new HashMap<>();
                    childDataMap.put("AssociatedIdentities", Arrays.asList(parentId));
                    searchRecord.setKind(childKind);
                    searchRecord.setId(childId);
                    searchRecord.setData(childDataMap);
                    searchResponse.setResults(Arrays.asList(searchRecord));
                }
                else {
                    // This branch is updateAssociatedRecords_updateAssociatedChildrenRecords_circularIndexing
                    kind = "*:*:*:*,-"+ childKind + ",-" + parentKind;
                    if(!searchRequest.getKind().toString().equals(kind)) {
                        throw new Exception("Unexpected search");
                    }
                }
            }
            return searchResponse;
        });

        // setup headers
        DpsHeaders dpsHeaders = new DpsHeaders();
        dpsHeaders.put(DpsHeaders.AUTHORIZATION, "testAuth");
        dpsHeaders.put(DpsHeaders.DATA_PARTITION_ID, "opendes");
        dpsHeaders.put(DpsHeaders.CORRELATION_ID, "123");
        when(this.requestInfo.getHeadersWithDwdAuthZ()).thenReturn(dpsHeaders);
    }

    @Test
    public void getRelatedObjectIds_with_valid_condition() {
        Map<String, Object> dataMap = getDataMap("well.json");
        RelatedObjectsSpec spec = new RelatedObjectsSpec();
        spec.setRelationshipDirection("ChildToParent");
        spec.setRelatedObjectKind("osdu:wks:master-data--GeoPoliticalEntity:1.");
        spec.setRelatedObjectID("data.GeoContexts[].GeoPoliticalEntityID");
        spec.setRelatedConditionProperty("data.GeoContexts[].GeoTypeID");
        List<String> matches = Arrays.asList("opendes:reference-data--GeoPoliticalEntityType:Country:", "opendes:reference-data--GeoPoliticalEntityType:LicenseBlock:");
        spec.setRelatedConditionMatches(matches);

        List<String> relatedObjectIds = sut.getRelatedObjectIds(dataMap, spec);
        Assert.assertEquals(2, relatedObjectIds.size());
    }

    @Test
    public void getValuePaths_with_valid_condition() {
        Map<String, Object> dataMap = getDataMap("well.json");
        ValueExtraction valueExtraction = new ValueExtraction();
        valueExtraction.setValuePath("data.NameAliases[].AliasName");
        valueExtraction.setRelatedConditionProperty("data.NameAliases[].AliasNameTypeID");
        List<String> matches = Arrays.asList(
                "opendes:reference-data--AliasNameType:UniqueIdentifier:",
                "opendes:reference-data--AliasNameType:RegulatoryName:",
                "opendes:reference-data--AliasNameType:PreferredName:",
                "opendes:reference-data--AliasNameType:CommonName:",
                "opendes:reference-data--AliasNameType:ShortName:");
        valueExtraction.setRelatedConditionMatches(matches);

        String valuePath = PropertyUtil.removeDataPrefix(valueExtraction.getValuePath());

        Map<String, Object> propertyValues = sut.getPropertyValues(dataMap, valueExtraction, false);
        Assert.assertTrue(propertyValues.containsKey(valuePath));
        Assert.assertTrue(propertyValues.get(valuePath) instanceof List);
        List<Object> values = (List<Object>)propertyValues.get(valuePath);
        Assert.assertEquals(2, values.size());
        Assert.assertTrue(values.contains("100000113552"));
        Assert.assertTrue(values.contains("Well1"));


        propertyValues = sut.getPropertyValues(dataMap, valueExtraction, true);
        Assert.assertTrue(propertyValues.containsKey(valuePath));
        Assert.assertTrue(propertyValues.get(valuePath) instanceof String);
        String value = (String)propertyValues.get(valuePath);
        Assert.assertEquals("100000113552", value);
    }

    private PropertyConfigurations getConfigurations(String file) throws JsonProcessingException {
        Map<String, Object> dataMap = getDataMap(file);
        ObjectMapper objectMapper = new ObjectMapper();
        String data = objectMapper.writeValueAsString(dataMap);
        PropertyConfigurations configurations = objectMapper.readValue(data, PropertyConfigurations.class);
        return configurations;
    }

    private Map<String, Object> getDataMap(String file) {
        String jsonText = getJsonFromFile(file);
        Type type = new TypeToken<Map<String, Object>>() {}.getType();
        return  gson.fromJson(jsonText, type);
    }

    @SneakyThrows
    private String getJsonFromFile(String file) {
        InputStream inStream = this.getClass().getResourceAsStream("/indexproperty/" + file);
        BufferedReader br = new BufferedReader(new InputStreamReader(inStream));
        StringBuilder stringBuilder = new StringBuilder();
        String sCurrentLine;
        while ((sCurrentLine = br.readLine()) != null)
        {
            stringBuilder.append(sCurrentLine).append("\n");
        }
        return stringBuilder.toString();
    }
}
