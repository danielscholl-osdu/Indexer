package org.opengroup.osdu.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;
import joptsimple.internal.Strings;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.response.InfoResponseMock;
import org.opengroup.osdu.util.Config;
import org.opengroup.osdu.util.ElasticUtils;
import org.opengroup.osdu.util.HTTPClient;

@Slf4j
public class InfoBase extends TestsBase {

  protected Map<String, String> headers = new HashMap<>();
  private InfoResponseMock response;

  public InfoBase(HTTPClient httpClient) {
    super(httpClient);
  }

  public InfoBase(HTTPClient httpClient, ElasticUtils elasticUtils) {
    super(httpClient, elasticUtils);
  }

  @Override
  protected String getApi() {
    return Config.getIndexerBaseURL() + "info";
  }

  @Override
  protected String getHttpMethod() {
    return "GET";
  }

  public void i_send_get_request_to_version_info_endpoint() {
    if (Config.getIndexerBaseURL().isEmpty()) {
      log.warn("Env variable INDEXER_HOST is empty. Version info endpoint test is skipped");
      return;
    }

    response =
        executeQuery(
            this.getApi(),
            Strings.EMPTY,
            headers,
            httpClient.getAccessToken(),
            InfoResponseMock.class);
  }

  public void i_should_get_version_info_in_response() {
    if (response != null) {
      assertEquals(200, response.getResponseCode());
      assertNotNull(response.getGroupId());
      assertNotNull(response.getArtifactId());
      assertNotNull(response.getVersion());
      assertNotNull(response.getBuildTime());
      assertNotNull(response.getBranch());
      assertNotNull(response.getCommitId());
      assertNotNull(response.getCommitMessage());
    } else {
      log.warn("Version info endpoint provided null response");
    }
  }
}
