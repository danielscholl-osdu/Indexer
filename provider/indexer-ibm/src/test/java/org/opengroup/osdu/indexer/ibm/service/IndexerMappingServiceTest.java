/* Licensed Materials - Property of IBM              */
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/
package org.opengroup.osdu.indexer.ibm.service;

import org.apache.http.StatusLine;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.IndicesClient;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opengroup.osdu.core.common.model.indexer.IndexSchema;
import org.opengroup.osdu.core.common.model.search.RecordMetaAttribute;
import org.opengroup.osdu.indexer.service.IndexerMappingServiceImpl;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@Ignore
@RunWith(SpringRunner.class)
public class IndexerMappingServiceTest {

    private final String kind = "tenant:test:test:1.0.0";
    private final String index = "tenant-test-test-1.0.0";
    private final String type = "test";
    private final String mappingValid = "{\"dynamic\":false,\"properties\":{\"data\":{\"properties\":{\"Location\":{\"type\":\"geo_point\"}}},\"id\":{\"type\":\"keyword\"}}}";

    @Mock
    private RestClient restClient;
    @Mock
    private Response response;
    @Mock
    private StatusLine statusLine;

    @InjectMocks
    private IndexerMappingServiceImpl sut;

    @InjectMocks
    private RestHighLevelClient restHighLevelClient;

    @InjectMocks
    private IndexSchema indexSchema;
    @InjectMocks
    private IndicesClient indicesClient;

    @InjectMocks
    private AcknowledgedResponse mappingResponse;

    @Before
    public void setup() throws IOException {
        Map<String, Object> dataMapping = new HashMap<>();
        dataMapping.put("Location", "geo_point");
        Map<String, Object> metaMapping = new HashMap<>();
        metaMapping.put(RecordMetaAttribute.ID.getValue(), "keyword");
        this.indexSchema = IndexSchema.builder().kind(kind).type(type).dataSchema(dataMapping).metaSchema(metaMapping)
                .build();

        this.indicesClient = mock(IndicesClient.class);
        this.restHighLevelClient = mock(RestHighLevelClient.class);

        when(this.restHighLevelClient.getLowLevelClient()).thenReturn(restClient);
        when(this.restClient.performRequest(ArgumentMatchers.any())).thenReturn(response);
        when(this.response.getStatusLine()).thenReturn(statusLine);
        when(this.statusLine.getStatusCode()).thenReturn(200);
    }

    @Test
    public void should_returnValidMapping_givenFalseMerge_createMappingTest() {
        try {
            String mapping = this.sut.createMapping(restHighLevelClient, indexSchema, index, false);
            assertEquals(mappingValid, mapping);
        } catch (Exception e) {
            fail("Should not throw this exception" + e.getMessage());
        }
    }

    @Test
    public void should_returnValidMapping_givenTrueMerge_createMappingTest() {
        try {
            doReturn(this.indicesClient).when(this.restHighLevelClient).indices();
            doReturn(mappingResponse).when(this.indicesClient).putMapping(ArgumentMatchers.any(PutMappingRequest.class), ArgumentMatchers.any(RequestOptions.class));
            String mapping = this.sut.createMapping(this.restHighLevelClient, this.indexSchema, this.index, true);
            assertEquals(this.mappingValid, mapping);
        } catch (Exception e) {
            fail("Should not throw this exception" + e.getMessage());
        }
    }

    @Test
    public void should_returnValidMapping_givenExistType_createMappingTest() {
        try {
            doReturn(this.indicesClient).when(this.restHighLevelClient).indices();
            doReturn(mappingResponse).when(this.indicesClient).putMapping(ArgumentMatchers.any(PutMappingRequest.class), ArgumentMatchers.any(RequestOptions.class));
            IndexerMappingServiceImpl indexerMappingServiceLocal = spy(new IndexerMappingServiceImpl());
            doReturn(false).when(indexerMappingServiceLocal).isTypeExist(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
            String mapping = this.sut.createMapping(this.restHighLevelClient, this.indexSchema, this.index, true);
            assertEquals(this.mappingValid, mapping);
        } catch (Exception e) {
            fail("Should not throw this exception" + e.getMessage());
        }
    }
}
