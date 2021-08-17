package org.opengroup.osdu.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.jersey.api.client.ClientResponse;
import cucumber.api.DataTable;
import lombok.extern.java.Log;
import org.elasticsearch.cluster.metadata.MappingMetadata;
import org.opengroup.osdu.core.common.model.entitlements.Acl;
import org.opengroup.osdu.models.Setup;
import org.opengroup.osdu.models.TestIndex;
import org.opengroup.osdu.models.record.RecordData;
import org.opengroup.osdu.util.ElasticUtils;
import org.opengroup.osdu.util.FileHandler;
import org.opengroup.osdu.util.HTTPClient;
import org.springframework.util.CollectionUtils;

import javax.ws.rs.HttpMethod;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static org.junit.Assert.*;
import static org.opengroup.osdu.util.Config.getEntitlementsDomain;
import static org.opengroup.osdu.util.Config.getStorageBaseURL;

@Log
public class RecordSteps extends TestsBase {
    private Map<String, TestIndex> inputIndexMap = new HashMap<>();
    private ObjectMapper mapper = new ObjectMapper();
    private boolean shutDownHookAdded = false;

    private String timeStamp = String.valueOf(System.currentTimeMillis());
    private List<Map<String, Object>> records;
    private Map<String, String> headers = httpClient.getCommonHeader();

    public RecordSteps(HTTPClient httpClient) {
        super(httpClient);
    }

    public RecordSteps(HTTPClient httpClient, ElasticUtils elasticUtils) {
        super(httpClient, elasticUtils);
    }

    /******************One time cleanup for whole feature**************/
    public void tearDown() {
        for (String kind : inputIndexMap.keySet()) {
            TestIndex testIndex = inputIndexMap.get(kind);
            testIndex.cleanupIndex();
            testIndex.deleteSchema(kind);
        }

        if (!CollectionUtils.isEmpty(records)) {
            cleanupRecords();
        }
    }

    protected void cleanupRecords() {
        for (Map<String, Object> testRecord : records) {
            String id = testRecord.get("id").toString();
            httpClient.send(HttpMethod.DELETE, getStorageBaseURL() + "records/" + id, null, headers, httpClient.getAccessToken());
            log.info("Deleted the records");
        }
    }

    public void the_schema_is_created_with_the_following_kind(DataTable dataTable) {

        List<Setup> inputList = dataTable.asList(Setup.class);
        for (Setup input : inputList) {
            TestIndex testIndex = getTextIndex();
            testIndex.setHttpClient(httpClient);
            testIndex.setIndex(generateActualName(input.getIndex(), timeStamp));
            testIndex.setKind(generateActualName(input.getKind(), timeStamp));
            testIndex.setSchemaFile(input.getSchemaFile());
            inputIndexMap.put(testIndex.getKind(), testIndex);
        }

        /******************One time setup for whole feature**************/
        if (!shutDownHookAdded) {
            for (String kind : inputIndexMap.keySet()) {
                TestIndex testIndex = inputIndexMap.get(kind);
                testIndex.setupSchema();
            }
        }
        addShutDownHook();
    }

    public void i_ingest_records_with_the_for_a_given(String record, String dataGroup, String kind) {

        String actualKind = generateActualName(kind, timeStamp);
        try {
            String fileContent = FileHandler.readFile(String.format("%s.%s", record, "json"));
            records = new Gson().fromJson(fileContent, new TypeToken<List<Map<String, Object>>>() {}.getType());
            String createTime = java.time.Instant.now().toString();

            for (Map<String, Object> testRecord : records) {
                testRecord.put("kind", actualKind);
                testRecord.put("id", generateRecordId(testRecord));
                testRecord.put("legal", generateLegalTag());
                String[] x_acl = {generateActualName(dataGroup,timeStamp)+"."+getEntitlementsDomain()};
                Acl acl = Acl.builder().viewers(x_acl).owners(x_acl).build();
                testRecord.put("acl", acl);
                String[] kindParts = kind.split(":");
                String authority = tenantMap.get(kindParts[0]);
                String source = kindParts[1];
                testRecord.put("authority", authority);
                testRecord.put("source", source);
                testRecord.put("createUser", "TestUser");
                testRecord.put("createTime", createTime);
            }
            String payLoad = new Gson().toJson(records);
            log.log(Level.INFO, "Start ingesting records={0}", payLoad);
            ClientResponse clientResponse = httpClient.send(HttpMethod.PUT, getStorageBaseURL() + "records", payLoad, headers, httpClient.getAccessToken());
            assertEquals(201, clientResponse.getStatus());
        } catch (Exception ex) {
            throw new AssertionError(ex.getMessage());
        }
    }

    protected String generateRecordId(Map<String, Object> testRecord) {
        return generateActualId(testRecord.get("id").toString(), timeStamp, testRecord.get("kind").toString());
    }

    public void i_should_get_the_documents_for_the_in_the_Elastic_Search(int expectedCount, String index) throws Throwable {
        index = generateActualName(index, timeStamp);
        long numOfIndexedDocuments = createIndex(index);
        assertEquals(expectedCount, numOfIndexedDocuments);
    }

    public void i_should_get_the_elastic_for_the_tenant_testindex_timestamp_well_in_the_Elastic_Search(String expectedMapping, String kind, String index) throws Throwable {
        index = generateActualName(index, timeStamp);
        Map<String, MappingMetadata> elasticMapping = elasticUtils.getMapping(index);
        assertNotNull(elasticMapping);

        String[] kindParts = kind.split(":");
        String authority = tenantMap.get(kindParts[0]);
        String source = kindParts[1];
        expectedMapping = expectedMapping.replaceAll("<authority-id>", authority).replaceAll("<source-id>", source);
        MappingMetadata typeMapping = elasticMapping.get(index);
        Map<String, Object> mapping = typeMapping.sourceAsMap();
        assertNotNull(mapping);
        assertTrue(areJsonEqual(expectedMapping, mapping.toString()));
    }

    public void i_can_validate_indexed_attributes(String index, String kind) throws Throwable {
        String authority = tenantMap.get(kind.substring(0, kind.indexOf(":")));
        index = generateActualName(index, timeStamp);
        log.log(Level.INFO, String.format("Search by authority: %s", authority));
        List<Map<String, Object>> hits = elasticUtils.fetchRecordsByAttribute(index, "authority", authority);

        assertTrue(hits.size() > 0);
        for (Map<String, Object> result : hits) {
            assertTrue(result.containsKey("authority"));
            assertEquals(authority, result.get("authority"));
            assertTrue(result.containsKey("source"));
            assertTrue(result.containsKey("createUser"));
            assertTrue(result.containsKey("createTime"));
        }
    }

    public void iShouldGetTheNumberDocumentsForTheIndexInTheElasticSearchWithOutSkippedAttribute(int expectedCount, String index, String skippedAttributes) throws Throwable {
        index = generateActualName(index, timeStamp);
        long numOfIndexedDocuments = createIndex(index);
        long documentCountByQuery = elasticUtils.fetchRecordsByExistQuery(index, skippedAttributes);
        assertEquals(expectedCount, documentCountByQuery);
    }

    public void iShouldBeAbleToSearchRecordByTagKeyAndTagValue(String index, String tagKey, String tagValue, int expectedNumber) throws Throwable {
        TimeUnit.SECONDS.sleep(40);
        index = generateActualName(index, timeStamp);
        long actualNumberOfRecords = elasticUtils.fetchRecordsByTags(index, tagKey, tagValue);
        assertEquals(expectedNumber, actualNumberOfRecords);
    }

    public void i_should_get_the_documents_for_the_in_the_Elastic_Search_by_geoQuery (
            int expectedNumber, String index, Double topLatitude, Double topLongitude, Double bottomLatitude, Double bottomLongitude, String field) throws Throwable {
        index = generateActualName(index, timeStamp);
        long numOfIndexedDocuments = createIndex(index);
        long actualNumberOfRecords = elasticUtils.fetchRecordsByBoundingBoxQuery(index, field, topLatitude, topLongitude, bottomLatitude, bottomLongitude);
        assertEquals(expectedNumber, actualNumberOfRecords);
    }

    public void i_should_get_the_documents_for_the_in_the_Elastic_Search_by_nestedQuery(
        int expectedNumber, String index, String path, String firstNestedField, String firstNestedValue, String secondNestedField, String secondNestedValue)
        throws Throwable {
        index = generateActualName(index, timeStamp);
        long numOfIndexedDocuments = createIndex(index);
        long actualNumberOfRecords = elasticUtils.fetchRecordsByNestedQuery(index, path, firstNestedField, firstNestedValue, secondNestedField, secondNestedValue);
        assertEquals(expectedNumber, actualNumberOfRecords);
    }

    public void i_should_be_able_search_documents_for_the_by_flattened_inner_properties(int expectedCount, String index, String flattenedField,
        String flattenedFieldValue) throws Throwable {
        index = generateActualName(index, timeStamp);
        long numOfIndexedDocuments = createIndex(index);
        long actualNumberOfRecords = elasticUtils.fetchRecordsWithFlattenedFieldsQuery(index, flattenedField, flattenedFieldValue);
        assertEquals(expectedCount, actualNumberOfRecords);
    }

    public void i_should_get_object_in_search_response_without_hints_in_schema(String objectField, String index, String recordFile, String acl, String kind)
        throws Throwable {
        index = generateActualName(index, timeStamp);
        long numOfIndexedDocuments = createIndex(index);
        String expectedRecord = FileHandler.readFile(String.format("%s.%s", recordFile, "json"));

        RecordData[] fileRecordData = mapper.readValue(expectedRecord, RecordData[].class);
        RecordData expectedRecordData = fileRecordData[0];

        String elasticRecordData = elasticUtils.fetchDataFromObjectsArrayRecords(index);
        RecordData actualRecordData = mapper.readValue(elasticRecordData, RecordData.class);

        assertEquals(expectedRecordData.getData().get(objectField),actualRecordData.getData().get(objectField));
    }

    private long createIndex(String index) throws InterruptedException, IOException {
        long numOfIndexedDocuments = 0;
        int iterator;

        // index.refresh_interval is set to default 30s, wait for 40s initially
        Thread.sleep(40000);

        for (iterator = 0; iterator < 20; iterator++) {

            numOfIndexedDocuments = elasticUtils.fetchRecords(index);
            if (numOfIndexedDocuments > 0) {
                log.info(String.format("index: %s | attempts: %s | documents acknowledged by elastic: %s", index, iterator, numOfIndexedDocuments));
                break;
            } else {
                log.info(String.format("index: %s | documents acknowledged by elastic: %s", index, numOfIndexedDocuments));
                Thread.sleep(5000);
            }

            if ((iterator + 1) % 5 == 0) elasticUtils.refreshIndex(index);
        }
        if (iterator >= 20) {
            fail(String.format("index not created after waiting for %s seconds", ((40000 + iterator * 5000) / 1000)));
        }
        return numOfIndexedDocuments;
    }

    private Boolean areJsonEqual(String firstJson, String secondJson) {
        Gson gson = new Gson();
        Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
        Map<String, Object> firstMap = gson.fromJson(firstJson, mapType);
        Map<String, Object> secondMap = gson.fromJson(secondJson, mapType);

        MapDifference<String, Object> result = Maps.difference(firstMap, secondMap);
        if (result != null && result.entriesDiffering().isEmpty()) return true;
        log.info(String.format("difference: %s", result.entriesDiffering()));
        return false;
    }

    @Override
    protected String getApi() {
        return null;
    }

    @Override
    protected String getHttpMethod() {
        return null;
    }

    public Map<String, TestIndex> getInputIndexMap() {
        return inputIndexMap;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    protected void addShutDownHook() {
        if (!shutDownHookAdded) {
            Runtime.getRuntime().addShutdownHook(new Thread(this::tearDown));
            shutDownHookAdded = true;
        }
    }
}