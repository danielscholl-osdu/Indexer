/*
  Copyright 2020 Google LLC
  Copyright 2020 EPAM Systems, Inc

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

package org.opengroup.osdu.indexer.provider.gcp.web.util;

import static org.opengroup.osdu.core.common.model.http.DpsHeaders.AUTHORIZATION;

import com.google.common.base.Strings;
import java.util.Map;
import java.util.logging.Level;
import javax.inject.Inject;
import lombok.extern.java.Log;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.auth.TokenProvider;
import org.opengroup.osdu.core.common.Constants;
import org.opengroup.osdu.core.common.model.entitlements.AuthorizationResponse;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.DeploymentEnvironment;
import org.opengroup.osdu.core.common.model.search.SearchServiceRole;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.IAuthorizationService;
import org.opengroup.osdu.core.common.provider.interfaces.IRequestInfo;
import org.opengroup.osdu.core.gcp.model.CloudTaskHeaders;
import org.opengroup.osdu.indexer.config.IndexerConfigurationProperties;
import org.springframework.stereotype.Component;


@Log
@Component
public class RequestInfoImpl implements IRequestInfo {

    @Inject
    private DpsHeaders dpsHeaders;

    @Inject
    private TokenProvider tokenProvider;

    @Inject
    private TenantInfo tenantInfo;

    @Inject
    private IndexerConfigurationProperties properties;

    @Inject
    private IAuthorizationService authorizationService;

    private static final String EXPECTED_CRON_HEADER_VALUE = "true";

    @Override
    public DpsHeaders getHeaders() {

        return this.dpsHeaders;
    }

    @Override
    public String getPartitionId() {
        return this.dpsHeaders.getPartitionId();
    }

    @Override
    public Map<String, String> getHeadersMap() {
        return this.dpsHeaders.getHeaders();
    }

    @Override
    public Map<String, String> getHeadersMapWithDwdAuthZ() {
        return getHeadersWithDwdAuthZ().getHeaders();
    }

    @Override
    public DpsHeaders getHeadersWithDwdAuthZ() {
        // Update DpsHeaders so that service account creds are passed down
        this.dpsHeaders.put(AUTHORIZATION, this.checkOrGetAuthorizationHeader());
        return this.dpsHeaders;
    }

    @Override
    public boolean isCronRequest() {
        String appEngineCronHeader = this.dpsHeaders.getHeaders().getOrDefault(CloudTaskHeaders.CLOUD_CRON_SERVICE, null);
        return EXPECTED_CRON_HEADER_VALUE.equalsIgnoreCase(appEngineCronHeader);
    }

    @Override
    public boolean isTaskQueueRequest() {
        if (this.dpsHeaders.getHeaders().containsKey(CloudTaskHeaders.CLOUD_TASK_QUEUE_NAME)) {
            log.log(Level.INFO, "Request confirmed as cloud task, token validation in progress");
            return isCloudTaskRequest();
        }
        if (this.dpsHeaders.getHeaders().containsKey(CloudTaskHeaders.APPENGINE_TASK_QUEUE_NAME)) {
            log.log(Level.INFO, "Request confirmed as AppEngine, headers validation in progress");
            return isAppEngineTaskRequest();
        }
        return false;
    }

    private boolean isCloudTaskRequest() {
        AuthorizationResponse authResponse = authorizationService.authorizeAny(dpsHeaders, SearchServiceRole.ADMIN);
        dpsHeaders.put(DpsHeaders.USER_EMAIL, authResponse.getUser());
        return true;
    }

    private boolean isAppEngineTaskRequest() {
        if (!this.dpsHeaders.getHeaders().containsKey(CloudTaskHeaders.APPENGINE_TASK_QUEUE_NAME)) {
            return false;
        }
        String queueId = this.dpsHeaders.getHeaders().get(CloudTaskHeaders.APPENGINE_TASK_QUEUE_NAME);
        return queueId.endsWith(Constants.INDEXER_QUEUE_IDENTIFIER);
    }

    public String checkOrGetAuthorizationHeader() {
        if (properties.getDeploymentEnvironment() == DeploymentEnvironment.LOCAL) {
            String authHeader = this.dpsHeaders.getAuthorization();
            if (Strings.isNullOrEmpty(authHeader)) {
                throw new AppException(HttpStatus.SC_UNAUTHORIZED, "Invalid authorization header", "Authorization token cannot be empty");
            }
            String user = this.dpsHeaders.getUserEmail();
            if (Strings.isNullOrEmpty(user)) {
                throw new AppException(HttpStatus.SC_UNAUTHORIZED, "Invalid user header", "User header cannot be empty");
            }
            return authHeader;
        } else {
            return "Bearer " + this.tokenProvider.getIdToken();
        }
    }
}
