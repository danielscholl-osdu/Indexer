package org.opengroup.osdu.indexer.service;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.opengroup.osdu.core.common.http.HeadersUtil;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.entitlements.Acl;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.indexer.*;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.core.common.model.storage.ConversionStatus;
import org.opengroup.osdu.core.common.provider.interfaces.IRequestInfo;
import org.opengroup.osdu.core.common.search.ElasticIndexNameResolver;
import org.opengroup.osdu.indexer.logging.AuditLogger;
import org.opengroup.osdu.indexer.model.indexproperty.PropertyConfigurations;
import org.opengroup.osdu.indexer.provider.interfaces.IPublisher;
import org.opengroup.osdu.indexer.util.ElasticClientHandler;
import org.opengroup.osdu.indexer.util.IndexerQueueTaskBuilder;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.*;

import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({RestHighLevelClient.class, BulkResponse.class, Acl.class, HeadersUtil.class})
public class IndexerServiceImplTest {

    @InjectMocks
    private IndexerServiceImpl sut;
    @Mock
    private ElasticClientHandler elasticClientHandler;
    @Mock
    private ElasticIndexNameResolver elasticIndexNameResolver;
    @Mock
    private IndicesService indicesService;
    @Mock
    private StorageService storageService;
    @Mock
    private StorageIndexerPayloadMapper storageIndexerPayloadMapper;
    @InjectMocks
    @Spy
    private JobStatus jobStatus = new JobStatus();
    @Mock
    private AuditLogger auditLogger;
    @Mock
    private BulkResponse bulkResponse;
    @Mock
    private IRequestInfo requestInfo;
    @Mock
    private RestHighLevelClient restHighLevelClient;
    @Mock
    private IndexSchemaService schemaService;
    @Mock
    private JaxRsDpsLog jaxRsDpsLog;
    @Mock
    private IMappingService mappingService;
    @Mock
    private IPublisher progressPublisher;
    @Mock
    private PropertyConfigurationsService propertyConfigurationsService;
    @Mock
    private IndexerQueueTaskBuilder indexerQueueTaskBuilder;

    private List<RecordInfo> recordInfos = new ArrayList<>();

    private final String pubsubMsg = "[{\"id\":\"opendes:doc:test1\",\"kind\":\"opendes:testindexer1:well:1.0.0\",\"op\":\"update\"}," +
            "{\"id\":\"opendes:doc:test2\",\"kind\":\"opendes:testindexer2:well:1.0.0\",\"op\":\"create\"}]";
    private final String pubsubMsgForDeletion = "[{\"id\":\"opendes:doc:test1\",\"kind\":\"opendes:testindexer1:well:1.0.0\",\"op\":\"delete\"}," +
            "{\"id\":\"opendes:doc:test2\",\"kind\":\"opendes:testindexer2:well:1.0.0\",\"op\":\"delete\"}]";
    private final String kind1 = "opendes:testindexer1:well:1.0.0";
    private final String kind2 = "opendes:testindexer2:well:1.0.0";
    private final String recordId1 = "opendes:doc:test1";
    private final String recordId2 = "opendes:doc:test2";
    private final String failureMassage = "test failure";

    private DpsHeaders dpsHeaders;
    private RecordChangedMessages recordChangedMessages;

    @Before
    public void setup() throws IOException {
    }

    @Test
    public void processSchemaMessagesTest() throws Exception {
        RecordInfo recordInfo = new RecordInfo();
        recordInfo.setId("opendes:ds:mytest3-d9033ae1-fb15-496c-9ba0-880fd1d2b2qf");
        recordInfo.setKind("opendes:ds:mytest2:1.0.0");
        recordInfo.setOp("purge_schema");
        this.recordInfos.add(recordInfo);

        initMocks(this);

        this.sut.processSchemaMessages(recordInfos);

        verify(this.elasticClientHandler, times(1)).createRestClient();
        verify(this.elasticIndexNameResolver, times(1)).getIndexNameFromKind(any());
        verify(this.indicesService, times(1)).isIndexExist(any(), any());
    }

    @Test
    public void should_properlyUpdateAuditLogs_givenValidCreateAndUpdateRecords() {
        try {
            prepareTestDataAndEnv(this.pubsubMsg);

            // test
            JobStatus jobStatus = this.sut.processRecordChangedMessages(recordChangedMessages, recordInfos);

            // validate
            assertEquals(2, jobStatus.getStatusesList().size());
            assertEquals(1, jobStatus.getIdsByIndexingStatus(IndexingStatus.FAIL).size());
            assertEquals(1, jobStatus.getIdsByIndexingStatus(IndexingStatus.SUCCESS).size());

            verify(this.auditLogger).indexCreateRecordSuccess(singletonList("RecordStatus(id=opendes:doc:test2, kind=opendes:testindexer2:well:1.0.0, operationType=create, status=SUCCESS)"));
            verify(this.auditLogger).indexUpdateRecordFail(singletonList("RecordStatus(id=opendes:doc:test1, kind=opendes:testindexer1:well:1.0.0, operationType=update, status=FAIL, message=test failure)"));
        } catch (Exception e) {
            fail("Should not throw this exception" + e.getMessage());
        }
    }

    @Test
    public void should_updateAssociatedRecords_givenValidCreateAndUpdateRecords() {
        try {
            prepareTestDataAndEnv(this.pubsubMsg);

            // setup property configuration
            when(this.propertyConfigurationsService.isPropertyConfigurationsEnabled(any())).thenReturn(true);
            ArgumentCaptor<Map<String, List<String>>> upsertArgumentCaptor = ArgumentCaptor.forClass(Map.class);
            ArgumentCaptor<Map<String, List<String>>> deleteArgumentCaptor = ArgumentCaptor.forClass(Map.class);

            // test
            this.sut.processRecordChangedMessages(recordChangedMessages, recordInfos);

            // validate
            verify(this.propertyConfigurationsService, times(1)).updateAssociatedRecords(any(), upsertArgumentCaptor.capture(), deleteArgumentCaptor.capture());
            Map<String, List<String>> upsertKindIds = upsertArgumentCaptor.getValue();
            Map<String, List<String>> deleteKindIds = deleteArgumentCaptor.getValue();
            assertEquals(1, upsertKindIds.size());
            assertFalse(upsertKindIds.containsKey(kind1));
            assertEquals(1, upsertKindIds.get(kind2).size());
            assertEquals(0, deleteKindIds.size());
        } catch (Exception e) {
            fail("Should not throw this exception" + e.getMessage());
        }
    }

    @Test
    public void should_updateAssociatedRecords_givenValidDeleteRecords() {
        try {
            prepareTestDataAndEnv(this.pubsubMsgForDeletion);

            // setup property configuration
            when(this.propertyConfigurationsService.isPropertyConfigurationsEnabled(any())).thenReturn(true);
            ArgumentCaptor<Map<String, List<String>>> upsertArgumentCaptor = ArgumentCaptor.forClass(Map.class);
            ArgumentCaptor<Map<String, List<String>>> deleteArgumentCaptor = ArgumentCaptor.forClass(Map.class);

            // test
            this.sut.processRecordChangedMessages(recordChangedMessages, recordInfos);

            // validate
            verify(this.propertyConfigurationsService, times(1)).updateAssociatedRecords(any(), upsertArgumentCaptor.capture(), deleteArgumentCaptor.capture());
            Map<String, List<String>> upsertKindIds = upsertArgumentCaptor.getValue();
            Map<String, List<String>> deleteKindIds = deleteArgumentCaptor.getValue();
            assertEquals(0, upsertKindIds.size());
            assertEquals(2, deleteKindIds.size());
            assertEquals(1, deleteKindIds.get(kind1).size());
            assertEquals(1, deleteKindIds.get(kind2).size());
        } catch (Exception e) {
            fail("Should not throw this exception" + e.getMessage());
        }
    }

    @Test
    public void should_mergeExtendedProperties_givenValidCreateAndUpdateRecords_and_kindsHavingPropertyConfigurations() {
        try {
            prepareTestDataAndEnv(this.pubsubMsg);

            // setup property configuration
            when(this.propertyConfigurationsService.isPropertyConfigurationsEnabled(any())).thenReturn(true);
            when(this.propertyConfigurationsService.getPropertyConfigurations(any())).thenReturn(new PropertyConfigurations());

            // test
            this.sut.processRecordChangedMessages(recordChangedMessages, recordInfos);

            // validate
            verify(this.propertyConfigurationsService, times(1)).getPropertyConfigurations(any());
            verify(this.propertyConfigurationsService, times(1)).getExtendedProperties(any(), any(), any());
            verify(this.propertyConfigurationsService, times(1)).cacheDataRecord(any(), any(), any());
        } catch (Exception e) {
            fail("Should not throw this exception" + e.getMessage());
        }
    }

    private void prepareTestDataAndEnv(String pubsubMsg) throws IOException, URISyntaxException {
        mockStatic(Acl.class);

        // setup headers
        this.dpsHeaders = new DpsHeaders();
        this.dpsHeaders.put(DpsHeaders.AUTHORIZATION, "testAuth");
        when(this.requestInfo.getHeaders()).thenReturn(dpsHeaders);
        when(this.requestInfo.getHeadersMapWithDwdAuthZ()).thenReturn(dpsHeaders.getHeaders());

        // setup message
        Type listType = new TypeToken<List<RecordInfo>>() {}.getType();
        this.recordInfos = (new Gson()).fromJson(pubsubMsg, listType);
        Map<String, String> messageAttributes = new HashMap<>();
        messageAttributes.put(DpsHeaders.DATA_PARTITION_ID, "opendes");
        this.recordChangedMessages = RecordChangedMessages.builder().attributes(messageAttributes).messageId("xxxx").publishTime("2000-01-02T10:10:44+0000").data("{}").build();

        // setup schema
        Map<String, Object> schema = createSchema();
        indexSchemaServiceMock(kind2, schema);
        indexSchemaServiceMock(kind1, null);

        // setup storage records
        Map<String, Object> storageData = new HashMap<>();
        storageData.put("schema1", "test-value");
        List<Records.Entity> validRecords = new ArrayList<>();
        validRecords.add(Records.Entity.builder().id(recordId2).kind(kind2).data(storageData).build());
        List<String> failedOrRetryRecordIds = new ArrayList<>();
        failedOrRetryRecordIds.add(recordId1);
        List<ConversionStatus> conversionStatus = new LinkedList<>();
        Records storageRecords = Records.builder().records(validRecords).missingRetryRecords(failedOrRetryRecordIds).conversionStatuses(conversionStatus).build();
        when(this.storageService.getStorageRecords(any(), any())).thenReturn(storageRecords);

        // setup elastic, index and mapped document
        when(this.indicesService.createIndex(any(), any(), any(), any(), any())).thenReturn(true);
        when(this.mappingService.getIndexMappingFromRecordSchema(any())).thenReturn(new HashMap<>());

        when(this.elasticClientHandler.createRestClient()).thenReturn(this.restHighLevelClient);
        when(this.restHighLevelClient.bulk(any(), any(RequestOptions.class))).thenReturn(this.bulkResponse);

        Map<String, Object> indexerMappedPayload = new HashMap<>();
        indexerMappedPayload.put("id", "keyword");
        when(this.storageIndexerPayloadMapper.mapDataPayload(any(), any(), any())).thenReturn(indexerMappedPayload);

        BulkItemResponse[] responses = new BulkItemResponse[]{prepareFailedResponse(), prepareSuccessfulResponse()};
        when(this.bulkResponse.getItems()).thenReturn(responses);
    }

    private BulkItemResponse prepareFailedResponse() {
        BulkItemResponse responseFail = mock(BulkItemResponse.class);
        when(responseFail.isFailed()).thenReturn(true);
        when(responseFail.getFailureMessage()).thenReturn(failureMassage);
        when(responseFail.getId()).thenReturn(recordId1);
        when(responseFail.getFailure()).thenReturn(new BulkItemResponse.Failure("failure index", "failure type", "failure id", new Exception("test failure")));
        return responseFail;
    }

    private BulkItemResponse prepareSuccessfulResponse() {
        BulkItemResponse responseSuccess = mock(BulkItemResponse.class);
        when(responseSuccess.getId()).thenReturn(recordId2);
        return responseSuccess;
    }

    private void indexSchemaServiceMock(String kind, Map<String, Object> schema) throws UnsupportedEncodingException, URISyntaxException {
        IndexSchema indexSchema = schema == null
                ? IndexSchema.builder().kind(kind).dataSchema(new HashMap<>()).build()
                : IndexSchema.builder().kind(kind).dataSchema(schema).build();
        when(schemaService.getIndexerInputSchema(kind, new ArrayList<>())).thenReturn(indexSchema);
    }

    private Map<String, Object> createSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("schema1", "keyword");
        schema.put("schema2", "boolean");
        schema.put("schema3", "date");
        schema.put("schema6", "object");
        return schema;
    }
}
