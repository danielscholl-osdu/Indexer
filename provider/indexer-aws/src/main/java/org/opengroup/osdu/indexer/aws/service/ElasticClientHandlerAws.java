// Copyright © Amazon Web Services
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

package org.opengroup.osdu.indexer.aws.service;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContextBuilder;
import org.elasticsearch.client.RestClient;
import org.opengroup.osdu.indexer.util.ElasticClientHandler;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import lombok.extern.java.Log;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.inject.Inject;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
// TODO: Elastic Client Handler should be designed to allow cloud providers to implement their own handler if not we have to inherited
// SPI needs to be refactored
@Primary
@Component
@Log
public class ElasticClientHandlerAws extends ElasticClientHandler {

    private static final int REST_CLIENT_CONNECT_TIMEOUT = 60000;
    private static final int REST_CLIENT_SOCKET_TIMEOUT = 60000;

    @Value("${aws.es.certificate.disableTrust:false}")
    // @Value("#{new Boolean('${aws.es.certificate.disableTrust:false}')}")
    private Boolean disableSslCertificateTrust;

    public ElasticClientHandlerAws() {
        //DO nothing here, just a class constructor
    }

    @Override
    public RestClientBuilder createClientBuilder(String host, String basicAuthenticationHeaderVal, int port, String protocolScheme, String tls) {

        RestClientBuilder builder = RestClient.builder(new HttpHost(host, port, protocolScheme));
        builder.setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder
                .setConnectTimeout(REST_CLIENT_CONNECT_TIMEOUT)
                .setSocketTimeout(REST_CLIENT_SOCKET_TIMEOUT));        

        if(isLocalHost(host) || disableSslCertificateTrust.booleanValue()) {            

            SSLContext sslContext;            
            try {
                sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[]{ UnsafeX509ExtendedTrustManager.INSTANCE }, null);
                builder.setHttpClientConfigCallback(httpClientBuilder -> 
                httpClientBuilder.setSSLContext(sslContext)
                                .setSSLHostnameVerifier((s, session) -> true));
            } catch (NoSuchAlgorithmException e) {
                log.severe(e.getMessage());
            } catch (KeyManagementException e) {
                log.severe(e.getMessage());
            }

        }
        Header[] defaultHeaders = new Header[]{
                new BasicHeader("client.transport.nodes_sampler_interval", "30s"),
                new BasicHeader("client.transport.ping_timeout", "30s"),
                new BasicHeader("client.transport.sniff", "false"),
                new BasicHeader("request.headers.X-Found-Cluster", host),
                new BasicHeader("cluster.name", host),
                new BasicHeader("xpack.security.transport.ssl.enabled", tls),
                new BasicHeader("Authorization", basicAuthenticationHeaderVal),
        };
        builder.setDefaultHeaders(defaultHeaders);
        return builder;
    }

    private boolean isLocalHost(String uri) {
        return (uri.contains("localhost") || uri.contains("127.0.0.1"));
    }
}
