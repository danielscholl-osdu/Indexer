package org.opengroup.osdu.indexer.util;


import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.search.ElasticIndexNameResolver;
import org.opengroup.osdu.indexer.model.SearchRecord;
import java.lang.reflect.Type;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SearchClientTest {

    @InjectMocks
    SearchClient sut;

    @Mock
    private ElasticClientHandler elasticClientHandler;

    @Mock
    private ElasticIndexNameResolver elasticIndexNameResolver;

    @Mock
    private JaxRsDpsLog logger;

    @Mock
    private ElasticsearchClient client;

    @Mock
    private Query query;

    @Mock
    private HitsMetadata<Map<String, Object>> searchHits;

    @Mock
    private Hit<Map<String, Object>> searchHit;

    private static final String kind = "a:b:c:1.0.0";

    private static final String pitId = "pitId";

    @Before
    public void init() {
        doReturn(client).when(elasticClientHandler).getOrCreateRestClient();
        when(elasticIndexNameResolver.getIndexNameFromKind(anyString())).thenAnswer(invocation -> {
            String kind = invocation.getArgument(0);
            return kind.replace(":", "-").toLowerCase();
        });
    }

    @Test
    public void search_with_normalQuery_whenSearchHitsIsNotEmpty() throws Exception {
        List<Hit<Map<String, Object>>> hits = new ArrayList<>();
        hits.add(searchHit);
        Map<String, Object> hitFields = new HashMap<>();

        SearchResponse searchResponse = mock(SearchResponse.class);
        doReturn(searchResponse).when(client).search(any(SearchRequest.class), eq((Type)Map.class));
        doReturn(searchHits).when(searchResponse).hits();
        doReturn(hits).when(searchHits).hits();
        doReturn(hitFields).when(searchHit).source();

        // act
        List<SearchRecord> records = sut.search(kind, query, null, null, -1);

        // assert
        ArgumentCaptor<SearchRequest> searchRequestArgumentCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(client, times(1)).search(searchRequestArgumentCaptor.capture(), eq((Type)Map.class));
        verify(client, times(0)).openPointInTime(any(OpenPointInTimeRequest.class));
        SearchRequest searchRequest = searchRequestArgumentCaptor.getValue();
        assertNull(searchRequest.pit());
        assertEquals(searchRequest.sort().size(), 0);
        assertEquals(searchRequest.source().filter().includes().size(), 0);
        assertEquals(records.size(), 1);
    }

    @Test
    public void search_with_whenSortOptionsAndReturnedFieldsAreNotEmpty() throws Exception {
        List<Hit<Map<String, Object>>> hits = new ArrayList<>();
        hits.add(searchHit);
        Map<String, Object> hitFields = new HashMap<>();

        SearchResponse searchResponse = mock(SearchResponse.class);
        doReturn(searchResponse).when(client).search(any(SearchRequest.class), eq((Type)Map.class));
        doReturn(searchHits).when(searchResponse).hits();
        doReturn(hits).when(searchHits).hits();
        doReturn(hitFields).when(searchHit).source();
        List<SortOptions> sortOptions = List.of(SortOptions.of(so -> so.score(s -> s.order(SortOrder.Desc))));
        List<String> returnedFields = List.of("id", "kind");

        // act
        List<SearchRecord> records = sut.search(kind, query, sortOptions, returnedFields, -1);

        // assert
        ArgumentCaptor<SearchRequest> searchRequestArgumentCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(client, times(1)).search(searchRequestArgumentCaptor.capture(), eq((Type)Map.class));
        verify(client, times(0)).openPointInTime(any(OpenPointInTimeRequest.class));
        SearchRequest searchRequest = searchRequestArgumentCaptor.getValue();
        assertEquals(searchRequest.sort(), sortOptions);
        assertEquals(searchRequest.source().filter().includes().size(), 2);
        assertTrue(searchRequest.source().filter().includes().contains("id"));
        assertTrue(searchRequest.source().filter().includes().contains("kind"));
        assertNull(searchRequest.pit());
        assertEquals(records.size(), 1);
    }

    @Test
    public void search_with_queryWithPIT_whenSearchHitsIsNotEmpty() throws Exception {
        List<List<Hit<Map<String, Object>>>> batches = new ArrayList<>();
        int totalRecordCount = 5100;
        List<Hit<Map<String, Object>>> hits = new ArrayList<>();
        for(int i = 0; i < totalRecordCount; i++) {
            if(i % 5000 == 0 && i > 0) {
                batches.add(hits);
                hits = new ArrayList<>();
            }
            hits.add(searchHit);
        }
        batches.add(hits);

        Map<String, Object> hitFields = new HashMap<>();
        Map<String, Integer> searchCallsCount = new HashMap<>();
        searchCallsCount.put("Count", 0);
        List<FieldValue> fieldValues = new ArrayList<>();

        OpenPointInTimeResponse openResponse = mock(OpenPointInTimeResponse.class);
        SearchResponse searchResponse = mock(SearchResponse.class);
        doReturn(openResponse).when(client).openPointInTime(any(OpenPointInTimeRequest.class));
        when(client.search(any(SearchRequest.class), eq((Type)Map.class))).thenAnswer(
                invocationOnMock -> {
                    searchCallsCount.put("Count", searchCallsCount.get("Count") + 1);
                    return searchResponse;
                });
        doReturn(pitId).when(openResponse).id();
        doReturn(searchHits).when(searchResponse).hits();
        when(searchHits.hits()).thenAnswer(invocation -> {
            // First call is normal query
            // The second and third calls are queries with search_after and PIT
            if(searchCallsCount.get("Count") < 3) {
                return batches.get(0);
            }
            else if(searchCallsCount.get("Count") == 3) {
                if(batches.size() == 2) {
                    batches.remove(0);
                }
                return batches.get(0);
            }
            else {
                return new ArrayList<>();
            }
        });
        doReturn(hitFields).when(searchHit).source();
        doReturn(fieldValues).when(searchHit).sort();
        List<SortOptions> sortOptions = List.of(SortOptions.of(so -> so.score(s -> s.order(SortOrder.Desc))));
        List<String> returnedFields = List.of("id", "kind");

        // act
        List<SearchRecord> records = sut.search(kind, query, sortOptions, returnedFields, -1);

        // assert
        ArgumentCaptor<SearchRequest> searchRequestArgumentCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(client, times(3)).search(searchRequestArgumentCaptor.capture(), eq((Type)Map.class));
        verify(client, times(1)).openPointInTime(any(OpenPointInTimeRequest.class));
        verify(client, times(1)).closePointInTime(any(ClosePointInTimeRequest.class));
        SearchRequest searchRequest = searchRequestArgumentCaptor.getValue();
        assertEquals(searchRequest.sort(), sortOptions);
        assertEquals(searchRequest.source().filter().includes().size(), 2);
        assertTrue(searchRequest.source().filter().includes().contains("id"));
        assertTrue(searchRequest.source().filter().includes().contains("kind"));
        assertEquals(searchRequest.pit().id(), pitId);
        assertEquals(searchRequest.searchAfter(), fieldValues);
        assertEquals(records.size(), totalRecordCount);
    }

    @Test
    public void search_throws_exception_whenKindIsNullOrEmpty() {
        Assert.assertThrows(Exception.class, () -> sut.search((String)null, query, null, null, -1));
        Assert.assertThrows(Exception.class, () -> sut.search("", query, null, null, -1));
    }

    @Test
    public void search_throws_exception_whenKindsAreNullOrEmpty() {
        Assert.assertThrows(Exception.class, () -> sut.search((List)null, query, null, null, -1));
        Assert.assertThrows(Exception.class, () -> sut.search(new ArrayList<>(), query, null, null, -1));
    }

    @Test
    public void search_throws_exception_whenQueryIsNull() {
        Assert.assertThrows(Exception.class, () -> sut.search(kind, null, null, null, -1));
        Assert.assertThrows(Exception.class, () -> sut.search(List.of(kind), null, null, null, -1));
    }
}
