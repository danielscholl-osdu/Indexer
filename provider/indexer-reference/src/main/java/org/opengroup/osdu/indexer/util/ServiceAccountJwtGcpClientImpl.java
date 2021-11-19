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

package org.opengroup.osdu.indexer.util;

import com.auth0.jwt.JWT;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.iam.v1.Iam;
import com.google.cloud.iam.credentials.v1.GenerateIdTokenResponse;
import com.google.cloud.iam.credentials.v1.IamCredentialsClient;
import com.google.cloud.iam.credentials.v1.ServiceAccountName;
import java.util.Collections;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.IdToken;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.IJwtCache;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.core.common.util.IServiceAccountJwtClient;
import org.opengroup.osdu.core.gcp.multitenancy.credentials.IamCredentialsProvider;
import org.opengroup.osdu.indexer.config.IndexerConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class ServiceAccountJwtGcpClientImpl implements IServiceAccountJwtClient {

  private static final String SERVICE_ACCOUNT_NAME_FORMAT = "projects/%s/serviceAccounts/%s";
  private final IamCredentialsProvider iamCredentialsProvider = new IamCredentialsProvider();

  private final IndexerConfigurationProperties indexerConfigurationProperties;
  private final ITenantFactory tenantInfoServiceProvider;
  private final IJwtCache cacheService;
  private final JaxRsDpsLog log;
  private final DpsHeaders dpsHeaders;

  private Iam iam;

  @Autowired
  public ServiceAccountJwtGcpClientImpl(
      IndexerConfigurationProperties indexerConfigurationProperties,
      ITenantFactory tenantInfoServiceProvider,
      IJwtCache cacheService, JaxRsDpsLog log, DpsHeaders dpsHeaders) {
    this.indexerConfigurationProperties = indexerConfigurationProperties;
    this.tenantInfoServiceProvider = tenantInfoServiceProvider;
    this.cacheService = cacheService;
    this.log = log;
    this.dpsHeaders = dpsHeaders;
  }

  public String getIdToken(String tenantName) {
    this.log.info("Tenant name received for auth token is: " + tenantName);
    TenantInfo tenant = this.tenantInfoServiceProvider.getTenantInfo(tenantName);
    if (tenant == null) {
      this.log.error("Invalid tenant name receiving from pubsub");
      throw new AppException(HttpStatus.SC_BAD_REQUEST, "Invalid tenant Name",
          "Invalid tenant Name from pubsub");
    }
    try {

      IdToken cachedToken = (IdToken) this.cacheService.get(tenant.getServiceAccount());
      // Add the user to DpsHeaders directly
      this.dpsHeaders.put(DpsHeaders.USER_EMAIL, tenant.getServiceAccount());

      if (!IdToken.refreshToken(cachedToken)) {
        return cachedToken.getTokenValue();
      }

      try (IamCredentialsClient iamCredentialsClient = iamCredentialsProvider
          .getIamCredentialsClient()) {
        ServiceAccountName serviceAccountName = ServiceAccountName
            .parse(String.format(SERVICE_ACCOUNT_NAME_FORMAT, tenant.getServiceAccount()));
        GenerateIdTokenResponse idTokenResponse = iamCredentialsClient
            .generateIdToken(serviceAccountName, Collections
                .emptyList(), indexerConfigurationProperties.getGoogleAudiences(), true);
        String token = idTokenResponse.getToken();
        IdToken idToken = IdToken.builder().tokenValue(token)
            .expirationTimeMillis(JWT.decode(token).getExpiresAt().getTime()).build();
        this.cacheService.put(tenant.getServiceAccount(), idToken);
        return token;
      }
    } catch (AppException e) {
      throw e;
    } catch (Exception e) {
      throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Persistence error",
          "Error generating token", e);
    }
  }
}
