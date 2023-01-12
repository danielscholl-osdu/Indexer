package org.opengroup.osdu.util;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.webtoken.JsonWebSignature;
import com.google.api.client.json.webtoken.JsonWebToken;
import com.google.api.client.util.Clock;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import lombok.Data;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

class JwtTokenUtil {

    private static String accessToken;

    static String getAccessToken() throws IOException {

        if (Strings.isNullOrEmpty(accessToken)) {
            accessToken = getServiceAccountAccessToken(getJwtForIntegrationTesterAccount());
        }
        return accessToken;
    }

    private static String getServiceAccountAccessToken(String key) throws IOException {

        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {

            List<NameValuePair> parameters = new ArrayList<>();
            parameters.add(new BasicNameValuePair("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer"));
            parameters.add(new BasicNameValuePair("assertion", key));

            HttpPost postRequest = new HttpPost("https://www.googleapis.com/oauth2/v4/token");
            postRequest.addHeader("Content-Type", "application/x-www-form-urlencoded");
            postRequest.setEntity(new UrlEncodedFormEntity(parameters));

            HttpResponse response = httpClient.execute(postRequest);
            String responseContent = IOUtils.toString(response.getEntity().getContent(), Charsets.toCharset("UTF-8"));

            JwtTokenUtil.ResponseToken responseToken = new Gson().fromJson(responseContent, JwtTokenUtil.ResponseToken.class);

            return responseToken.getId_token();
        }
    }

    private static String getJwtForIntegrationTesterAccount() throws IOException {
        String serviceAccountFile = Config.getKeyValue();
        return getJwt(serviceAccountFile);
    }

    private static String getJwt(String serviceAccountFile) throws IOException {
        long currentTime = Clock.SYSTEM.currentTimeMillis();

        InputStream stream = new ByteArrayInputStream(Base64.getDecoder().decode(serviceAccountFile));
        GoogleCredential credential = GoogleCredential.fromStream(stream);

        JsonWebSignature.Header header = new JsonWebSignature.Header();
        header.setAlgorithm("RS256");
        header.setType("JWT");
        header.setKeyId(credential.getServiceAccountPrivateKeyId());

        JsonWebSignature.Payload payload = new JsonWebToken.Payload();
        payload.setIssuedAtTimeSeconds(currentTime / 1000);
        payload.setExpirationTimeSeconds(currentTime / 1000 + 3600);
        payload.setAudience("https://www.googleapis.com/oauth2/v4/token");
        payload.setIssuer(credential.getServiceAccountId());
        payload.set("target_audience", "osdu");

        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        String signedJwt = null;
        try {
            signedJwt = JsonWebSignature.signUsingRsaSha256(credential.getServiceAccountPrivateKey(), jsonFactory, header, payload);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }

        return signedJwt;
    }

    @Data
    class ResponseToken {
        public String id_token;
    }
}
