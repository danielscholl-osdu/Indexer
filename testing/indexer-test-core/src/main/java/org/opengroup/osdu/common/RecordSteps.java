package org.opengroup.osdu.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.jersey.api.client.ClientResponse;
import cucumber.api.DataTable;
import lombok.extern.java.Log;
import org.elasticsearch.action.DocWriteResponse.Result;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.cluster.metadata.MappingMetadata;
import org.opengroup.osdu.core.common.http.CollaborationContextFactory;
import org.opengroup.osdu.core.common.model.entitlements.Acl;
import org.opengroup.osdu.core.common.model.http.CollaborationContext;
import org.opengroup.osdu.core.common.model.storage.UpsertRecords;
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
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static org.junit.Assert.*;
import static org.opengroup.osdu.util.Config.getEntitlementsDomain;
import static org.opengroup.osdu.util.Config.getStorageBaseURL;
import static org.opengroup.osdu.util.HTTPClient.indentatedResponseBody;
import static org.opengroup.osdu.util.JsonPathMatcher.FindArrayInJson;

@Log
public class RecordSteps extends TestsBase {
    private Map<String, TestIndex> inputIndexMap = new HashMap<>();
    private ObjectMapper mapper = new ObjectMapper();
    private boolean shutDownHookAdded = false;

    private String timeStamp = String.valueOf(System.currentTimeMillis());
    private List<Map<String, Object>> records;
    private Map<String, String> headers = httpClient.getCommonHeader();

    private UpsertRecords upsertedRecordsWithXcollab;
    public static final String DpsHeaders_COLLABORATION = "x-collaboration";
    public static final String X_COLLABORATION = "collaborationId";

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
            testIndex.cleanupIndex(kind);
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
                if(testRecord.containsKey("data")) {
                    Map<String, Object> data = (Map<String, Object>)testRecord.get("data");
                    if(data != null && data.size() > 0) {
                        data = replaceValues(data, timeStamp);
                        testRecord.put("data", data);
                    }
                }

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
            log.info(String.format("Response body: %s\n Correlation id: %s\nResponse Status code: %s", indentatedResponseBody(clientResponse.getEntity(String.class)), clientResponse.getHeaders().get("correlation-id"), clientResponse.getStatus()));
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

    public void i_should_not_get_any_documents_for_the_index_in_the_Elastic_Search(String index) throws Throwable {
        index = generateActualName(index, timeStamp);
        getRecordsInIndex(index, 0);
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
        long numOfIndexedDocuments = createIndex(index);
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

    public void iShouldCleanupIndicesOfExtendedKinds(String extendedKinds) throws Throwable {
        String[] kinds = extendedKinds.split(",");
        for(String kind : kinds) {
            String actualKind = this.generateActualName(kind.trim(), timeStamp);
            TestIndex testIndex = this.getInputIndexMap().get(actualKind);
            testIndex.cleanupIndex(actualKind);
        }
    }

    public void iShouldBeAbleToSearchRecordByFieldAndFieldValue(String index, String fieldKey, String fieldValue, int expectedNumber) throws Throwable {
        TimeUnit.SECONDS.sleep(60);
        index = generateActualName(index, timeStamp);
        long actualNumberOfRecords = elasticUtils.fetchRecordsByFieldAndFieldValue(index, fieldKey, fieldValue);
        assertEquals(expectedNumber, actualNumberOfRecords);
    }

    public void i_should_get_the_documents_for_the_in_the_Elastic_Search_by_geoQuery (
            int expectedNumber, String index, Double topLatitude, Double topLongitude, Double bottomLatitude, Double bottomLongitude, String field) throws Throwable {
        index = generateActualName(index, timeStamp);
        long numOfIndexedDocuments = createIndex(index);
        long actualNumberOfRecords = elasticUtils.fetchRecordsByBoundingBoxQuery(index, field, topLatitude, topLongitude, bottomLatitude, bottomLongitude);
        assertEquals(expectedNumber, actualNumberOfRecords);
    }

    public void i_should_get_the_documents_for_the_in_the_Elastic_Search_by_AsIngestedCoordinates(
            int expectedNumber, String index, Double topPointX, Double bottomPointX, String pointX, Double topPointY, Double bottomPointY, String pointY) throws Throwable {
        index = generateActualName(index, timeStamp);
        long numOfIndexedDocuments = createIndex(index);
        long actualNumberOfRecords = elasticUtils.fetchRecordsByAsIngestedCoordinates(index, pointX, topPointX, bottomPointX, pointY, topPointY, bottomPointY);
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

    public void i_should_get_object_in_search_response(String innerField, String index)
            throws Throwable {
        index = generateActualName(index, timeStamp);
        long numOfIndexedDocuments = createIndex(index);

        String elasticRecordData = elasticUtils.fetchDataFromObjectsArrayRecords(index);
        RecordData actualRecordData = mapper.readValue(elasticRecordData, RecordData.class);

        assertTrue(actualRecordData.getData().containsKey(innerField));
    }

    public void i_should_get_string_array_in_search_response(String index, String field, String fieldValue, String arrayField, String desiredArrayValue)
            throws Throwable {
        TimeUnit.SECONDS.sleep(40);
        final List<Map<String, Object>> elasticRecordData =  elasticUtils.fetchRecordsByAttribute(index, field, fieldValue);
        assertEquals(1, elasticRecordData.size());
        final List<String> stringList = Arrays.asList(arrayField.split("\\."));
        final Map<String, Object> jsonRecord = elasticRecordData.get(0);
        assertEquals(String.join(",", (ArrayList<String>) FindArrayInJson(jsonRecord, stringList)), desiredArrayValue);
    }

    public void i_create_index_with_mapping_file_for_a_given_kind(String mappingFile, String index, String kind) throws Throwable {
        String actualKind = generateActualName(kind, timeStamp);
        TestIndex testIndex = getInputIndexMap().get(actualKind);
        testIndex.setMappingFile(mappingFile);
        this.getInputIndexMap().put(actualKind, testIndex);
        testIndex.addIndex();
    }

    public void i_ingest_records_with_xcollab_value_included_with_the_with_for_a_given(String xCollab,
                                                                                       String record,
                                                                                       String dataGroup,
                                                                                       String kind) {
        String actualKind = generateActualName(kind, timeStamp);
        try {
            String fileContent = FileHandler.readFile(String.format("%s.%s", record, "json"));
            records = new Gson().fromJson(fileContent, new TypeToken<List<Map<String, Object>>>() {
            }.getType());
            String createTime = java.time.Instant.now().toString();

            for (Map<String, Object> testRecord : records) {
                if (testRecord.containsKey("data")) {
                    Map<String, Object> data = (Map<String, Object>) testRecord.get("data");
                    if (data != null && data.size() > 0) {
                        data = replaceValues(data, timeStamp);
                        testRecord.put("data", data);
                    }
                }

                testRecord.put("kind", actualKind);
                testRecord.put("id", generateRecordId(testRecord));
                testRecord.put("legal", generateLegalTag());
                String[] x_acl = {generateActualName(dataGroup, timeStamp) + "." + getEntitlementsDomain()};
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

            // put record in WIP by the use of x-collaboration header
            Map<String, String> headerXcollab = httpClient.getCommonHeader();
            headerXcollab.put(DpsHeaders_COLLABORATION, xCollab);

            String payLoad = new Gson().toJson(records);
            log.log(Level.INFO, "Start ingesting records={0}", payLoad);
            ClientResponse clientResponse = httpClient.send(HttpMethod.PUT,
                getStorageBaseURL() + "records",
                payLoad,
                headerXcollab,
                httpClient.getAccessToken());

            String responseEntity = clientResponse.getEntity(String.class);
            log.info(String.format("Response body with xcollab: %s\n Correlation id: %s\nResponse Status code: %s",
                indentatedResponseBody(responseEntity),
                clientResponse.getHeaders().get("correlation-id"),
                clientResponse.getStatus()));
            assertEquals(201, clientResponse.getStatus());

            // remember record id for future tests
            upsertedRecordsWithXcollab = mapper.readValue(responseEntity, UpsertRecords.class);
            upsertedRecordsWithXcollab.getRecordIds()
                .forEach(recordId -> log.info("Record ids with xcollab : " + recordId));

            Optional<String> recordWithXcollab = upsertedRecordsWithXcollab.getRecordIds().stream().findAny();
            assertTrue(recordWithXcollab.isPresent());

        } catch (Exception ex) {
            throw new AssertionError(ex.getMessage());
        }
    }

    protected void i_should_get_the_documents_with_xcollab_value_included_for_the_in_the_Elastic_Search(int expectedNumber,
                                                                                                        String xcollab,
                                                                                                        String index)
        throws Exception {

        index = generateActualName(index, timeStamp);
        // upsertedRecordsWithoutXcollab should have id received from previous steps
        String id = upsertedRecordsWithXcollab.getRecordIds().stream().findAny().get();
        log.log(Level.INFO, String.format("Try to find in Elastic a record with X collab with id : %s ", id));

        SearchResponse searchResponse = null;
        // should wait while Storage will publish Record into queue,
        // then Index-queue should read message and pass it with http request to Indexer
        // Indexer then will index the record into Elastic
        CollaborationContextFactory collaborationContextFactory = new CollaborationContextFactory();
        Optional<CollaborationContext> collaborationContext = collaborationContextFactory.create(xcollab);
        String collaborationId = collaborationContext.orElseThrow().getId();

        for (int i = 0; i < 20; i++) {
            searchResponse = elasticUtils.fetchRecordsByIdAndMustHaveXcollab(index, id, collaborationId);
            if (searchResponse.getHits().getTotalHits().value == 0) {
                log.log(Level.INFO, String.format("No records found with in index: %s, id: %s, collaborationId: %s,"
                    + " will try to wait up to 3 seconds.", index, id, collaborationId));
                TimeUnit.SECONDS.sleep(3);
            } else {
                break;
            }
        }

        log.log(Level.INFO,
            String.format("xcollab feature: print searchResponse while being get a record by id and x-collab : %s",
                searchResponse));
        assertEquals(expectedNumber, searchResponse.getHits().getTotalHits().value);

        // delete test record in namespace
        String elasticId = Arrays.stream(searchResponse.getHits().getHits()).findAny().get().getId();
        DeleteResponse deleteResponse = elasticUtils.deleteRecordsById(index, elasticId);
        log.log(Level.INFO, String.format("Deleting record from Elasticsearch in index: %s with id: %s", index, elasticId));
        assertEquals(deleteResponse.getResult(), Result.DELETED);
    }

    private Map<String, Object> replaceValues(Map<String, Object> data, String timeStamp) {
        for(String key : data.keySet()) {
            Object value = data.get(key);
            Object replacedValue = replaceValue(value, timeStamp);
            data.put(key, replacedValue);
        }
        return data;
    }

    private List<Object> replaceValues(List<Object> values, String timeStamp) {
        List<Object> replacedValues = new ArrayList<>();
        for(Object value : values) {
            Object replacedValue = replaceValue(value, timeStamp);
            replacedValues.add(replacedValue);
        }

        return replacedValues;
    }

    private Object replaceValue(Object value, String timeStamp) {
        Object replacedValue = value;

        if(value instanceof String) {
            String rawValue = (String) value;
            for (Map.Entry<String, String> tenant : tenantMap.entrySet()) {
                rawValue = rawValue.replaceAll(tenant.getKey() + ":", tenant.getValue() + ":");
            }
            replacedValue = rawValue.replaceAll("<timestamp>", timeStamp);
        }
        else if(value instanceof List) {
            replacedValue = replaceValues((List)value, timeStamp);
        }
        else if(value instanceof Map) {
            replacedValue = replaceValues((Map<String, Object>) value, timeStamp);
        }

        return replacedValue;
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

    private long getRecordsInIndex(String index, int expectedCount) throws InterruptedException, IOException {
        long numOfIndexedDocuments = 0;
        int iterator;

        // index.refresh_interval is set to default 30s, wait for 40s initially
        Thread.sleep(40000);

        for (iterator = 0; iterator < 20; iterator++) {

            numOfIndexedDocuments = elasticUtils.fetchRecords(index);
            if (expectedCount == numOfIndexedDocuments) {
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
