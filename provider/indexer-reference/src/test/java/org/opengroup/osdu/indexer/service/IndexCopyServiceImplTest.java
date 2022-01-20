/*
 * Copyright 2021 Google LLC
 * Copyright 2021 EPAM Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.indexer.service;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.indexer.IElasticSettingService;
import org.opengroup.osdu.core.common.model.search.ClusterSettings;
import org.opengroup.osdu.core.common.provider.interfaces.IRequestInfo;
import org.opengroup.osdu.core.common.search.ElasticIndexNameResolver;
import org.opengroup.osdu.indexer.logging.AuditLogger;
import org.opengroup.osdu.indexer.util.ElasticClientHandler;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@PrepareForTest({RestHighLevelClient.class, Response.class, RestClient.class, HttpEntity.class,
    EntityUtils.class})
public class IndexCopyServiceImplTest {

  private final String correlationId = UUID.randomUUID().toString();

  @Mock
  private HttpEntity httpEntity;
  @Mock
  private HttpEntity httpEntityRequest;
  @Mock
  private IRequestInfo requestInfo;
  @Mock
  private DpsHeaders headersInfo;
  @Mock
  private RestClient restClient;
  @Mock
  private RestHighLevelClient restHighLevelClient;
  @Mock
  private IndicesService indicesService;
  @Mock
  private IMappingService mappingService;
  @Mock
  private ElasticClientHandler elasticClientHandler;
  @Mock
  private ElasticIndexNameResolver elasticIndexNameResolver;
  @Mock
  private Response response;
  @Mock
  private IElasticSettingService elasticSettingService;
  @Mock
  private AuditLogger auditLogger;
  @Mock
  private Map<String, String> httpHeaders;
  @InjectMocks
  private IndexCopyServiceImpl sut;

  private ClusterSettings commonCluster;

  private Map<String, Object> correctMap;

  @Before
  public void setup() {

    commonCluster = ClusterSettings.builder().host("commonhost").port(8080)
        .userNameAndPassword("username:pwd").build();

    httpHeaders = new HashMap<>();
    httpHeaders.put(DpsHeaders.AUTHORIZATION, "testAuth");
    httpHeaders.put(DpsHeaders.CORRELATION_ID, correlationId);
    when(requestInfo.getHeadersMapWithDwdAuthZ()).thenReturn(httpHeaders);
    when(response.getEntity()).thenReturn(httpEntity);

    Type mapType = new TypeToken<Map<String, Object>>() {
    }.getType();
    String afterFormat = "{\"properties\":{\"id\":{\"type\":\"keyword\"}}}";
    correctMap = new Gson().fromJson(afterFormat, mapType);

    restHighLevelClient = mock(RestHighLevelClient.class);

  }

  @Test(expected = IOException.class)
  public void should_throwIOException_when_indexMappingNotFound() throws Exception {
    IOException exception = new IOException(
        "Fail to get mapping for the given index from common cluster.");

    when(this.mappingService.getIndexMapping(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenThrow(exception);

    this.sut.copyIndex("common:metadata:entity:1.0.0");
  }

  @Test(expected = IllegalArgumentException.class)
  public void should_throwIllegalArgExceptionCopyIndexRequest_copyIndexTest() {
    try {
      this.sut.copyIndex(null);
    } catch (IOException e) {
      fail("Should not throw IOException but illegalArgumentException.");
    }
  }

  @Test
  public void should_returnIndexMapping_getIndexMappingFromCommonClustertest() {
    String mappingJson = "{\"common-metadata-entity-1.0.0\":{\"mappings\":{\"entity\":{\"properties\":{\"id\":{\"type\":\"keyword\"}}}}}}";
    when(elasticClientHandler.createRestClient()).thenReturn(restHighLevelClient);
    try {
      when(this.mappingService.getIndexMapping(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(mappingJson);
      Map<String, Object> resultMap = this.sut.getIndexMappingsFromCommonCluster("test", "test");
      Assert.assertEquals(resultMap, correctMap);
    } catch (Exception ignored) {
    }
  }

  @Test
  public void should_returnClusterInfo_getCommonClusterInformationtest() {
    try {
      String[] correctCommonCluster = {"https://commonhost:8080", "username", "pwd"};

      when(elasticClientHandler.createRestClient()).thenReturn(restHighLevelClient);

      when(elasticSettingService.getElasticClusterInformation()).thenReturn(commonCluster);

      String[] resultCommonCluster = this.sut.getCommonClusterInformation();
      Assert.assertEquals(correctCommonCluster[0], resultCommonCluster[0]);
      Assert.assertEquals(correctCommonCluster[1], resultCommonCluster[1]);
      Assert.assertEquals(correctCommonCluster[2], resultCommonCluster[2]);
    } catch (IOException ignored) {
      fail("Should not throw this exception " + ignored.getMessage());
    }
  }

  @Test(expected = AppException.class)
  public void should_throwException_failToCreateIndexInTenantCluster_createIndexInTenantClustertest() {
    try {
      when(elasticClientHandler.createRestClient()).thenReturn(restHighLevelClient);
      when(indicesService
          .createIndex(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
              ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(false);
      this.sut.createIndexInTenantCluster("test", "test", "test", correctMap);
    } catch (IOException ignored) {
      fail("Should not throw this exception " + ignored.getMessage());
    }
  }

  @Ignore
  public void should_returnTaskIdResponse_reindexRequestSucceed_reindexInTenantClustertest() {
    //TODO: fix the null Response from restHighLevelClient.getLowLevelClient().performRequest().
    try {
      String[] correctCommonCluster = {"https://commonhost:8080", "username", "pwd"};
      Request request = new Request("POST", "/_reindex?wait_for_completion=false");
      request.setEntity(httpEntityRequest);
      when(elasticClientHandler.createRestClient()).thenReturn(restHighLevelClient);
      when(indicesService
          .createIndex(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
              ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(false);
      when(restHighLevelClient.getLowLevelClient()).thenReturn(restClient);
      when(restClient.performRequest(request)).thenReturn(response);
      when(response.getEntity()).thenReturn(httpEntity);
      Assert.assertEquals(httpEntity,
          this.sut.reindexInTenantCluster("test", "test", correctCommonCluster));
    } catch (IOException ignored) {
    }
  }
}
