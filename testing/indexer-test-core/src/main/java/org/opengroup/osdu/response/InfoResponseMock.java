package org.opengroup.osdu.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class InfoResponseMock extends ResponseBase {
  private String groupId;
  private String artifactId;
  private String version;
  private String buildTime;
  private String branch;
  private String commitId;
  private String commitMessage;
}