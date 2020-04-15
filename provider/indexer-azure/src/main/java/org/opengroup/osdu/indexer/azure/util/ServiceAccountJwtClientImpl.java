// Copyright © Microsoft Corporation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.indexer.azure.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.ClientCredential;
import org.apache.http.HttpStatus;

import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.IdToken;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.IJwtCache;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.core.common.util.IServiceAccountJwtClient;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Component
@RequestScope
public class ServiceAccountJwtClientImpl implements IServiceAccountJwtClient {

    @Inject
    private ITenantFactory tenantInfoServiceProvider;

    @Inject
    private DpsHeaders dpsHeaders;

    @Inject
    private IJwtCache cacheService;

    @Inject
    private JaxRsDpsLog log;

    @Inject
    @Named("AAD_OBO_API")
    private String authAPI;

    @Inject
    @Named("AUTH_CLIENT_ID")
    private String authClientID;

    @Inject
    @Named("AUTH_CLIENT_SECRET")
    private String authClientSecret;

    @Inject
    @Named("AUTH_URL")
    private String authURL;

    public String getIdToken(String tenantName) {
        this.log.info("Tenant name received for auth token is: " + tenantName);
        TenantInfo tenant = this.tenantInfoServiceProvider.getTenantInfo(tenantName);
        if (tenant == null) {
            this.log.error("Invalid tenant name receiving from azure");
            throw new AppException(HttpStatus.SC_BAD_REQUEST, "Invalid tenant Name", "Invalid tenant Name from azure");
        }
        String ACCESS_TOKEN = "";
        try {

            IdToken cachedToken = (IdToken) this.cacheService.get(tenant.getServiceAccount());
            this.dpsHeaders.put(DpsHeaders.USER_EMAIL, tenant.getServiceAccount());

            if (!IdToken.refreshToken(cachedToken)) {
                return cachedToken.getTokenValue();
            }

            ExecutorService service = Executors.newFixedThreadPool(1);
            AuthenticationContext context = null;

            try {
                context = new AuthenticationContext(authURL, false, service);
                ClientCredential credential = new ClientCredential(authClientID, authClientSecret);
                Future<AuthenticationResult> future = context.acquireToken(authAPI, credential, null);

                ACCESS_TOKEN =  future.get().getAccessToken();

                if (future == null) {
                    log.error(String.format("Azure Authentication: %s", future.get().getAccessToken()));
                    throw new AppException(HttpStatus.SC_FORBIDDEN, "Access denied", "The user is not authorized to perform this action");
                }
                IdToken idToken = IdToken.builder().tokenValue(ACCESS_TOKEN).expirationTimeMillis(JWT.decode(ACCESS_TOKEN).getExpiresAt().getTime()).build();

                this.cacheService.put(tenant.getServiceAccount(), idToken);

            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                service.shutdown();
            }
        } catch (JWTDecodeException e) {
            throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Persistence error", "Invalid token, error decoding", e);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Persistence error", "Error generating token", e);
        }

        return ACCESS_TOKEN;
    }
}
