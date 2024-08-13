package org.opengroup.osdu.common.info;

import org.opengroup.osdu.common.InfoBase;
import org.opengroup.osdu.util.ElasticUtils;
import org.opengroup.osdu.util.HTTPClient;

public class InfoSteps extends InfoBase {

  public InfoSteps(HTTPClient httpClient) {
    super(httpClient);
  }

  public InfoSteps(HTTPClient httpClient, ElasticUtils elasticUtils) {
    super(httpClient, elasticUtils);
  }
}
