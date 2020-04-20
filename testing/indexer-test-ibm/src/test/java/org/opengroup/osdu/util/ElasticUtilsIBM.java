// Copyright 2020 IBM Corp. All Rights Reserved.
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

package org.opengroup.osdu.util;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Base64;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestClientBuilder.HttpClientConfigCallback;
import org.elasticsearch.client.RestHighLevelClient;

import lombok.extern.java.Log;

/**
 * All util methods to use elastic apis for tests
 * It should be used only in the Setup or TearDown phase of the test
 */
@Log
public class ElasticUtilsIBM extends ElasticUtils {
	
	private static final int REST_CLIENT_CONNECT_TIMEOUT = 5000;
    private static final int REST_CLIENT_SOCKET_TIMEOUT = 60000;
    private static final int REST_CLIENT_RETRY_TIMEOUT = 60000;

    public ElasticUtilsIBM() {
        super();
    }
    
    @Override
    protected RestHighLevelClient createClient(String username, String password, String host) {

        RestHighLevelClient restHighLevelClient;
        int port = Config.getPort();
        try {
            String rawString = String.format("%s:%s", username, password);
            RestClientBuilder builder = RestClient.builder(new HttpHost(host, port, "https"));
            builder.setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder.setConnectTimeout(REST_CLIENT_CONNECT_TIMEOUT)
                    .setSocketTimeout(REST_CLIENT_SOCKET_TIMEOUT));
            builder.setMaxRetryTimeoutMillis(REST_CLIENT_RETRY_TIMEOUT);

            Header[] defaultHeaders = new Header[]{
                    new BasicHeader("client.transport.nodes_sampler_interval", "30s"),
                    new BasicHeader("client.transport.ping_timeout", "30s"),
                    new BasicHeader("client.transport.sniff", "false"),
                    new BasicHeader("request.headers.X-Found-Cluster", Config.getElasticHost()),
                    new BasicHeader("cluster.name", Config.getElasticHost()),
                    new BasicHeader("xpack.security.transport.ssl.enabled", Boolean.toString(true)),
                    new BasicHeader("Authorization", String.format("Basic %s", Base64.getEncoder().encodeToString(rawString.getBytes()))),
            };
            
            
            SSLContext sslContext = SSLContext.getInstance("SSL");
            // set up a TrustManager that trusts everything
	        sslContext.init(null, new TrustManager[] { new X509TrustManager() {
	           	public X509Certificate[] getAcceptedIssuers() {
	           		return null;
	           	}
	           	public void checkClientTrusted(X509Certificate[] certs, String authType) { }
	           	public void checkServerTrusted(X509Certificate[] certs, String authType) { }
	         } }, new SecureRandom());
              
               builder.setHttpClientConfigCallback(new HttpClientConfigCallback() {
                   @Override
                   public HttpAsyncClientBuilder customizeHttpClient(
                           HttpAsyncClientBuilder httpClientBuilder) {
                       return httpClientBuilder
                       		.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
                       		.setSSLContext(sslContext);
                   }
               })
               .setDefaultHeaders(defaultHeaders);
               
            restHighLevelClient = new RestHighLevelClient(builder);

        } catch (Exception e) {
            throw new AssertionError("Setup elastic error");
        }
        return restHighLevelClient;
    }
    
}