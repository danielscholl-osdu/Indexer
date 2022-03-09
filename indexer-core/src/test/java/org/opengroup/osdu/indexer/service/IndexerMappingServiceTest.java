// Copyright © Schlumberger
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

import com.google.gson.Gson;
import org.apache.http.StatusLine;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.*;
import org.elasticsearch.client.indices.PutMappingRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.indexer.IndexSchema;
import org.opengroup.osdu.core.common.model.search.RecordMetaAttribute;
import org.opengroup.osdu.indexer.cache.PartitionSafeIndexCache;
import org.opengroup.osdu.indexer.util.ElasticClientHandler;
import org.opengroup.osdu.indexer.util.TypeMapper;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({RestHighLevelClient.class, IndicesClient.class})
public class IndexerMappingServiceTest {

    private final String kind = "tenant:test:test:1.0.0";
    private final String index = "tenant-test-test-1.0.0";
    private final String type = "test";
    private final String validMapping = "{\"dynamic\":false,\"properties\":{\"data\":{\"properties\":{\"Msg\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"null_value\":\"null\",\"ignore_above\":256,\"type\":\"keyword\"}}},\"Location\":{\"type\":\"geo_point\"}}},\"authority\":{\"type\":\"constant_keyword\",\"value\":\"tenant\"},\"id\":{\"type\":\"keyword\"},\"acl\":{\"properties\":{\"viewers\":{\"type\":\"keyword\"},\"owners\":{\"type\":\"keyword\"}}}}}";
    private final String emptyDataValidMapping = "{\"dynamic\":false,\"properties\":{\"id\":{\"type\":\"keyword\"},\"acl\":{\"properties\":{\"viewers\":{\"type\":\"keyword\"},\"owners\":{\"type\":\"keyword\"}}},\"authority\":{\"type\":\"constant_keyword\",\"value\":\"tenant\"}}}";

    @Mock
    private RestClient restClient;
    @Mock
    private Response response;
    @Mock
    private StatusLine statusLine;
    @Mock
    private JaxRsDpsLog log;
    @Mock
    private ElasticClientHandler elasticClientHandler;
    @Mock
    private PartitionSafeIndexCache indexCache;
    @Mock
    private IMappingService mappingService;
    @InjectMocks
    private IndexerMappingServiceImpl sut;

    private IndexSchema indexSchema;
    private IndicesClient indicesClient;
    private RestHighLevelClient restHighLevelClient;

    @Before
    public void setup() throws IOException {
        initMocks(this);

        this.indexSchema = IndexSchema.builder().kind(kind).type(type).dataSchema(getDataAttributeMapping()).metaSchema(getMetaAttributeMapping()).build();

        this.indicesClient = PowerMockito.mock(IndicesClient.class);
        this.restHighLevelClient = PowerMockito.mock(RestHighLevelClient.class);

        when(this.restHighLevelClient.getLowLevelClient()).thenReturn(restClient);
        when(this.restClient.performRequest(any())).thenReturn(response);
        when(this.response.getStatusLine()).thenReturn(statusLine);
        when(this.statusLine.getStatusCode()).thenReturn(200);
    }

    private Map<String, Object> getMetaAttributeMapping() {
        Map<String, Object> metaMapping = new HashMap<>();
        metaMapping.put(RecordMetaAttribute.ID.getValue(), "keyword");
        metaMapping.put(RecordMetaAttribute.ACL.getValue(), TypeMapper.getIndexerType(RecordMetaAttribute.ACL));
        metaMapping.put(RecordMetaAttribute.AUTHORITY.getValue(), TypeMapper.getIndexerType(RecordMetaAttribute.AUTHORITY));
        return metaMapping;
    }

    private Map<String, Object> getDataAttributeMapping() {
        Map<String, Object> dataMapping = new HashMap<>();
        dataMapping.put("Location", "geo_point");
        dataMapping.put("Msg", "text");
        return dataMapping;
    }

    @Test
    public void should_returnValidMapping_givenFalseMerge_createMappingTest() {
        try {
            String mapping = this.sut.createMapping(restHighLevelClient, indexSchema, index, false);
            assertEquals(validMapping, mapping);
        } catch (Exception e) {
            fail("Should not throw this exception" + e.getMessage());
        }
    }

    @Test
    public void should_returnValidMapping_givenTrueMerge_createMappingTest() {
        try {
            AcknowledgedResponse mappingResponse = new AcknowledgedResponse(true);
            doReturn(this.indicesClient).when(this.restHighLevelClient).indices();
            doReturn(mappingResponse).when(this.indicesClient).putMapping(any(PutMappingRequest.class), any(RequestOptions.class));

            String mapping = this.sut.createMapping(this.restHighLevelClient, this.indexSchema, this.index, true);
            assertEquals(this.validMapping, mapping);
        } catch (Exception e) {
            fail("Should not throw this exception" + e.getMessage());
        }
    }

    @Test
    public void should_returnValidMapping_givenExistType_createMappingTest() {
        try {
            AcknowledgedResponse mappingResponse = new AcknowledgedResponse(true);
            doReturn(this.indicesClient).when(this.restHighLevelClient).indices();
            doReturn(mappingResponse).when(this.indicesClient).putMapping(any(PutMappingRequest.class), any(RequestOptions.class));

            IndexerMappingServiceImpl indexerMappingServiceLocal = spy(new IndexerMappingServiceImpl());
            doReturn(false).when(indexerMappingServiceLocal).isTypeExist(any(), any(), any());
            String mapping = this.sut.createMapping(this.restHighLevelClient, this.indexSchema, this.index, true);
            assertEquals(this.validMapping, mapping);
        } catch (Exception e) {
            fail("Should not throw this exception" + e.getMessage());
        }
    }

    @Test
    public void should_returnDocumentMapping_givenValidIndexSchema() {
        try {
            Map<String, Object> documentMapping = this.sut.getIndexMappingFromRecordSchema(this.indexSchema);
            String documentMappingJson = new Gson().toJson(documentMapping);
            assertEquals(this.validMapping, documentMappingJson);
        } catch (Exception e) {
            fail("Should not throw this exception" + e.getMessage());
        }
    }

    @Test
    public void should_returnDocumentMapping_givenValidEmptyDataIndexSchema() {
        try {
            IndexSchema emptyDataIndexSchema = IndexSchema.builder().kind(kind).type(type).metaSchema(getMetaAttributeMapping()).build();
            Map<String, Object> documentMapping = this.sut.getIndexMappingFromRecordSchema(emptyDataIndexSchema);
            String documentMappingJson = new Gson().toJson(documentMapping);
            assertEquals(this.emptyDataValidMapping, documentMappingJson);
        } catch (Exception e) {
            fail("Should not throw this exception" + e.getMessage());
        }
    }

    @Test
    public void should_returnCachedStatus_givenUpdatedIndex() throws Exception {
        final String cacheKey = String.format("metaAttributeMappingSynced-%s", index);
        when(this.indexCache.get(cacheKey)).thenReturn(true);

        this.sut.syncIndexMappingIfRequired(restHighLevelClient, index);

        verifyNoMoreInteractions(this.mappingService);
    }

    @Test
    public void should_applyNoUpdate_givenUpdateIndex() throws Exception {
        final String cacheKey = String.format("metaAttributeMappingSynced-%s", index);
        final String mapping = "{\"dynamic\":\"false\",\"properties\":{\"acl\":{\"properties\":{\"owners\":{\"type\":\"keyword\"},\"viewers\":{\"type\":\"keyword\"}}},\"ancestry\":{\"properties\":{\"parents\":{\"type\":\"keyword\"}}},\"authority\":{\"type\":\"constant_keyword\",\"value\":\"opendes\"},\"createTime\":{\"type\":\"date\"},\"createUser\":{\"type\":\"keyword\"},\"data\":{\"properties\":{\"message\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\",\"null_value\":\"null\",\"ignore_above\":256}}}}},\"id\":{\"type\":\"keyword\"},\"index\":{\"properties\":{\"lastUpdateTime\":{\"type\":\"date\"},\"statusCode\":{\"type\":\"integer\"},\"trace\":{\"type\":\"text\"}}},\"kind\":{\"type\":\"keyword\"},\"legal\":{\"properties\":{\"legaltags\":{\"type\":\"keyword\"},\"otherRelevantDataCountries\":{\"type\":\"keyword\"},\"status\":{\"type\":\"keyword\"}}},\"modifyTime\":{\"type\":\"date\"},\"modifyUser\":{\"type\":\"keyword\"},\"namespace\":{\"type\":\"keyword\"},\"source\":{\"type\":\"constant_keyword\",\"value\":\"test\"},\"tags\":{\"type\":\"flattened\"},\"type\":{\"type\":\"keyword\"},\"version\":{\"type\":\"long\"},\"x-acl\":{\"type\":\"keyword\"}}}";
        when(this.mappingService.getIndexMapping(restHighLevelClient, index)).thenReturn(mapping);

        this.sut.syncIndexMappingIfRequired(restHighLevelClient, index);

        verify(this.indexCache, times(1)).get(cacheKey);
        verify(this.indexCache, times(1)).put(cacheKey, true);
    }

    @Test
    public void should_applyUpdate_givenExistingIndex() throws Exception {
        final String cacheKey = String.format("metaAttributeMappingSynced-%s", index);
        final String mapping = "{\"dynamic\":\"false\",\"properties\":{\"acl\":{\"properties\":{\"owners\":{\"type\":\"keyword\"},\"viewers\":{\"type\":\"keyword\"}}},\"ancestry\":{\"properties\":{\"parents\":{\"type\":\"keyword\"}}},\"data\":{\"properties\":{\"message\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\",\"null_value\":\"null\",\"ignore_above\":256}}}}},\"id\":{\"type\":\"keyword\"},\"index\":{\"properties\":{\"lastUpdateTime\":{\"type\":\"date\"},\"statusCode\":{\"type\":\"integer\"},\"trace\":{\"type\":\"text\"}}},\"kind\":{\"type\":\"keyword\"},\"legal\":{\"properties\":{\"legaltags\":{\"type\":\"keyword\"},\"otherRelevantDataCountries\":{\"type\":\"keyword\"},\"status\":{\"type\":\"keyword\"}}},\"namespace\":{\"type\":\"keyword\"},\"tags\":{\"type\":\"flattened\"},\"type\":{\"type\":\"keyword\"},\"version\":{\"type\":\"long\"},\"x-acl\":{\"type\":\"keyword\"}}}";
        when(this.mappingService.getIndexMapping(restHighLevelClient, index)).thenReturn(mapping);

        AcknowledgedResponse mappingResponse = new AcknowledgedResponse(true);
        doReturn(this.indicesClient).when(this.restHighLevelClient).indices();
        doReturn(mappingResponse).when(this.indicesClient).putMapping(any(PutMappingRequest.class), any(RequestOptions.class));

        this.sut.syncIndexMappingIfRequired(restHighLevelClient, index);

        verify(this.indexCache, times(1)).get(cacheKey);
        verify(this.indexCache, times(1)).put(cacheKey, true);
        verify(this.indicesClient, times(1)).putMapping(any(PutMappingRequest.class), any(RequestOptions.class));
    }
}