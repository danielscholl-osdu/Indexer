/* Licensed Materials - Property of IBM              */		
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/
package org.opengroup.osdu.indexer.ibm.service;

import org.apache.http.StatusLine;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.mapping.get.GetFieldMappingsRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetFieldMappingsResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.bulk.BulkItemResponse.Failure;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.*;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opengroup.osdu.core.common.model.indexer.IndexSchema;
import org.opengroup.osdu.indexer.service.IndexerMappingServiceImpl;
import org.opengroup.osdu.core.common.model.search.RecordMetaAttribute;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.indexer.util.ElasticClientHandler;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@Ignore
@RunWith(SpringRunner.class)
@PrepareForTest({ RestHighLevelClient.class, IndicesClient.class })
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

	@Mock
    private ElasticClientHandler elasticClientHandler;

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

		this.indicesClient = PowerMockito.mock(IndicesClient.class);
		this.restHighLevelClient = PowerMockito.mock(RestHighLevelClient.class);

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
			IndexerMappingServiceImpl indexerMappingServiceLocal = PowerMockito.spy(new IndexerMappingServiceImpl());
			doReturn(false).when(indexerMappingServiceLocal).isTypeExist(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
			String mapping = this.sut.createMapping(this.restHighLevelClient, this.indexSchema, this.index, true);
			assertEquals(this.mappingValid, mapping);
		} catch (Exception e) {
			fail("Should not throw this exception" + e.getMessage());
		}
	}
	
	@Test
	public void should_update_indices_field_with_keyword_when_valid_indices() throws Exception {
		try {
			Set<String> indices = new HashSet<String>();
			indices.add("indices 1");
			GetFieldMappingsResponse getFieldMappingsResponse = mock(GetFieldMappingsResponse.class);
			doReturn(this.indicesClient).when(this.restHighLevelClient).indices();
			when(this.indicesClient.getFieldMapping(ArgumentMatchers.any(GetFieldMappingsRequest.class), ArgumentMatchers.any())).thenReturn(getFieldMappingsResponse);
			XContentBuilder builder = XContentFactory.jsonBuilder();
			builder.startObject();
			builder.field("any field", new HashMap());
			builder.endObject();
			BytesReference bytesReference = BytesReference.bytes(builder);
			GetFieldMappingsResponse.FieldMappingMetadata mappingMetaData = new GetFieldMappingsResponse.FieldMappingMetadata(index, bytesReference);
			Map<String, GetFieldMappingsResponse.FieldMappingMetadata> mapBuilder = new HashMap<>();
			mapBuilder.put("data.any field", mappingMetaData);
			Map<String, Map<String, GetFieldMappingsResponse.FieldMappingMetadata>> mappingBuilder = new HashMap<>();
			mappingBuilder.put("any index 1", mapBuilder);
			mappingBuilder.put("any index 2", mapBuilder);
			Map<String, Map<String, Map<String, GetFieldMappingsResponse.FieldMappingMetadata>>> mapping = new HashMap<>();
			mapping.put("indices 1", mappingBuilder);
			when(getFieldMappingsResponse.mappings()).thenReturn(mapping);
			doReturn(mappingResponse).when(this.indicesClient).putMapping(ArgumentMatchers.any(PutMappingRequest.class), ArgumentMatchers.any(RequestOptions.class));
			BulkByScrollResponse response = mock(BulkByScrollResponse.class);
			doReturn(response).when(this.restHighLevelClient).updateByQuery(ArgumentMatchers.any(), ArgumentMatchers.any(RequestOptions.class));
			when(response.getBulkFailures()).thenReturn(new ArrayList<Failure>());
			when(elasticClientHandler.createRestClient()).thenReturn(restHighLevelClient);

			this.sut.updateIndexMappingForIndicesOfSameType( indices,"any field");
		} catch (Exception e) {
			fail("Should not throw this exception" + e.getMessage());
		}
	}
	
	@Test(expected = AppException.class)
	public void should_throw_exception_if_someIndex_is_invalid_andWeIndexfield_with_keyword() throws Exception {
		try {
			Set<String> indices = new HashSet<String>();
			indices.add("invalid 1");
			GetFieldMappingsResponse getFieldMappingsResponse = mock(GetFieldMappingsResponse.class);
			doReturn(this.indicesClient).when(this.restHighLevelClient).indices();
			when(this.indicesClient.getFieldMapping(ArgumentMatchers.any(GetFieldMappingsRequest.class), ArgumentMatchers.any())).thenReturn(getFieldMappingsResponse);
			XContentBuilder builder = XContentFactory.jsonBuilder();
			builder.startObject();
			builder.field("any field", new HashMap());
			builder.endObject();
			BytesReference bytesReference = BytesReference.bytes(builder);
			GetFieldMappingsResponse.FieldMappingMetadata mappingMetaData = new GetFieldMappingsResponse.FieldMappingMetadata(index, bytesReference);
			Map<String, GetFieldMappingsResponse.FieldMappingMetadata> mapBuilder = new HashMap<>();
			mapBuilder.put("data.any field", mappingMetaData);
			Map<String, Map<String, GetFieldMappingsResponse.FieldMappingMetadata>> mappingBuilder = new HashMap<>();
			mappingBuilder.put("any index 1", mapBuilder);
			mappingBuilder.put("any index 2", mapBuilder);
			Map<String, Map<String, Map<String, GetFieldMappingsResponse.FieldMappingMetadata>>> mapping = new HashMap<>();
			mapping.put("indices 1", mappingBuilder);
			when(getFieldMappingsResponse.mappings()).thenReturn(mapping);
			doReturn(mappingResponse).when(this.indicesClient).putMapping(ArgumentMatchers.any(PutMappingRequest.class), ArgumentMatchers.any(RequestOptions.class));
			BulkByScrollResponse response = mock(BulkByScrollResponse.class);
			doReturn(response).when(this.restHighLevelClient).updateByQuery(ArgumentMatchers.any(), ArgumentMatchers.any(RequestOptions.class));
			when(response.getBulkFailures()).thenReturn(new ArrayList<Failure>());
			when(elasticClientHandler.createRestClient()).thenReturn(restHighLevelClient);

			this.sut.updateIndexMappingForIndicesOfSameType(indices,"any field");
		} catch (Exception e) {
			throw e;
		}
	}

	@Test(expected = AppException.class)
	public void should_throw_exception_if_type_of_index_is_invalid_andWeIndexfield_with_keyword() throws Exception {
		try {
			Set<String> indices = new HashSet<String>();
			indices.add("indices 1");			
			GetFieldMappingsResponse getFieldMappingsResponse = mock(GetFieldMappingsResponse.class);
			doReturn(this.indicesClient).when(this.restHighLevelClient).indices();
			when(this.indicesClient.getFieldMapping(ArgumentMatchers.any(GetFieldMappingsRequest.class), ArgumentMatchers.any())).thenReturn(getFieldMappingsResponse);
			XContentBuilder builder = XContentFactory.jsonBuilder();
			builder.startObject();
			builder.field("any field", new HashMap());
			builder.endObject();
			BytesReference bytesReference = BytesReference.bytes(builder);
			GetFieldMappingsResponse.FieldMappingMetadata mappingMetaData = new GetFieldMappingsResponse.FieldMappingMetadata(index, bytesReference);
			Map<String, GetFieldMappingsResponse.FieldMappingMetadata> mapBuilder = new HashMap<>();
			mapBuilder.put("data.any field", mappingMetaData);
			Map<String, Map<String, GetFieldMappingsResponse.FieldMappingMetadata>> mappingBuilder = new HashMap<>();
			mappingBuilder.put("any index 1", mapBuilder);
			mappingBuilder.put("any index 2", mapBuilder);
			Map<String, Map<String, Map<String, GetFieldMappingsResponse.FieldMappingMetadata>>> mapping = new HashMap<>();
			mapping.put("indices 1", mappingBuilder);
			when(getFieldMappingsResponse.mappings()).thenReturn(mapping);
			doReturn(mappingResponse).when(this.indicesClient).putMapping(ArgumentMatchers.any(PutMappingRequest.class), ArgumentMatchers.any(RequestOptions.class));
			BulkByScrollResponse response = mock(BulkByScrollResponse.class);
			doReturn(response).when(this.restHighLevelClient).updateByQuery(ArgumentMatchers.any(), ArgumentMatchers.any(RequestOptions.class));
			when(response.getBulkFailures()).thenReturn(new ArrayList<Failure>());
			when(elasticClientHandler.createRestClient()).thenReturn(restHighLevelClient);
			this.sut.updateIndexMappingForIndicesOfSameType(indices,"any field invalid");
		} catch (Exception e) {
			throw e;
		}
	}

	@Test(expected = AppException.class)
	public void should_throw_exception_if_elastic_search_failedToFetch_andWeIndexfield_with_keyword() throws Exception {
		try {

			Set<String> indices = new HashSet<String>();
			indices.add("indices 1");
			indices.add("indices Invalid");
			GetFieldMappingsResponse getFieldMappingsResponse = mock(GetFieldMappingsResponse.class);
			doReturn(this.indicesClient).when(this.restHighLevelClient).indices();
			when(this.indicesClient.getFieldMapping(ArgumentMatchers.any(GetFieldMappingsRequest.class), ArgumentMatchers.any())).thenThrow(new ElasticsearchException(""));
			XContentBuilder builder = XContentFactory.jsonBuilder();
			builder.startObject();
			builder.field("any field", new HashMap());
			builder.endObject();
			BytesReference bytesReference = BytesReference.bytes(builder);
			GetFieldMappingsResponse.FieldMappingMetadata mappingMetaData = new GetFieldMappingsResponse.FieldMappingMetadata(index, bytesReference);
			Map<String, GetFieldMappingsResponse.FieldMappingMetadata> mapBuilder = new HashMap<>();
			mapBuilder.put("data.any field", mappingMetaData);
			Map<String, Map<String, GetFieldMappingsResponse.FieldMappingMetadata>> mappingBuilder = new HashMap<>();
			mappingBuilder.put("any index 1", mapBuilder);
			mappingBuilder.put("any index 2", mapBuilder);
			Map<String, Map<String, Map<String, GetFieldMappingsResponse.FieldMappingMetadata>>> mapping = new HashMap<>();
			mapping.put("indices 1", mappingBuilder);
			when(getFieldMappingsResponse.mappings()).thenReturn(mapping);
			doReturn(mappingResponse).when(this.indicesClient).putMapping(ArgumentMatchers.any(PutMappingRequest.class), ArgumentMatchers.any(RequestOptions.class));
			BulkByScrollResponse response = mock(BulkByScrollResponse.class);
			doReturn(response).when(this.restHighLevelClient).updateByQuery(ArgumentMatchers.any(), ArgumentMatchers.any(RequestOptions.class));
			when(response.getBulkFailures()).thenReturn(new ArrayList<Failure>());
			when(elasticClientHandler.createRestClient()).thenReturn(restHighLevelClient);
			this.sut.updateIndexMappingForIndicesOfSameType(indices,"any field");
		} catch (AppException e) {
			throw e;
		}
	}

	@Test(expected = AppException.class)
	public void should_throw_exception_when_elastic_failedToIndex_indices_field_with_keyword() {
		try {
			Set<String> indices = new HashSet<String>();
			indices.add("indices 1");
			indices.add("indices Invalid");
			GetFieldMappingsResponse getFieldMappingsResponse = mock(GetFieldMappingsResponse.class);
			doReturn(this.indicesClient).when(this.restHighLevelClient).indices();
			when(this.indicesClient.getFieldMapping(ArgumentMatchers.any(GetFieldMappingsRequest.class), ArgumentMatchers.any())).thenReturn(getFieldMappingsResponse);
			XContentBuilder builder = XContentFactory.jsonBuilder();
			builder.startObject();
			builder.field("any field", new HashMap());
			builder.endObject();
			BytesReference bytesReference = BytesReference.bytes(builder);
			GetFieldMappingsResponse.FieldMappingMetadata mappingMetaData = new GetFieldMappingsResponse.FieldMappingMetadata(index, bytesReference);
			Map<String, GetFieldMappingsResponse.FieldMappingMetadata> mapBuilder = new HashMap<>();
			mapBuilder.put("data.any field", mappingMetaData);
			Map<String, Map<String, GetFieldMappingsResponse.FieldMappingMetadata>> mappingBuilder = new HashMap<>();
			mappingBuilder.put("any index 1", mapBuilder);
			mappingBuilder.put("any index 2", mapBuilder);
			Map<String, Map<String, Map<String, GetFieldMappingsResponse.FieldMappingMetadata>>> mapping = new HashMap<>();
			mapping.put("indices 1", mappingBuilder);
			when(getFieldMappingsResponse.mappings()).thenReturn(mapping);
			doReturn(mappingResponse).when(this.indicesClient).putMapping(ArgumentMatchers.any(PutMappingRequest.class), ArgumentMatchers.any(RequestOptions.class));
			BulkByScrollResponse response = mock(BulkByScrollResponse.class);
			doReturn(response).when(this.restHighLevelClient).updateByQuery(ArgumentMatchers.any(), ArgumentMatchers.any(RequestOptions.class));
			when(response.getBulkFailures()).thenReturn(new ArrayList<Failure>());
			when(this.indicesClient.putMapping(ArgumentMatchers.any(PutMappingRequest.class), ArgumentMatchers.any(RequestOptions.class))).thenThrow(new ElasticsearchException(""));
			when(elasticClientHandler.createRestClient()).thenReturn(restHighLevelClient);
			this.sut.updateIndexMappingForIndicesOfSameType(indices,"any field");
		} catch (AppException e) {
			throw e;
		} catch (Exception e) {
			fail("Should not throw this exception" + e.getMessage());
		}
	}
}

