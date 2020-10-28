package org.opengroup.osdu.indexer.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opengroup.osdu.core.common.model.indexer.RecordInfo;
import org.opengroup.osdu.core.common.search.ElasticIndexNameResolver;
import org.opengroup.osdu.core.common.search.IndicesService;
import org.opengroup.osdu.indexer.util.ElasticClientHandler;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class IndexerServiceImplTest {

  @InjectMocks
  private IndexerServiceImpl indexerService;

  @Mock
  private ElasticClientHandler elasticClientHandler;

  @Mock
  private ElasticIndexNameResolver elasticIndexNameResolver;

  @Mock
  private IndicesService indicesService;

  private List<RecordInfo> recordInfos = new ArrayList<>();

  @Before
  public void setup() {
    RecordInfo recordInfo = new RecordInfo();
    recordInfo.setId("opendes:ds:mytest3-d9033ae1-fb15-496c-9ba0-880fd1d2b2qf");
    recordInfo.setKind("opendes:ds:mytest2:1.0.0");
    recordInfo.setOp("purge_schema");
    recordInfos.add(recordInfo);

    initMocks(this);
  }

  @Test
  public void processSchemaMessagesTest() throws Exception {
    indexerService.processSchemaMessages(recordInfos);

    verify(elasticClientHandler, times(1)).createRestClient();
    verify(elasticIndexNameResolver, times(1)).getIndexNameFromKind(any());
    verify(indicesService, times(1)).isIndexExist(any(), any());
  }
}
