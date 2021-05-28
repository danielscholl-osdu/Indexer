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

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.gax.rpc.UnaryCallable;
import com.google.cloud.iam.credentials.v1.IamCredentialsClient;
import com.google.cloud.iam.credentials.v1.SignJwtRequest;
import com.google.cloud.iam.credentials.v1.SignJwtResponse;
import com.google.cloud.iam.credentials.v1.stub.IamCredentialsStub;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.search.DeploymentEnvironment;
import org.opengroup.osdu.core.common.model.search.IdToken;
import org.opengroup.osdu.core.common.provider.interfaces.IJwtCache;
import org.opengroup.osdu.indexer.config.IndexerConfigurationProperties;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.reflect.Whitebox;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.when;

@Ignore
@RunWith(SpringRunner.class)
@PrepareForTest({GoogleNetHttpTransport.class, GoogleCredential.class, NetHttpTransport.class, SignJwtResponse.class, HttpClients.class, EntityUtils.class, IndexerConfigurationProperties.class})
public class ServiceAccountJwtGcpClientImplTest {

    private static final String JWT_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ik1UVXlPREE0TXpFd09BPT0ifQ.eyJzdWIiOiJtemh1OUBzbGIuY29tIiwiaXNzIjoic2F1dGgtcHJldmlldy5zbGIuY29tIiwiYXVkIjoidGVzdC1zbGJkZXYtZGV2cG9ydGFsLnNsYmFwcC5jb20iLCJpYXQiOjE1MjgxNDg5MTUsImV4cCI6MTUyODIzNTMxNSwicHJvdmlkZXIiOiJzbGIuY29tIiwiY2xpZW50IjoidGVzdC1zbGJkZXYtZGV2cG9ydGFsLnNsYmFwcC5jb20iLCJ1c2VyaWQiOiJtemh1OUBzbGIuY29tIiwiZW1haWwiOiJtemh1OUBzbGIuY29tIiwiYXV0aHoiOiJ7XCJhY2NvdW50Q291bnRyeVwiOntcImNvZGVcIjpcInVzXCIsXCJpZFwiOjU3MTU5OTkxMDE4MTI3MzYsXCJuYW1lXCI6XCJVbml0ZWQgU3RhdGVzIG9mIEFtZXJpY2FcIn0sXCJhY2NvdW50SWRcIjo1NjkxODc4ODMzOTEzODU2LFwiYWNjb3VudE5hbWVcIjpcIlNJUyBJbnRlcm5hbCBIUVwiLFwiY3JlYXRlZFwiOlwiMjAxOC0wNS0wM1QxNzoyNTo1NS40NDNaXCIsXCJkZXBhcnRtZW50TWFuYWdlclwiOm51bGwsXCJzdWJzY3JpcHRpb25zXCI6W3tcImFjY291bnRJZFwiOjU2OTE4Nzg4MzM5MTM4NTYsXCJjb250cmFjdElkXCI6NTc1MTcwMDIxMjE1NDM2OCxcImNyZWF0ZWRcIjpcIjIwMTgtMDUtMDNUMTc6MzM6MDkuNTczWlwiLFwiY3JtQ29udHJhY3RJZFwiOlwiU0lTLUlOVEVSTkFMLUhRLVFBXCIsXCJjcm1Db250cmFjdEl0ZW1JZFwiOlwiZGV2bGlcIixcImV4cGlyYXRpb25cIjpcIjE5NzAtMDEtMDFUMDA6MDA6MDAuMDAwWlwiLFwiaWRcIjo1MDc5Mjg4NTA0MTIzMzkyLFwicHJvZHVjdFwiOntcImNvZGVcIjpcImRldmVsb3Blci1saWdodFwiLFwiY29tY2F0TmFtZVwiOlwiTm90IGluIENvbUNhdFwiLFwiZmVhdHVyZVNldHNcIjpbe1wiYXBwbGljYXRpb25cIjp7XCJjb2RlXCI6XCJhcGlkZXZlbG9wZXJwb3J0YWxcIixcImlkXCI6NTE2ODkzMDY5NTkzODA0OCxcIm5hbWVcIjpcIkFQSSBEZXZlbG9wZXIgUG9ydGFsXCIsXCJ0eXBlXCI6XCJXZWJBcHBcIn0sXCJjbGFpbXNcIjpudWxsLFwiaWRcIjo1MTkxNTcyMjg3MTI3NTUyLFwibmFtZVwiOlwiRGV2ZWxvcGVyXCIsXCJ0eXBlXCI6XCJCQVNFXCJ9XSxcImlkXCI6NTE1MDczMDE1MTI2NDI1NixcIm5hbWVcIjpcIkRldmVsb3BlciBQb3J0YWxcIixcInBhcnROdW1iZXJcIjpcIlNERUwtUEItU1VCVVwifX1dLFwidXNlckVtYWlsXCI6XCJtemh1OUBzbGIuY29tXCIsXCJ1c2VyTmFtZVwiOlwiTWluZ3lhbmcgWmh1XCJ9XG4iLCJsYXN0bmFtZSI6IlpodSIsImZpcnN0bmFtZSI6Ik1pbmd5YW5nIiwiY291bnRyeSI6IiIsImNvbXBhbnkiOiIiLCJqb2J0aXRsZSI6IiIsInN1YmlkIjoiNDE3YjczMjktYmMwNy00OTFmLWJiYzQtZTQ1YjRhMWFiYjVjLVd3U0c0dyIsImlkcCI6ImNvcnAyIiwiaGQiOiJzbGIuY29tIn0.WQfGr1Xu-6IdaXdoJ9Fwzx8O2el1UkFPWo1vk_ujiAfdOjAR46UG5SrBC7mzC7gYRyK3a4fimBmbv3uRVJjTNXdxXRLZDw0SvXUMIOqjUGLom491ESbrtka_Xz7vGO-tWyDcEQDTfFzQ91LaVN7XdzL18_EDTXZoPhKb-zquyk9WLQxP9Mw-3Yh-UrbvC9nl1-GRn1IVbzp568kqkpOVUFM9alYSGw-oMGDZNt1DIYOJnpGaw2RB5B3AKvNivZH_Xdac7ZTzQbsDOt8B8DL2BphuxcJ9jshCJkM2SHQ15uErv8sfnzMwdF08e_0QcC_30I8eX9l8yOu6TnwwqlXunw";

    @Mock
    private IndexerConfigurationProperties indexerConfigurationProperties;
    @Mock
    private JaxRsDpsLog log;
    @Mock
    private GoogleCredential credential;
    @Mock
    private NetHttpTransport httpTransport;
    @Mock
    private IamCredentialsClient iam;
    @Mock
    private IamCredentialsStub iamCredentialsStub;
    @Mock
    private UnaryCallable<SignJwtRequest, SignJwtResponse> unaryCallable;
    @Mock
    private CloseableHttpClient httpClient;
    @Mock
    private CloseableHttpResponse httpResponse;
//    @InjectMocks
//    private TenantInfoServiceImpl tenantInfoServiceProvider;
//    @Mock
//    private TenantInfoServiceImpl tenantInfoService;
    @Mock
    private IJwtCache cacheService;
    @InjectMocks @Spy
    private ServiceAccountJwtGcpClientImpl sut;
    @Before
    public void setup() throws Exception {
        initMocks(this);

//        mockStatic(GoogleNetHttpTransport.class);
//        mockStatic(GoogleCredential.class);
//        mockStatic(HttpClients.class);
//        mockStatic(EntityUtils.class);
//        mockStatic(Config.class);

        when(GoogleNetHttpTransport.newTrustedTransport()).thenReturn(httpTransport);
        when(GoogleCredential.getApplicationDefault()).thenReturn(credential);
        when(credential.createScopedRequired()).thenReturn(true);
        when(credential.createScoped(any())).thenReturn(credential);
        when(HttpClients.createDefault()).thenReturn(httpClient);
        when(httpClient.execute(any())).thenReturn(httpResponse);
        when(indexerConfigurationProperties.getDeploymentEnvironment()).thenReturn(DeploymentEnvironment.LOCAL);
        when(indexerConfigurationProperties.getGoogleAudiences()).thenReturn("aud");

//        when(this.tenantInfoServiceProvider).thenReturn(this.tenantInfoService);
        
        TenantInfo tenantInfo = new TenantInfo();
        tenantInfo.setServiceAccount("tenant");
//        when(this.tenantInfoService.getTenantInfo()).thenReturn(tenantInfo);

        when(this.sut.getIamCredentialsClient()).thenReturn(iam);
        Whitebox.setInternalState(iam, "stub", iamCredentialsStub);

        SignJwtResponse signJwtResponse = SignJwtResponse.getDefaultInstance();
        SignJwtRequest signJwtRequest = SignJwtRequest.newBuilder().build();

        when(iamCredentialsStub.signJwtCallable()).thenReturn(unaryCallable);
        when(unaryCallable.call(any())).thenReturn(signJwtResponse);
        when(iam.signJwt(signJwtRequest)).thenReturn(signJwtResponse);

    }

    @Test
    public void should_returnCachedToken_givenCachedToken_getIdTokenTest() {
        String tokenValue = "tokenValue";
        IdToken idToken = IdToken.builder().tokenValue(tokenValue).expirationTimeMillis(System.currentTimeMillis() + 10000000L).build();
        when(this.cacheService.get(any())).thenReturn(idToken);

        String returnedIdToken = this.sut.getIdToken(tokenValue);

        Assert.assertEquals(tokenValue, returnedIdToken);
    }

    @Test
    public void should_returnValidToken_getIdTokenTest() throws Exception {
        when(EntityUtils.toString(any())).thenReturn(String.format("{\"id_token\":\"%s\"}", JWT_TOKEN));

        String returnedToken = this.sut.getIdToken("tenant");

        Assert.assertEquals(JWT_TOKEN, returnedToken);
    }

    @Test
    public void should_return500_given_invalidJWTResponse_getIdTokenException() {
        try {
            when(EntityUtils.toString(any())).thenReturn(String.format("{\"id_token\":\"%s\"}", "invalid jwt"));

            this.sut.getIdToken("tenant");
            fail("Should throw exception");
        } catch (AppException e) {
            Assert.assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getError().getCode());
            Assert.assertEquals("Invalid token, error decoding", e.getError().getMessage());
        } catch (Exception e) {
            fail("Should not throw this exception" + e.getMessage());
        }
    }

    @Test
    public void should_return403_given_missingIdTokenResponse_getIdTokenException() {
        try {
            when(EntityUtils.toString(any())).thenReturn("{}");

            this.sut.getIdToken("tenant");
            fail("Should throw exception");
        } catch (AppException e) {
            Assert.assertEquals(HttpStatus.SC_FORBIDDEN, e.getError().getCode());
            Assert.assertEquals("The user is not authorized to perform this action", e.getError().getMessage());
        } catch (Exception e) {
            fail("Should not throw this exception" + e.getMessage());
        }
    }
}
