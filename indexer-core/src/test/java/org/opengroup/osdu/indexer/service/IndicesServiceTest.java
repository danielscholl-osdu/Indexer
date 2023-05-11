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

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.*;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.elasticsearch.rest.RestStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.search.IndexInfo;
import org.opengroup.osdu.core.common.search.ElasticIndexNameResolver;
import org.opengroup.osdu.indexer.cache.PartitionSafeIndexCache;
import org.opengroup.osdu.indexer.util.ElasticClientHandler;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.springframework.context.annotation.Lazy;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(SpringRunner.class)
@PrepareForTest({RestHighLevelClient.class, IndicesClient.class, ClusterClient.class, EntityUtils.class})
public class IndicesServiceTest {
    @Mock
    private ElasticClientHandler elasticClientHandler;
    @Mock
    private ElasticIndexNameResolver elasticIndexNameResolver;
    @Mock
    private PartitionSafeIndexCache indicesExistCache;
    @Mock
    @Lazy
    private JaxRsDpsLog log;
    @Mock
    private RestClient restClient;
    @Mock
    private Response response;
    @Mock
    private HttpEntity httpEntity;
    @Mock
    private IndexAliasService indexAliasService;
    @InjectMocks
    private IndicesServiceImpl sut;

    private RestHighLevelClient restHighLevelClient;
    private IndicesClient indicesClient;
    private ClusterClient clusterClient;

    @Before
    public void setup() {
        initMocks(this);
        indicesClient = PowerMockito.mock(IndicesClient.class);
        clusterClient = PowerMockito.mock(ClusterClient.class);
        restHighLevelClient = PowerMockito.mock(RestHighLevelClient.class);
    }

    @Test
    public void create_elasticIndex() throws Exception {
        String index = "common-welldb-wellbore-1.2.0";
        CreateIndexResponse indexResponse = new CreateIndexResponse(true, true, index);
        AcknowledgedResponse acknowledgedResponse = new AcknowledgedResponse(true);

        when(elasticIndexNameResolver.getIndexNameFromKind(any())).thenReturn(index);
        when(restHighLevelClient.indices()).thenReturn(indicesClient);
        when(indicesClient.create(any(CreateIndexRequest.class), any(RequestOptions.class))).thenReturn(indexResponse);
        when(indicesClient.updateAliases(any(IndicesAliasesRequest.class), any(RequestOptions.class))).thenReturn(acknowledgedResponse);

        boolean response = this.sut.createIndex(restHighLevelClient, index, null, "anytype", new HashMap<>());
        assertTrue(response);
        when(this.indicesExistCache.get(index)).thenReturn(true);
        verify(this.indexAliasService, times(1)).createIndexAlias(any(), any());
    }

    @Test
    public void create_elasticIndex_fail() throws Exception {
        String index = "common-welldb-wellbore-1.2.0";
        CreateIndexResponse indexResponse = new CreateIndexResponse(false, false, index);

        when(restHighLevelClient.indices()).thenReturn(indicesClient);
        when(indicesClient.create(any(CreateIndexRequest.class), any(RequestOptions.class))).thenReturn(indexResponse);
        boolean response = this.sut.createIndex(restHighLevelClient, index, null, "anytype", new HashMap<>());
        assertFalse(response);
        verify(this.indicesExistCache, times(0)).put(any(), any());
        verify(this.indicesClient, times(0)).updateAliases(any(IndicesAliasesRequest.class), any(RequestOptions.class));
    }

    @Test
    public void create_existingElasticIndex() throws Exception {
        String index = "common-welldb-wellbore-1.2.0";
        ElasticsearchStatusException elasticsearchStatusException = new ElasticsearchStatusException("resource_already_exists_exception", RestStatus.BAD_REQUEST);

        when(restHighLevelClient.indices()).thenReturn(indicesClient);
        when(indicesClient.create(any(CreateIndexRequest.class), any(RequestOptions.class))).thenThrow(elasticsearchStatusException);
        boolean response = this.sut.createIndex(restHighLevelClient, index, null, "anytype", new HashMap<>());
        assertTrue(response);
        verify(this.indicesExistCache, times(1)).put(any(), any());
        verify(this.indicesClient, times(0)).updateAliases(any(IndicesAliasesRequest.class), any(RequestOptions.class));
    }

    @Test
    public void delete_existingElasticIndex() throws Exception {
        AcknowledgedResponse indexResponse = new AcknowledgedResponse(true);
        GetIndexResponse getIndexResponse = PowerMockito.mock(GetIndexResponse.class);
        String[] indices = {"anyIndex"};

        when(elasticClientHandler.createRestClient()).thenReturn(restHighLevelClient);
        doReturn(indicesClient).when(restHighLevelClient).indices();
        doReturn(indexResponse).when(indicesClient).delete(any(), any(RequestOptions.class));
        doReturn(getIndexResponse).when(indicesClient).get(any(GetIndexRequest.class), any(RequestOptions.class));
        doReturn(indices).when(getIndexResponse).getIndices();
        boolean response = this.sut.deleteIndex("anyIndex");
        assertTrue(response);
    }

    @Test
    public void delete_existingElasticIndex_usingSameClient() throws Exception {
        AcknowledgedResponse indexResponse = new AcknowledgedResponse(true);
        GetIndexResponse getIndexResponse = PowerMockito.mock(GetIndexResponse.class);
        String[] indices = {"anyIndex"};

        when(elasticClientHandler.createRestClient()).thenReturn(restHighLevelClient);
        doReturn(indicesClient).when(restHighLevelClient).indices();
        doReturn(indexResponse).when(indicesClient).delete(any(), any(RequestOptions.class));
        doReturn(getIndexResponse).when(indicesClient).get(any(GetIndexRequest.class), any(RequestOptions.class));
        doReturn(indices).when(getIndexResponse).getIndices();
        boolean response = this.sut.deleteIndex(restHighLevelClient, "anyIndex");
        assertTrue(response);
    }

    @Test
    public void should_throw_internalServerException_delete_isNotAcknowledged() throws Exception {
        AcknowledgedResponse indexResponse = new AcknowledgedResponse(false);
        GetIndexResponse getIndexResponse = PowerMockito.mock(GetIndexResponse.class);
        String[] indices = {"anyIndex"};
        when(elasticClientHandler.createRestClient()).thenReturn(restHighLevelClient);
        doReturn(indicesClient).when(restHighLevelClient).indices();
        doReturn(indexResponse).when(indicesClient).delete(any(), any(RequestOptions.class));
        doReturn(getIndexResponse).when(indicesClient).get(any(GetIndexRequest.class), any(RequestOptions.class));
        doReturn(indices).when(getIndexResponse).getIndices();

        try {
            this.sut.deleteIndex("anyIndex");
            fail("Should not succeed!");
        } catch (AppException e) {
            assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getError().getCode());
            assertEquals("Could not delete index anyIndex", e.getError().getMessage());
            assertEquals("Index deletion error", e.getError().getReason());
        } catch (Exception e) {
            fail("Should not throw this exception " + e.getMessage());
        }
    }

    @Test
    public void should_throwAppException_when_delete_existingElasticIndex_and_backupIsRunning() throws Exception {
        ElasticsearchStatusException exception = new ElasticsearchStatusException(
                "Cannot delete indices that are being snapshotted: [[anyIndex/8IXuPeFnTJGEu_LjjXrHwA]]. Try again after snapshot finishes or cancel the currently running snapshot.", RestStatus.BAD_REQUEST);
        GetIndexResponse getIndexResponse = PowerMockito.mock(GetIndexResponse.class);
        String[] indices = {"anyIndex"};
        when(elasticClientHandler.createRestClient()).thenReturn(restHighLevelClient);
        doReturn(indicesClient).when(restHighLevelClient).indices();
        doThrow(exception).when(indicesClient).delete(any(), any(RequestOptions.class));
        doReturn(getIndexResponse).when(indicesClient).get(any(GetIndexRequest.class), any(RequestOptions.class));
        doReturn(indices).when(getIndexResponse).getIndices();

        try {
            this.sut.deleteIndex("anyIndex");
            fail("Should not succeed!");
        } catch (AppException e) {
            assertEquals(HttpStatus.SC_CONFLICT, e.getError().getCode());
            assertEquals("Unable to delete the index because it is currently locked. Try again in few minutes.", e.getError().getMessage());
            assertEquals("Index deletion error", e.getError().getReason());
        } catch (Exception e) {
            fail("Should not throw this exception " + e.getMessage());
        }
    }

    @Test
    public void should_throwAppException_when_delete_nonExistent_index() throws Exception {
        ElasticsearchStatusException exception = new ElasticsearchStatusException("no such index", RestStatus.NOT_FOUND);
        when(elasticClientHandler.createRestClient()).thenReturn(restHighLevelClient);
        doReturn(indicesClient).when(restHighLevelClient).indices();
        doThrow(exception).when(indicesClient).get(any(GetIndexRequest.class), any(RequestOptions.class));

        try {
            this.sut.deleteIndex("anyIndex");
            fail("Should not succeed!");
        } catch (AppException e) {
            assertEquals(HttpStatus.SC_NOT_FOUND, e.getError().getCode());
            assertEquals("Index anyIndex not found", e.getError().getMessage());
            assertEquals("Index deletion error", e.getError().getReason());
        } catch (Exception e) {
            fail("Should not throw this exception " + e.getMessage());
        }
    }

    @Test
    public void should_get_valid_indexInfo() throws IOException {
        String responseJson = "[" +
                "  {" +
                "    \"index\": \"tenant1-aapg-file-1.0.0\"," +
                "    \"docs.count\": \"92\"," +
                "    \"creation.date\": \"1545912860994\"" +
                "  }" +
                "]";
        Request request = new Request("GET", "/_cat/indices/*,-.*?h=index,docs.count,creation.date&s=docs.count:asc&format=json");
        StringEntity entity = new StringEntity(responseJson, ContentType.APPLICATION_JSON);
        when(this.restHighLevelClient.getLowLevelClient()).thenReturn(this.restClient);
        when(this.restClient.performRequest(request)).thenReturn(response);
        when(this.response.getEntity()).thenReturn(entity);

        List<IndexInfo> infos = this.sut.getIndexInfo(this.restHighLevelClient, "");
        assertNotNull(infos);
        assertEquals(1, infos.size());
    }

    @Test
    public void should_get_valid_indexInfoByPattern() throws IOException {
        String responseJson = "[" +
                "  {" +
                "    \"index\": \"tenant1-aapg-file-1.0.0\"," +
                "    \"docs.count\": \"92\"," +
                "    \"creation.date\": \"1545912860994\"" +
                "  }," +
                "  {" +
                "    \"index\": \"tenant1-aapg-document-1.0.0\"," +
                "    \"docs.count\": \"0\"," +
                "    \"creation.date\": \"1545912868416\"" +
                "  }" +
                "]";
        Request request = new Request("GET", "/_cat/indices/tenant1-aapg-*?h=index,docs.count,creation.date&format=json");
        StringEntity entity = new StringEntity(responseJson, ContentType.APPLICATION_JSON);
        when(this.restHighLevelClient.getLowLevelClient()).thenReturn(this.restClient);
        when(this.restClient.performRequest(request)).thenReturn(response);
        when(this.response.getEntity()).thenReturn(entity);

        List<IndexInfo> infos = this.sut.getIndexInfo(this.restHighLevelClient, "tenant1-aapg-*");
        assertNotNull(infos);
        assertEquals(2, infos.size());
    }

    @Test
    public void should_properly_deserialize_indices_get_response() {
        String jsonResponse = "[{\"index\":\"tenant1-test-hello-1.0.1\",\"docs.count\":\"1\",\"creation.date\":\"1551996907769\"}]";

        final Type typeOf = new TypeToken<List<IndexInfo>>() {}.getType();
        List<IndexInfo> indicesList = new Gson().fromJson(jsonResponse, typeOf);

        assertEquals(1, indicesList.size());
        assertEquals("tenant1-test-hello-1.0.1", indicesList.get(0).getName());
        assertEquals("1", indicesList.get(0).getDocumentCount());
        assertEquals("1551996907769", indicesList.get(0).getCreationDate());
    }

    @Test
    public void should_returnTrue_indexExistInCache() throws IOException {
        when(this.indicesExistCache.get("anyIndex")).thenReturn(true);

        boolean result = this.sut.isIndexExist(any(RestHighLevelClient.class), "anyIndex");

        assertTrue(result);
    }

    @Test
    public void should_getIndexExist_whenIndexNotInCache() throws IOException {
        when(this.indicesExistCache.get("anyIndex")).thenReturn(false);

        doReturn(indicesClient).when(restHighLevelClient).indices();
        doReturn(true).when(indicesClient).exists(any(GetIndexRequest.class), any(RequestOptions.class));

        boolean result = this.sut.isIndexExist(restHighLevelClient, "anyIndex");

        assertTrue(result);
        verify(this.indicesExistCache, times(1)).get("anyIndex");
        verify(this.indicesExistCache, times(1)).put("anyIndex", true);
    }

    @Test
    public void should_getIndexReadyStatus_whenIndexInCache() throws IOException {
        when(this.indicesExistCache.get("anyIndex")).thenReturn(true);

        boolean result = this.sut.isIndexReady(any(RestHighLevelClient.class), "anyIndex");

        assertTrue(result);
    }

    @Test
    public void should_getIndexReadyStatus_whenIndexNotInCache() throws IOException {
        when(this.indicesExistCache.get("anyIndex")).thenReturn(false);
        doReturn(indicesClient).when(restHighLevelClient).indices();
        doReturn(true).when(indicesClient).exists(any(GetIndexRequest.class), any(RequestOptions.class));

        ClusterHealthResponse healthResponse = mock(ClusterHealthResponse.class);
        when(healthResponse.status()).thenReturn(RestStatus.OK);
        doReturn(clusterClient).when(restHighLevelClient).cluster();
        doReturn(healthResponse).when(clusterClient).health(any(ClusterHealthRequest.class), any(RequestOptions.class));

        boolean result = this.sut.isIndexReady(restHighLevelClient, "anyIndex");

        assertTrue(result);
        verify(this.indicesExistCache, times(1)).get("anyIndex");
        verify(this.indicesExistCache, times(1)).put("anyIndex", true);
    }
}
