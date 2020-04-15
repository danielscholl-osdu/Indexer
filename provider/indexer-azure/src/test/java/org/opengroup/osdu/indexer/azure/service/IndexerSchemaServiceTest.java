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

package org.opengroup.osdu.indexer.azure.service;

import org.apache.http.HttpStatus;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opengroup.osdu.core.common.model.indexer.IndexSchema;
import org.opengroup.osdu.core.common.model.indexer.OperationType;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.indexer.provider.interfaces.ISchemaCache;
import org.opengroup.osdu.indexer.service.IndexSchemaServiceImpl;
import org.opengroup.osdu.indexer.service.IndexerMappingService;
import org.opengroup.osdu.indexer.service.StorageService;
import org.opengroup.osdu.core.common.model.http.RequestStatus;
import org.opengroup.osdu.core.common.search.IndicesService;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.indexer.util.ElasticClientHandler;
import org.opengroup.osdu.core.common.search.ElasticIndexNameResolver;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@Ignore
@RunWith(SpringRunner.class)
@PrepareForTest({RestHighLevelClient.class})
public class IndexerSchemaServiceTest {

    private final String kind = "tenant:test:test:1.0.0";
    private final String emptySchema = null;
    private final String someSchema = "{\"kind\":\"tenant:test:test:1.0.0\", \"schema\":[{\"path\":\"test-path\", \"kind\":\"tenant:test:test:1.0.0\"}]}";

    @Mock
    private JaxRsDpsLog log;
    @Mock
    private StorageService storageService;
    @Mock
    private ElasticClientHandler elasticClientHandler;
    @Mock
    private ElasticIndexNameResolver elasticIndexNameResolver;
    @Mock
    private IndexerMappingService mappingService;
    @Mock
    private IndicesService indicesService;
    @Mock
    private ISchemaCache schemaCache;
    @InjectMocks
    private IndexSchemaServiceImpl sut;

    @Before
    public void setup() {
        initMocks(this);
        RestHighLevelClient restHighLevelClient = mock(RestHighLevelClient.class);
        when(elasticClientHandler.createRestClient()).thenReturn(restHighLevelClient);
    }

    @Test
    public void should_returnNull_givenEmptySchema_getIndexerInputSchemaSchemaTest() throws Exception {
        when(storageService.getStorageSchema(any())).thenReturn(emptySchema);

        IndexSchema indexSchema = this.sut.getIndexerInputSchema(kind);

        Assert.assertNotNull(indexSchema);
    }

    @Test
    public void should_returnValidResponse_givenValidSchema_getIndexerInputSchemaTest() throws Exception {
        when(storageService.getStorageSchema(any())).thenReturn(someSchema);

        IndexSchema indexSchema = this.sut.getIndexerInputSchema(kind);

        Assert.assertEquals(kind, indexSchema.getKind());
    }

    @Test
    public void should_returnValidResponse_givenValidSchemaWithCacheHit_getIndexerInputSchemaTest() throws Exception {
        when(storageService.getStorageSchema(any())).thenReturn(someSchema);
        when(this.schemaCache.get(kind + "_flattened")).thenReturn(someSchema);

        IndexSchema indexSchema = this.sut.getIndexerInputSchema(kind);

        Assert.assertEquals(kind, indexSchema.getKind());
    }

    @Test
    public void should_throw500_givenInvalidSchemaCacheHit_getIndexerInputSchemaTest() {
        try {
            String invalidSchema = "{}}";
            when(storageService.getStorageSchema(any())).thenReturn(invalidSchema);

            this.sut.getIndexerInputSchema(kind);
            fail("Should throw exception");
        } catch (AppException e) {
            Assert.assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getError().getCode());
            Assert.assertEquals("An error has occurred while normalizing the schema.", e.getError().getMessage());
        } catch (Exception e) {
            fail("Should not throw exception" + e.getMessage());
        }
    }

    @Test
    public void should_return_basic_schema_when_storage_returns_no_schema() {
        IndexSchema returnedSchema = this.sut.getIndexerInputSchema(kind);

        assertNotNull(returnedSchema.getDataSchema());
        assertNotNull(returnedSchema);
        assertEquals(kind, returnedSchema.getKind());
    }

    @Test
    public void should_create_schema_when_storage_returns_valid_schema() throws IOException, URISyntaxException {
        String kind = "tenant1:avocet:completion:1.0.0";
        String storageSchema = "{" +
                "  \"kind\": \"tenant1:avocet:completion:1.0.0\"," +
                "  \"schema\": [" +
                "    {" +
                "      \"path\": \"status\"," +
                "      \"kind\": \"string\"" +
                "    }," +
                "    {" +
                "      \"path\": \"startDate\"," +
                "      \"kind\": \"string\"" +
                "    }," +
                "    {" +
                "      \"path\": \"endDate\"," +
                "      \"kind\": \"string\"" +
                "    }," +
                "    {" +
                "      \"path\": \"type \"," +
                "      \"kind\": \"string\"" +
                "    }," +
                "    {" +
                "      \"path\": \"itemguid\"," +
                "      \"kind\": \"string\"" +
                "    }" +
                "  ]" +
                "}";
        Map<String, OperationType> schemaMessages = new HashMap<>();
        schemaMessages.put(kind, OperationType.create_schema);

        when(this.elasticIndexNameResolver.getIndexNameFromKind(kind)).thenReturn(kind.replace(":", "-"));
        when(this.schemaCache.get(kind)).thenReturn(null);
        when(this.indicesService.isIndexExist(any(), any())).thenReturn(false);
        when(this.storageService.getStorageSchema(kind)).thenReturn(storageSchema);

        this.sut.processSchemaMessages(schemaMessages);

        verify(this.mappingService, times(1)).getIndexMappingFromRecordSchema(any());
        verify(this.indicesService, times(1)).createIndex(any(), any(), any(), any(), any());
        verifyNoMoreInteractions(this.mappingService);
    }

    @Test
    public void should_merge_mapping_when_storage_returns_valid_schema() throws IOException, URISyntaxException {
        String kind = "tenant1:avocet:completion:1.0.0";
        String storageSchema = "{" +
                "  \"kind\": \"tenant1:avocet:completion:1.0.0\"," +
                "  \"schema\": [" +
                "    {" +
                "      \"path\": \"status\"," +
                "      \"kind\": \"string\"" +
                "    }," +
                "    {" +
                "      \"path\": \"startDate\"," +
                "      \"kind\": \"string\"" +
                "    }" +
                "  ]" +
                "}";
        Map<String, OperationType> schemaMessages = new HashMap<>();
        schemaMessages.put(kind, OperationType.create_schema);

        when(this.elasticIndexNameResolver.getIndexNameFromKind(kind)).thenReturn(kind.replace(":", "-"));
        when(this.schemaCache.get(kind)).thenReturn(null);
        when(this.indicesService.isIndexExist(any(), any())).thenReturn(true);
        when(this.storageService.getStorageSchema(kind)).thenReturn(storageSchema);

        this.sut.processSchemaMessages(schemaMessages);

        verify(this.indicesService, times(0)).createIndex(any(), any(), any(), any(), any());
        verify(this.mappingService, times(1)).createMapping(any(), any(), any(), anyBoolean());
        verifyNoMoreInteractions(this.mappingService);
    }

    @Test
    public void should_throw_mapping_conflict_when_elastic_backend_cannot_process_schema_changes() throws IOException, URISyntaxException {
        String kind = "tenant1:avocet:completion:1.0.0";
        String reason = String.format("Could not create type mapping %s/completion.", kind.replace(":", "-"));
        String storageSchema = "{" +
                "  \"kind\": \"tenant1:avocet:completion:1.0.0\"," +
                "  \"schema\": [" +
                "    {" +
                "      \"path\": \"status\"," +
                "      \"kind\": \"string\"" +
                "    }" +
                "  ]" +
                "}";
        Map<String, OperationType> schemaMessages = new HashMap<>();
        schemaMessages.put(kind, OperationType.create_schema);

        when(this.elasticIndexNameResolver.getIndexNameFromKind(kind)).thenReturn(kind.replace(":", "-"));
        when(this.schemaCache.get(kind)).thenReturn(null);
        when(this.indicesService.isIndexExist(any(), any())).thenReturn(true);
        when(this.storageService.getStorageSchema(kind)).thenReturn(storageSchema);
        when(this.mappingService.createMapping(any(), any(), any(), anyBoolean())).thenThrow(new AppException(HttpStatus.SC_BAD_REQUEST, reason, ""));

        try {
            this.sut.processSchemaMessages(schemaMessages);
        } catch (AppException e){
            assertEquals(e.getError().getCode(), RequestStatus.SCHEMA_CONFLICT);
            assertEquals(e.getError().getMessage(), "error creating or merging index mapping");
            assertEquals(e.getError().getReason(), reason);
        } catch (Exception e) {
            fail("Should not throw this exception " + e.getMessage());
        }
    }

    @Test
    public void should_throw_genericAppException_when_elastic_backend_cannot_process_schema_changes() throws IOException, URISyntaxException {
        String kind = "tenant1:avocet:completion:1.0.0";
        String reason = String.format("Could not create type mapping %s/completion.", kind.replace(":", "-"));
        String storageSchema = "{" +
                "  \"kind\": \"tenant1:avocet:completion:1.0.0\"," +
                "  \"schema\": [" +
                "    {" +
                "      \"path\": \"status\"," +
                "      \"kind\": \"string\"" +
                "    }" +
                "  ]" +
                "}";
        Map<String, OperationType> schemaMessages = new HashMap<>();
        schemaMessages.put(kind, OperationType.create_schema);

        when(this.elasticIndexNameResolver.getIndexNameFromKind(kind)).thenReturn(kind.replace(":", "-"));
        when(this.schemaCache.get(kind)).thenReturn(null);
        when(this.indicesService.isIndexExist(any(), any())).thenReturn(true);
        when(this.storageService.getStorageSchema(kind)).thenReturn(storageSchema);
        when(this.mappingService.createMapping(any(), any(), any(), anyBoolean())).thenThrow(new AppException(HttpStatus.SC_FORBIDDEN, reason, "blah"));

        try {
            this.sut.processSchemaMessages(schemaMessages);
        } catch (AppException e){
            assertEquals(e.getError().getCode(), HttpStatus.SC_FORBIDDEN);
            assertEquals(e.getError().getMessage(), "blah");
            assertEquals(e.getError().getReason(), reason);
        } catch (Exception e) {
            fail("Should not throw this exception " + e.getMessage());
        }
    }


    @Test
    public void should_log_and_do_nothing_when_storage_returns_invalid_schema() throws IOException, URISyntaxException {
        String kind = "tenant1:avocet:completion:1.0.0";
        String storageSchema = "{" +
                "  \"kind\": \"tenant1:avocet:completion:1.0.0\"" +
                "}";
        Map<String, OperationType> schemaMessages = new HashMap<>();
        schemaMessages.put(kind, OperationType.create_schema);

        when(this.elasticIndexNameResolver.getIndexNameFromKind(kind)).thenReturn(kind.replace(":", "-"));
        when(this.schemaCache.get(kind)).thenReturn(null);
        when(this.indicesService.isIndexExist(any(), any())).thenReturn(true);
        when(this.storageService.getStorageSchema(kind)).thenReturn(storageSchema);

        this.sut.processSchemaMessages(schemaMessages);

        verify(this.log).warning(eq("schema not found for kind: tenant1:avocet:completion:1.0.0"));
    }

    @Test
    public void should_invalidateCache_when_purge_schema_and_schema_found_in_cache() throws IOException {
        String kind = "tenant1:avocet:completion:1.0.0";
        Map<String, OperationType> schemaMessages = new HashMap<>();
        schemaMessages.put(kind, OperationType.purge_schema);

        when(this.elasticIndexNameResolver.getIndexNameFromKind(kind)).thenReturn(kind.replace(":", "-"));
        when(this.indicesService.isIndexExist(any(), any())).thenReturn(true);
        when(this.schemaCache.get(kind)).thenReturn("schema");
        when(this.schemaCache.get(kind + "_flattened")).thenReturn("flattened schema");

        this.sut.processSchemaMessages(schemaMessages);

        verify(this.schemaCache, times(2)).get(anyString());
        verify(this.schemaCache, times(2)).delete(anyString());
    }

    @Test
    public void should_log_warning_when_purge_schema_and_schema_not_found_in_cache() throws IOException {
        String kind = "tenant1:avocet:completion:1.0.0";
        Map<String, OperationType> schemaMessages = new HashMap<>();
        schemaMessages.put(kind, OperationType.purge_schema);

        when(this.elasticIndexNameResolver.getIndexNameFromKind(kind)).thenReturn(kind.replace(":", "-"));
        when(this.indicesService.isIndexExist(any(), any())).thenReturn(false);

        this.sut.processSchemaMessages(schemaMessages);

        verify(this.log).warning(eq(String.format("Kind: %s not found", kind)));
    }
}
