package org.opengroup.osdu.common;

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
import org.opengroup.osdu.util.ElasticUtils;
import org.opengroup.osdu.util.FileHandler;
import org.opengroup.osdu.util.HTTPClient;

import javax.ws.rs.HttpMethod;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.opengroup.osdu.util.Config.getEntitlementsDomain;
import static org.opengroup.osdu.util.Config.getStorageBaseURL;

@Log
public class RecordSteps extends TestsBase {
    private Map<String, TestIndex> inputIndexMap = new HashMap<>();
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
        if (records != null && records.size() > 0) {
            for (Map<String, Object> testRecord : records) {
                String id = testRecord.get("id").toString();
                httpClient.send(HttpMethod.DELETE, getStorageBaseURL() + "records/" + id, null, headers, httpClient.getAccessToken());
                log.info("Deleted the records");
            }
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
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    tearDown();
                }
            });
            shutDownHookAdded = true;
            for (String kind : inputIndexMap.keySet()) {
                TestIndex testIndex = inputIndexMap.get(kind);
                testIndex.setupSchema();
            }
        }
    }

    public void i_ingest_records_with_the_for_a_given(String record, String dataGroup, String kind) {

        String actualKind = generateActualName(kind, timeStamp);
        try {
            String fileContent = FileHandler.readFile(String.format("%s.%s", record, "json"));
            records = new Gson().fromJson(fileContent, new TypeToken<List<Map<String, Object>>>() {}.getType());

            for (Map<String, Object> testRecord : records) {
                testRecord.put("id", generateActualName(testRecord.get("id").toString(), timeStamp));
                testRecord.put("kind", actualKind);
                testRecord.put("legal", generateLegalTag());
                String[] x_acl = {generateActualName(dataGroup,timeStamp)+"."+getEntitlementsDomain()};
                Acl acl = Acl.builder().viewers(x_acl).owners(x_acl).build();
                testRecord.put("acl", acl);
            }
            String payLoad = new Gson().toJson(records);
            ClientResponse clientResponse = httpClient.send(HttpMethod.PUT, getStorageBaseURL() + "records", payLoad, headers, httpClient.getAccessToken());
            assertEquals(201, clientResponse.getStatus());
        } catch (Exception ex) {
            throw new AssertionError(ex.getMessage());
        }
    }

    public void i_should_get_the_documents_for_the_in_the_Elastic_Search(int expectedCount, String index) throws Throwable {
        index = generateActualName(index, timeStamp);
        long numOfIndexedDocuments = createIndex(index);
        assertEquals(expectedCount, numOfIndexedDocuments);
    }

    public void i_should_get_the_elastic_for_the_tenant_testindex_timestamp_well_in_the_Elastic_Search(String expectedMapping, String type, String index) throws Throwable {
        index = generateActualName(index, timeStamp);
        Map<String, MappingMetadata> elasticMapping = elasticUtils.getMapping(index);
        assertNotNull(elasticMapping);

        MappingMetadata typeMapping = elasticMapping.get(index);
        Map<String, Object> mapping = typeMapping.sourceAsMap();
        assertNotNull(mapping);
        assertTrue(areJsonEqual(expectedMapping, mapping.toString()));
    }

    public void iShouldGetTheNumberDocumentsForTheIndexInTheElasticSearchWithOutSkippedAttribute(int expectedCount, String index, String skippedAttributes) throws Throwable {
        index = generateActualName(index, timeStamp);
        long numOfIndexedDocuments = createIndex(index);
        long documentCountByQuery = elasticUtils.fetchRecordsByExistQuery(index, skippedAttributes);
        assertEquals(expectedCount, documentCountByQuery);
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

}