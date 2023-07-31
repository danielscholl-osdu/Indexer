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

import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.GetAliasesResponse;
import org.elasticsearch.client.IndicesClient;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.cluster.metadata.AliasMetadata;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.aggregations.Aggregations;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.search.ElasticIndexNameResolver;
import org.opengroup.osdu.indexer.model.IndexAliasesResult;
import org.opengroup.osdu.indexer.service.mock.BucketMock;
import org.opengroup.osdu.indexer.service.mock.TermMock;
import org.opengroup.osdu.indexer.util.ElasticClientHandler;
import org.springframework.context.annotation.Lazy;

import java.io.IOException;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(MockitoJUnitRunner.class)
public class IndexAliasServiceImplTest {
    @Mock
    private ElasticClientHandler elasticClientHandler;
    @Mock
    private ElasticIndexNameResolver elasticIndexNameResolver;
    @Mock
    @Lazy
    private JaxRsDpsLog log;
    @InjectMocks
    private IndexAliasServiceImpl sut;

    private RestHighLevelClient restHighLevelClient;
    private IndicesClient indicesClient;
    private GetAliasesResponse getAliasesResponse, getAliasesNotFoundResponse;


    private static String kind = "common:welldb:wellbore:1.2.0";
    private static String index = "common-welldb-wellbore-1.2.0";
    private static String alias = "a1234567890";

    @Before
    public void setup() {
        initMocks(this);
        indicesClient = mock(IndicesClient.class);
        restHighLevelClient = mock(RestHighLevelClient.class);
        getAliasesResponse = mock(GetAliasesResponse.class);
        getAliasesNotFoundResponse = mock(GetAliasesResponse.class);

    }

    @Test
    public void createIndexAlias_test_when_index_name_is_not_alias() throws IOException {
        AcknowledgedResponse updateAliasesResponse = new AcknowledgedResponse(true);
        when(elasticIndexNameResolver.getIndexNameFromKind(any())).thenReturn(index);
        when(elasticIndexNameResolver.getIndexAliasFromKind(any())).thenReturn(alias);
        when(elasticIndexNameResolver.isIndexAliasSupported(any())).thenReturn(true);
        when(restHighLevelClient.indices()).thenReturn(indicesClient);
        when(indicesClient.getAlias(any(GetAliasesRequest.class), any(RequestOptions.class))).thenReturn(getAliasesNotFoundResponse);
        when(getAliasesNotFoundResponse.status()).thenReturn(RestStatus.NOT_FOUND);
        when(indicesClient.updateAliases(any(IndicesAliasesRequest.class), any(RequestOptions.class))).thenReturn(updateAliasesResponse);

        boolean ok = sut.createIndexAlias(restHighLevelClient, kind);
        Assert.assertTrue(ok);
    }

    @Test
    public void createIndexAlias_test_when_index_name_is_alias() throws IOException {
        Map<String, Set<AliasMetadata>> aliases = new HashMap<>();
        Set<AliasMetadata> aliasMetadataSet = new HashSet<>();
        aliasMetadataSet.add(AliasMetadata.builder(index).build());
        aliases.put(index + "_123456789", aliasMetadataSet);

        AcknowledgedResponse updateAliasesResponse = new AcknowledgedResponse(true);
        when(elasticIndexNameResolver.getIndexNameFromKind(any())).thenReturn(index);
        when(elasticIndexNameResolver.getIndexAliasFromKind(any())).thenReturn(alias);
        when(elasticIndexNameResolver.isIndexAliasSupported(any())).thenReturn(true);
        when(restHighLevelClient.indices()).thenReturn(indicesClient);
        when(indicesClient.getAlias(any(GetAliasesRequest.class), any(RequestOptions.class))).thenReturn(getAliasesResponse);
        when(getAliasesResponse.status()).thenReturn(RestStatus.OK);
        when(getAliasesResponse.getAliases()).thenReturn(aliases);
        when(indicesClient.updateAliases(any(IndicesAliasesRequest.class), any(RequestOptions.class))).thenReturn(updateAliasesResponse);

        boolean ok = sut.createIndexAlias(restHighLevelClient, kind);
        Assert.assertTrue(ok);
    }

    @Test
    public void createIndexAlias_test_when_updateAliases_fails() throws IOException {
        AcknowledgedResponse updateAliasesResponse = new AcknowledgedResponse(false);
        when(elasticIndexNameResolver.getIndexNameFromKind(any())).thenReturn(index);
        when(elasticIndexNameResolver.getIndexAliasFromKind(any())).thenReturn(alias);
        when(elasticIndexNameResolver.isIndexAliasSupported(any())).thenReturn(true);
        when(restHighLevelClient.indices()).thenReturn(indicesClient);
        when(indicesClient.getAlias(any(GetAliasesRequest.class), any(RequestOptions.class))).thenReturn(getAliasesNotFoundResponse);
        when(getAliasesNotFoundResponse.status()).thenReturn(RestStatus.NOT_FOUND);
        when(indicesClient.updateAliases(any(IndicesAliasesRequest.class), any(RequestOptions.class))).thenReturn(updateAliasesResponse);

        boolean ok = sut.createIndexAlias(restHighLevelClient, kind);
        Assert.assertFalse(ok);
    }

    @Test
    public void createIndexAlias_test_when_updateAliases_throws_exception() throws IOException {
        when(elasticIndexNameResolver.getIndexNameFromKind(any())).thenReturn(index);
        when(elasticIndexNameResolver.getIndexAliasFromKind(any())).thenReturn(alias);
        when(elasticIndexNameResolver.isIndexAliasSupported(any())).thenReturn(true);
        when(restHighLevelClient.indices()).thenReturn(indicesClient);
        when(indicesClient.getAlias(any(GetAliasesRequest.class), any(RequestOptions.class))).thenReturn(getAliasesNotFoundResponse);
        when(getAliasesNotFoundResponse.status()).thenReturn(RestStatus.NOT_FOUND);
        when(indicesClient.updateAliases(any(IndicesAliasesRequest.class), any(RequestOptions.class))).thenThrow(IOException.class);

        boolean ok = sut.createIndexAlias(restHighLevelClient, kind);
        Assert.assertFalse(ok);
    }

    @Test
    public void createIndexAliasesForAll_test() throws IOException {
        String unsupportedKind = "common:welldb:wellbore:1";
        String unsupportedIndex = unsupportedKind.replace(":", "-");

        SearchResponse searchResponse = mock(SearchResponse.class);
        Aggregations aggregations = mock(Aggregations.class);
        TermMock terms = mock(TermMock.class);
        BucketMock bucket = mock(BucketMock.class);
        BucketMock bucket2 = mock(BucketMock.class);
        List<BucketMock> bucketList = Arrays.asList(bucket, bucket, bucket2);
        AcknowledgedResponse updateAliasesResponse = new AcknowledgedResponse(true);
        when(elasticIndexNameResolver.getIndexNameFromKind(any()))
                .thenAnswer(invocation ->{
                    String argument = invocation.getArgument(0);
                    return argument.replace(":", "-");
                });
        when(elasticIndexNameResolver.getIndexAliasFromKind(any())).thenReturn(alias);
        when(elasticIndexNameResolver.isIndexAliasSupported(any()))
                .thenAnswer(invocation ->{
                    String argument = invocation.getArgument(0);
                    return !unsupportedKind.equals(argument);
                });
        when(elasticClientHandler.createRestClient()).thenReturn(restHighLevelClient);
        when(restHighLevelClient.indices()).thenReturn(indicesClient);
        when(restHighLevelClient.search(any(SearchRequest.class), any(RequestOptions.class))).thenReturn(searchResponse);
        when(searchResponse.getAggregations()).thenReturn(aggregations);
        when(aggregations.get(anyString())).thenReturn(terms);
        when(terms.getBuckets()).thenReturn(bucketList);
        when(bucket.getKey()).thenReturn(kind);
        when(bucket2.getKey()).thenReturn(unsupportedKind);
        when(indicesClient.getAlias(any(GetAliasesRequest.class), any(RequestOptions.class))).thenReturn(getAliasesNotFoundResponse);
        when(getAliasesNotFoundResponse.status()).thenReturn(RestStatus.NOT_FOUND);
        when(indicesClient.updateAliases(any(IndicesAliasesRequest.class), any(RequestOptions.class))).thenReturn(updateAliasesResponse);

        IndexAliasesResult result = sut.createIndexAliasesForAll();
        Assert.assertEquals(2,result.getIndicesWithAliases().size());
        Assert.assertEquals(index, result.getIndicesWithAliases().get(0));
        Assert.assertEquals(1,result.getIndicesWithoutAliases().size());
        Assert.assertEquals(unsupportedIndex, result.getIndicesWithoutAliases().get(0));
    }
}
