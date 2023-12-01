package org.opengroup.osdu.models;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.sun.jersey.api.client.ClientResponse;
import lombok.Data;

import javax.ws.rs.HttpMethod;

import org.opengroup.osdu.util.HTTPClient;
import org.opengroup.osdu.core.common.model.entitlements.Acl;
import org.opengroup.osdu.core.common.model.legal.Legal;
import org.opengroup.osdu.util.ElasticUtils;
import org.opengroup.osdu.util.FileHandler;
import org.opengroup.osdu.util.IndexerClientUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.opengroup.osdu.util.Config.*;
import static org.opengroup.osdu.util.HTTPClient.indentatedResponseBody;

@Data
public class TestIndex {
    private static final Logger LOGGER = Logger.getLogger(TestIndex.class.getName());
    private String kind;
    private String index;
    private String mappingFile;
    private String recordFile;
    private int recordCount;
    private String schemaFile;
    private String[] dataGroup;
    private String[] viewerGroup;
    private String[] ownerGroup;
    private HTTPClient httpClient;
    private Map<String, String> headers;
    private ElasticUtils elasticUtils;
    private IndexerClientUtil indexerClientUtil;
    private Gson gson = new Gson();

    public TestIndex(ElasticUtils elasticUtils){
        this.elasticUtils = elasticUtils;
    }

    public void setHttpClient(HTTPClient httpClient) {
        this.httpClient = httpClient;
        headers = httpClient.getCommonHeader();
        this.indexerClientUtil = new IndexerClientUtil(this.httpClient);
    }

    public void setupIndex() {
        this.addIndex();
        List<Map<String, Object>> records = getRecordsFromTestFile();
        this.recordCount = this.elasticUtils.indexRecords(this.index, this.kind, records);
    }

    public void setupSchema() {
        ClientResponse clientResponse = this.httpClient.send(HttpMethod.POST, getStorageBaseURL() + "schemas", this.getStorageSchemaFromJson(), headers, httpClient.getAccessToken());
        if (clientResponse.getType() != null) {
            LOGGER.info(String.format("Response status: %s, type: %s\nResponse body: %s", clientResponse.getStatus(), clientResponse.getType().toString(), indentatedResponseBody(clientResponse.getEntity(String.class))));
        }
    }

    public void deleteSchema(String kind) {
        ClientResponse clientResponse = this.httpClient.send(HttpMethod.DELETE, getStorageBaseURL() + "schemas/" + kind, null, headers, httpClient.getAccessToken());
        assertEquals(204, clientResponse.getStatus());
        if (clientResponse.getType() != null)
            LOGGER.info(String.format("Response status: %s, type: %s", clientResponse.getStatus(), clientResponse.getType().toString()));
    }

    public void addIndex() {
        this.elasticUtils.createIndex(this.index, this.getIndexMappingFromJson());
    }

    public void cleanupIndex(String kind) {
        this.indexerClientUtil.deleteIndex(kind);
    }

    private String getRecordFile() {
        return String.format("%s.json", this.recordFile);
    }

    private String getMappingFile() {
        return String.format("%s.mapping", this.mappingFile);
    }

    protected String getSchemaFile() {
        return String.format("%s.schema", this.schemaFile);
    }

    private List<Map<String, Object>> getRecordsFromTestFile() {
         try {
            String fileContent = FileHandler.readFile(getRecordFile());
            List<Map<String, Object>> records = new Gson().fromJson(
                    fileContent, new TypeToken<List<Map<String,Object>>>() {}.getType());

            for (Map<String, Object> testRecord : records) {
                testRecord.put("kind", this.kind);
                testRecord.put("legal", generateLegalTag());
                testRecord.put("x-acl", dataGroup);
                Acl acl = Acl.builder().viewers(viewerGroup).owners(ownerGroup).build();
                testRecord.put("acl", acl);
            }
            return records;
        } catch (Exception ex) {
            throw new AssertionError(ex.getMessage());
        }
    }

    private String getIndexMappingFromJson() {
        try {
            String fileContent = FileHandler.readFile(getMappingFile());
            JsonElement json = gson.fromJson(fileContent, JsonElement.class);
            return gson.toJson(json);
        } catch (Exception e) {
            throw new AssertionError(e.getMessage());
        }
    }

    private String getStorageSchemaFromJson() {
        try {
            String fileContent = FileHandler.readFile(getSchemaFile());
            fileContent = fileContent.replaceAll("KIND_VAL", this.kind);
            JsonElement json = gson.fromJson(fileContent, JsonElement.class);
            return gson.toJson(json);
        } catch (Exception e) {
            throw new AssertionError(e.getMessage());
        }
    }

    private Legal generateLegalTag() {
        Legal legal = new Legal();
        Set<String> legalTags = new HashSet<>();
        legalTags.add(getLegalTag());
        legal.setLegaltags(legalTags);
        Set<String> otherRelevantCountries = new HashSet<>();
        otherRelevantCountries.add(getOtherRelevantDataCountries());
        legal.setOtherRelevantDataCountries(otherRelevantCountries);
        return legal;
    }

}
