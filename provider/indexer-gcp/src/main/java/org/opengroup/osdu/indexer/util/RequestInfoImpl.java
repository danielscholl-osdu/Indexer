package org.opengroup.osdu.indexer.util;

import com.google.common.base.Strings;
import lombok.extern.java.Log;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.Constants;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.search.DeploymentEnvironment;
import org.opengroup.osdu.core.common.util.IServiceAccountJwtClient;
import org.opengroup.osdu.core.gcp.model.AppEngineHeaders;
import org.opengroup.osdu.core.common.provider.interfaces.IRequestInfo;
import org.opengroup.osdu.indexer.config.IndexerConfigurationProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import javax.inject.Inject;
import java.util.Map;

import static org.opengroup.osdu.core.common.model.http.DpsHeaders.AUTHORIZATION;


@Log
@Component
@RequestScope
public class RequestInfoImpl implements IRequestInfo {

    @Inject
    private DpsHeaders dpsHeaders;

    @Inject
    private IServiceAccountJwtClient serviceAccountJwtClient;

    @Inject
    private TenantInfo tenantInfo;

    @Inject
    private IndexerConfigurationProperties properties;

    private static final String expectedCronHeaderValue = "true";

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
        String appEngineCronHeader = this.dpsHeaders.getHeaders().getOrDefault(AppEngineHeaders.CRON_SERVICE, null);
        return expectedCronHeaderValue.equalsIgnoreCase(appEngineCronHeader);
    }

    @Override
    public boolean isTaskQueueRequest() {
        if (!this.dpsHeaders.getHeaders().containsKey(AppEngineHeaders.TASK_QUEUE_NAME)) return false;

        String queueId = this.dpsHeaders.getHeaders().get(AppEngineHeaders.TASK_QUEUE_NAME);
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
            return "Bearer " + this.serviceAccountJwtClient.getIdToken(tenantInfo.getName());
        }
    }
}
