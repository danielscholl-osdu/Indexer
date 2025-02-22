package org.opengroup.osdu.indexer.api;

import static org.junit.Assert.fail;
import static org.mockito.MockitoAnnotations.initMocks;
import com.google.gson.Gson;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opengroup.osdu.core.common.http.HeadersUtil;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.core.common.search.Config;
import org.opengroup.osdu.indexer.logging.AuditLogger;
import org.opengroup.osdu.indexer.service.IndexerService;
import org.opengroup.osdu.indexer.util.IndexerQueueTaskBuilder;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RunWith(MockitoJUnitRunner.class)
public class CleanupIndiciesApiTest {

  private final String messageValid = "{\"data\":\"[{\\\"id\\\":\\\"opendes:welldb:wellbore-d9033ae1-fb15-496c-9ba0-880fd1d2b2cf\\\",\\\"kind\\\":\\\"tenant1:welldb:wellbore:1.0.0\\\",\\\"op\\\":\\\"purge_schema\\\"}]\",\"attributes\":{\"account-id\":\"opendes\",\"correlation-id\":\"b5a281bd-f59d-4db2-9939-b2d85036fc7e\"},\"messageId\":\"75328163778221\",\"publishTime\":\"2018-05-08T21:48:56.131Z\"}";
  private final String messageEmpty = "{}";
  private final String messageWithEmptyData = "{\"data\":\"[]\",\"attributes\":{\"account-id\":\"opendes\",\"correlation-id\":\"b5a281bd-f59d-4db2-9939-b2d85036fc7e\"},\"messageId\":\"75328163778221\",\"publishTime\":\"2018-05-08T21:48:56.131Z\"}";
  private final String messageWithIncorrectJsonFormat = "{\"data\":\"[{}}]\",\"attributes\":{\"account-id\":\"opendes\",\"correlation-id\":\"b5a281bd-f59d-4db2-9939-b2d85036fc7e\"},\"messageId\":\"75328163778221\",\"publishTime\":\"2018-05-08T21:48:56.131Z\"}";

  @InjectMocks
  private CleanupIndiciesApi sut;

  @Mock
  private IndexerService indexerService;

  @Mock
  private AuditLogger auditLogger;

  @Before
  public void setup() {
    initMocks(this);
  }

  @Test
  public void should_return200_given_validMessage_indexCleanupTest() {
    should_return200_indexerWorkerTest(messageValid);
  }

  @Test
  public void should_return200_given_emptyData_indexCleanupTest() {
    should_return200_indexerWorkerTest(messageWithEmptyData);
  }

  @Test
  public void should_return400_given_emptyMessage_indexCleanupTest() {
    should_return400_indexerWorkerTest(messageEmpty, String.format("Required header: '%s' not found", DpsHeaders.DATA_PARTITION_ID));
  }

  @Test
  public void should_return400_given_incorrectJsonFormatMessage_indexWorkerTest() {
    should_return400_indexerWorkerTest(messageWithIncorrectJsonFormat, "Unable to parse request payload.");
  }

  private void should_return200_indexerWorkerTest(String message) {
    ResponseEntity response = this.sut.cleanupIndices(createRecordChangedMessage(message));
    Assert.assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
  }

  private void should_return400_indexerWorkerTest(String message, String errorMessage) {
    try {
      this.sut.cleanupIndices(createRecordChangedMessage(message));
      fail("Should throw exception");
    } catch (AppException e) {
      Assert.assertEquals(HttpStatus.BAD_REQUEST.value(), e.getError().getCode());
      Assert.assertEquals(errorMessage, e.getError().getMessage());
    } catch (Exception e) {
      fail("Should not throw this exception" + e.getMessage());
    }
  }

  private RecordChangedMessages createRecordChangedMessage(String message) {
    return (new Gson()).fromJson(message, RecordChangedMessages.class);
  }
}
