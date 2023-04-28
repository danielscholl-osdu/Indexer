// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package org.opengroup.osdu.indexer.aws.util;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;
import org.opengroup.osdu.core.aws.iam.IAMConfig;
import org.opengroup.osdu.core.aws.secrets.SecretsManager;

import org.opengroup.osdu.core.common.model.http.AppException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.opengroup.osdu.core.common.http.HttpClient;
import org.opengroup.osdu.core.common.http.HttpRequest;
import org.opengroup.osdu.core.common.http.HttpResponse;
import org.opengroup.osdu.core.common.http.IHttpClient;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import javax.annotation.PostConstruct;

@Component
public class AwsServiceAccountAuthToken {

    @Value("${ENVIRONMENT}")
    private String awsEnvironment;

    @Value("${AWS_REGION}")
    private String awsRegion;

    private final static String ENVIRONMENT = "ENVIRONMENT";
    private final static String REGION = "AWS_REGION";

    private String client_credentials_secret;
    private String client_credentials_clientid;
    private String tokenUrl;
    private String oauthCustomScope;
    private String token= null;
    private long expirationTimeMillis;
    private AWSSimpleSystemsManagement ssmManager;

    @PostConstruct
    private void init() {
        SecretsManager sm = new SecretsManager();
        String environment = awsEnvironment;
        if (environment == null) {
              environment = System.getProperty(ENVIRONMENT, System.getenv(ENVIRONMENT));
          }
          String amazonRegion = awsRegion;
          if (amazonRegion == null) {
              amazonRegion = System.getProperty(REGION, System.getenv(REGION));
          }

          String oauth_token_url = "/osdu/" + environment + "/oauth-token-uri";
          String oauth_custom_scope = "/osdu/" + environment + "/oauth-custom-scope";

          String client_credentials_client_id = "/osdu/" + environment + "/client-credentials-client-id";
          String client_secret_key = "client_credentials_client_secret";
          String client_secret_secretName = "/osdu/" + environment + "/client_credentials_secret";
          AWSCredentialsProvider amazonAWSCredentials = IAMConfig.amazonAWSCredentials();
          this.ssmManager= AWSSimpleSystemsManagementClientBuilder.standard()
              .withCredentials(amazonAWSCredentials)
              .withRegion(amazonRegion)
              .build();

          this.client_credentials_clientid = getSsmParameter(client_credentials_client_id);
          this.client_credentials_secret = sm.getSecret(client_secret_secretName, amazonRegion, client_secret_key);
          this.tokenUrl = getSsmParameter(oauth_token_url);
          this.oauthCustomScope = getSsmParameter(oauth_custom_scope);
      }

    public String getAuthToken() throws AppException {
        if (expiredToken()){
            Map<String,String> headers = new HashMap<>();
            String authorizationHeaderContents=getEncodedAuthorization(this.client_credentials_clientid,this.client_credentials_secret);
            headers.put("Authorization","Basic "+authorizationHeaderContents);
            headers.put("Content-Type", "application/x-www-form-urlencoded");
    
            IHttpClient httpClient = new HttpClient();
            HttpRequest rq = HttpRequest.post().url(this.tokenUrl).headers(headers).body(
                    String.format("%s=%s&%s=%s", "grant_type", "client_credentials", "scope", this.oauthCustomScope)).build();
            HttpResponse result = httpClient.send(rq);
            try {
                AccessToken accessToken = this.getResult(result, AccessToken.class);
                this.token = accessToken.getAccess_token();
                int duration = Integer.parseInt(accessToken.getExpires_in());
                this.expirationTimeMillis = System.currentTimeMillis()+duration*1000;
            }catch(Exception e) {
                System.out.println("Could not parse AccessToken result");
            }
        }
        return this.token; 
    }
    
    private boolean expiredToken() {
        if(this.token == null)
            return true;
        // get a new token if token has 2 minutes to expire
        long diff = this.expirationTimeMillis - System.currentTimeMillis();
        long diffMinutes = (diff / 1000) / 60;
            return diffMinutes <= 2;
    }

    private String getEncodedAuthorization(String clientID, String clientSecret)
    {
        return Base64.getEncoder().encodeToString((clientID+":"+ clientSecret).getBytes());
    }

    private <T> T getResult(HttpResponse result, Class<T> type) throws Exception {
        Gson gson = new Gson();
        if (result.isSuccessCode()) {
            try {
                return gson.fromJson(result.getBody(), type);
            } catch (JsonSyntaxException e) {
                throw new IllegalArgumentException("array parsing error in getResult of HttpResonse, not a valid array");
            }
        } else {
            throw new Exception("Invalid Response");
        }
    }

    private String getSsmParameter(String parameterKey) {
        GetParameterRequest paramRequest = (new GetParameterRequest()).withName(parameterKey).withWithDecryption(true);
        GetParameterResult paramResult = ssmManager.getParameter(paramRequest);
        return paramResult.getParameter().getValue();
    }
}
