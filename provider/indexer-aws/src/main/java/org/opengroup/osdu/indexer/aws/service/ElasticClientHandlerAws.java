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
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.opengroup.osdu.indexer.util.ElasticClientHandler;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
// TODO: Elastic Client Handler should be designed to allow cloud providers to implement their own handler if not we have to inherited
// SPI needs to be refactored
@Primary
@Component
public class ElasticClientHandlerAws extends ElasticClientHandler {

    private static final int REST_CLIENT_CONNECT_TIMEOUT = 60000;
    private static final int REST_CLIENT_SOCKET_TIMEOUT = 60000;
    private static final int REST_CLIENT_RETRY_TIMEOUT = 60000;

    public ElasticClientHandlerAws() {
    }

    @Override
    public RestClientBuilder createClientBuilder(String host, String basicAuthenticationHeaderVal, int port, String protocolScheme, String tls) {

        RestClientBuilder builder = RestClient.builder(new HttpHost(host, port, protocolScheme));
        builder.setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder
                .setConnectTimeout(REST_CLIENT_CONNECT_TIMEOUT)
                .setSocketTimeout(REST_CLIENT_SOCKET_TIMEOUT));
        builder.setMaxRetryTimeoutMillis(REST_CLIENT_RETRY_TIMEOUT);

        if(isLocalHost(host)) {
            builder.setHttpClientConfigCallback(httpAsyncClientBuilder -> httpAsyncClientBuilder.setSSLHostnameVerifier((s, sslSession) -> true));
        }
        Header[] defaultHeaders = new Header[]{
                new BasicHeader("client.transport.nodes_sampler_interval", "30s"),
                new BasicHeader("client.transport.ping_timeout", "30s"),
                new BasicHeader("client.transport.sniff", "false"),
                new BasicHeader("request.headers.X-Found-Cluster", host),
                new BasicHeader("cluster.name", host),
                new BasicHeader("xpack.security.transport.ssl.enabled", tls)
        };
        builder.setDefaultHeaders(defaultHeaders);
        return builder;
    }

    private boolean isLocalHost(String uri) {
        return (uri.contains("localhost") || uri.contains("127.0.0.1"));
    }
}
