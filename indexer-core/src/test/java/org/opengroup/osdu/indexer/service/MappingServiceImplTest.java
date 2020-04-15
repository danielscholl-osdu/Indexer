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
/*
package org.opengroup.osdu.is.core.service;

import org.apache.http.HttpStatus;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.client.IndicesClient;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.indexer.IElasticSettingService;
import org.opengroup.osdu.core.common.service.coreis.*;
import org.opengroup.osdu.is.core.util.ElasticClientHandler;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(SpringRunner.class)
@PrepareForTest({RestHighLevelClient.class, IndicesClient.class, GetMappingsResponse.class})
public class MappingServiceImplTest {
    @Mock
    private IElasticSettingService elasticSettingService;
    @Mock
    private ElasticClientHandler elasticClientHandler;
    @Mock
    private ElasticIndexNameResolver elasticIndexNameResolver;
    @Mock
    private IndicesService indicesService;
    @InjectMocks
    private MappingServiceImpl sut;

    private RestHighLevelClient restHighLevelClient;
    private IndicesClient indicesClient;
    private GetMappingsResponse mappingsResponse;
    private String index = "anyindex";

    @Before
    public void setup() {
        initMocks(this);
        this.indicesClient = PowerMockito.mock(IndicesClient.class);
        this.restHighLevelClient = PowerMockito.mock(RestHighLevelClient.class);
        this.mappingsResponse = PowerMockito.mock(GetMappingsResponse.class);
    }

    @Test
    public void error_when_get_mapping_from_non_exists_elastic_index() throws Exception {
        try {
            when(this.indicesService.isIndexExist(restHighLevelClient, index)).thenReturn(false);

            this.sut.getIndexMapping(restHighLevelClient, index);
            fail("expected exception");
        } catch (AppException ex) {
            assertEquals(HttpStatus.SC_NOT_FOUND, ex.getError().getCode());
        }
    }

    @Test(expected = AppException.class)
    public void error_when_get_mapping_failed_from_existing_elastic_index() throws Exception {
        when(this.indicesService.isIndexExist(this.restHighLevelClient, this.index)).thenReturn(true);
        doReturn(this.indicesClient).when(this.restHighLevelClient).indices();
        doThrow(new IOException()).when(this.indicesClient).getMapping(any(), any(RequestOptions.class));

        this.sut.getIndexMapping(this.restHighLevelClient, this.index);

        fail("expected exception");
    }

    @Test
    public void get_mapping_from_existing_elastic_index() throws Exception {
        when(this.indicesService.isIndexExist(restHighLevelClient, index)).thenReturn(true);
        doReturn(this.indicesClient).when(this.restHighLevelClient).indices();
        doReturn(this.mappingsResponse).when(this.indicesClient).getMapping(any(), any(RequestOptions.class));
        doReturn("dummy").when(this.mappingsResponse).toString();

        String mapping = this.sut.getIndexMapping(restHighLevelClient, index);

        // TODO
        assertEquals("dummy", mapping);
    }
}*/