// Copyright 2017-2019, Schlumberger
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

package org.opengroup.osdu.indexer.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.cloud.iam.credentials.v1.IamCredentialsClient;
import com.google.cloud.iam.credentials.v1.ServiceAccountName;
import com.google.cloud.iam.credentials.v1.SignJwtRequest;
import com.google.cloud.iam.credentials.v1.SignJwtResponse;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.IdToken;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.IJwtCache;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.core.common.util.IServiceAccountJwtClient;
import org.opengroup.osdu.indexer.config.IndexerConfigurationProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Primary
@Component
@RequestScope
public class ServiceAccountJwtGcpClientImpl implements IServiceAccountJwtClient {

    private static final String JWT_AUDIENCE = "https://www.googleapis.com/oauth2/v4/token";
    private static final String SERVICE_ACCOUNT_NAME_FORMAT = "projects/-/serviceAccounts/%s";

    private final JsonFactory jsonFactory = new JacksonFactory();

    private IamCredentialsClient iam;

    @Inject
    private ITenantFactory tenantInfoServiceProvider;
    @Inject
    private IJwtCache cacheService;
    @Inject
    private JaxRsDpsLog log;
    @Inject
    private DpsHeaders dpsHeaders;
    @Inject
    private IndexerConfigurationProperties properties;

    public String getIdToken(String tenantName) {
        this.log.info("Tenant name received for auth token is: " + tenantName);
        TenantInfo tenant = this.tenantInfoServiceProvider.getTenantInfo(tenantName);
        if (tenant == null) {
            this.log.error("Invalid tenant name receiving from pubsub");
            throw new AppException(HttpStatus.SC_BAD_REQUEST, "Invalid tenant Name", "Invalid tenant Name from pubsub");
        }
        try {

            IdToken cachedToken = (IdToken) this.cacheService.get(tenant.getServiceAccount());
            // Add the user to DpsHeaders directly
            this.dpsHeaders.put(DpsHeaders.USER_EMAIL, tenant.getServiceAccount());

            if (!IdToken.refreshToken(cachedToken)) {
                return cachedToken.getTokenValue();
            }

            // Getting signed JWT
            Map<String, Object> signJwtPayload = this.getJWTCreationPayload(tenant);

            ServiceAccountName name = ServiceAccountName.parse(String.format(SERVICE_ACCOUNT_NAME_FORMAT,
                tenant.getServiceAccount()));
            List<String> delegates = new ArrayList<>();
            delegates.add(tenant.getServiceAccount());

            SignJwtRequest request = SignJwtRequest.newBuilder()
                .setName(name.toString())
                .addAllDelegates(delegates)
                .setPayload(jsonFactory.toString(signJwtPayload))
                .build();
            SignJwtResponse signJwtResponse = this.getIamCredentialsClient().signJwt(request);
            String signedJwt = signJwtResponse.getSignedJwt();

            // Getting id token
            List<NameValuePair> postParameters = new ArrayList<>();
            postParameters.add(new BasicNameValuePair("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer"));
            postParameters.add(new BasicNameValuePair("assertion", signedJwt));

            HttpPost post = new HttpPost(JWT_AUDIENCE);
            post.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
            post.setEntity(new UrlEncodedFormEntity(postParameters, "UTF-8"));

            try(CloseableHttpClient httpclient = HttpClients.createDefault();
                CloseableHttpResponse httpResponse = httpclient.execute(post)) {
                JsonObject jsonContent = new JsonParser().parse(EntityUtils.toString(httpResponse.getEntity())).getAsJsonObject();

                if (!jsonContent.has("id_token")) {
                    log.error(String.format("Google IAM response: %s", jsonContent.toString()));
                    throw new AppException(HttpStatus.SC_FORBIDDEN, "Access denied", "The user is not authorized to perform this action");
                }

                String token = jsonContent.get("id_token").getAsString();
                IdToken idToken = IdToken.builder().tokenValue(token).expirationTimeMillis(JWT.decode(token).getExpiresAt().getTime()).build();

                this.cacheService.put(tenant.getServiceAccount(), idToken);

                return token;
            }
        } catch (JWTDecodeException e) {
            throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Persistence error", "Invalid token, error decoding", e);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Persistence error", "Error generating token", e);
        }
    }

    public IamCredentialsClient getIamCredentialsClient() throws Exception {
        if (this.iam == null) {
            this.iam = IamCredentialsClient.create();
        }
        return this.iam;
    }

    private Map<String, Object> getJWTCreationPayload(TenantInfo tenantInfo) {

        Map<String, Object> payload = new HashMap<>();
        String googleAudience = properties.getGoogleAudiences();
        if (googleAudience.contains(",")) {
            googleAudience = googleAudience.split(",")[0];
        }
        payload.put("target_audience", googleAudience);
        payload.put("exp", System.currentTimeMillis() / 1000 + 3600);
        payload.put("iat", System.currentTimeMillis() / 1000);
        payload.put("iss", tenantInfo.getServiceAccount());
        payload.put("aud", JWT_AUDIENCE);

        return payload;
    }
}
