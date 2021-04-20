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

package org.opengroup.osdu.indexer.middleware;

import com.google.common.base.Strings;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.java.Log;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.http.ResponseHeaders;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.DeploymentEnvironment;
import org.opengroup.osdu.core.common.provider.interfaces.IRequestInfo;
import org.opengroup.osdu.indexer.config.IndexerConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

@Log
@Component
public class IndexFilter implements Filter {


  private final IndexerConfigurationProperties indexerConfigurationProperties;
  private final DpsHeaders dpsHeaders;
  private final IRequestInfo requestInfo;

  private FilterConfig filterConfig;

  private static final String PATH_SWAGGER = "/swagger.json";
  private static final String PATH_TASK_HANDLERS = "task-handlers";
  private static final String PATH_CRON_HANDLERS = "cron-handlers";

  @Autowired
  public IndexFilter(IndexerConfigurationProperties indexerConfigurationProperties,
      DpsHeaders dpsHeaders, IRequestInfo requestInfo) {
    this.indexerConfigurationProperties = indexerConfigurationProperties;
    this.dpsHeaders = dpsHeaders;
    this.requestInfo = requestInfo;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
      FilterChain filterChain)
      throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
    String uri = httpRequest.getRequestURI().toLowerCase();

    if (httpRequest.getMethod().equalsIgnoreCase(HttpMethod.POST.name()) && uri
        .contains(PATH_TASK_HANDLERS)) {
      if (DeploymentEnvironment.valueOf(indexerConfigurationProperties.getEnvironment())
          != DeploymentEnvironment.LOCAL) {
        checkWorkerApiAccess(requestInfo);
      }
    }

    if (httpRequest.getMethod().equalsIgnoreCase(HttpMethod.GET.name()) && uri
        .contains(PATH_CRON_HANDLERS)) {
      checkWorkerApiAccess(requestInfo);
    }

    filterChain.doFilter(servletRequest, servletResponse);
    
    HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
    Map<String, List<Object>> standardHeaders = ResponseHeaders.STANDARD_RESPONSE_HEADERS;
    for (Map.Entry<String, List<Object>> header : standardHeaders.entrySet()) {
      httpResponse.addHeader(header.getKey(), header.getValue().toString());
    }
    if (httpResponse.getHeader(DpsHeaders.CORRELATION_ID) == null) {
      httpResponse.addHeader(DpsHeaders.CORRELATION_ID, dpsHeaders.getCorrelationId());
    }

  }

  @Override
  public void destroy() {
  }

  private void checkWorkerApiAccess(IRequestInfo requestInfo) {
  }

  private List<String> validateAccountId(DpsHeaders requestHeaders) {
    String accountHeader = requestHeaders.getPartitionIdWithFallbackToAccountId();
    String debuggingInfo = String.format("%s:%s", DpsHeaders.DATA_PARTITION_ID, accountHeader);

    if (Strings.isNullOrEmpty(accountHeader)) {
      throw new AppException(HttpStatus.SC_BAD_REQUEST, "Bad request",
          "invalid or empty data partition", debuggingInfo);
    }

    List<String> dataPartitions = Arrays.asList(accountHeader.trim().split("\\s*,\\s*"));
    if (dataPartitions.isEmpty() || dataPartitions.size() > 1) {
      throw new AppException(HttpStatus.SC_BAD_REQUEST, "Bad request",
          "invalid or empty data partition", debuggingInfo);
    }
    return dataPartitions;
  }
}
