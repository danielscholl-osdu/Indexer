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

import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.http.AWSRequestSigningApacheInterceptor;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.opengroup.osdu.core.aws.iam.IAMConfig;
import org.opengroup.osdu.indexer.util.ElasticClientHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
// TODO: Elastic Client Handler should be designed to allow cloud providers to implement their own handler if not we have to inherited
// SPI needs to be refactored
@Primary
@Component
public class ElasticClientHandlerAws extends ElasticClientHandler {

    @Value("${aws.es.serviceName}")
    private String serviceName;

    @Value("${aws.region}")
    private String region;

    @Value("${aws.es.host}")
    String host;

    public ElasticClientHandlerAws() {
    }

    @Override
    public RestHighLevelClient createRestClient() {

        return esClient();
    }

    private RestHighLevelClient esClient() {
        AWSCredentialsProvider credentials = new IAMConfig().amazonAWSCredentials();
        AWS4Signer signer = new AWS4Signer();
        signer.setServiceName(serviceName);
        signer.setRegionName(region);
        HttpRequestInterceptor interceptor = new AWSRequestSigningApacheInterceptor(serviceName, signer, credentials);
        return new RestHighLevelClient(RestClient.builder(HttpHost.create(host)).setHttpClientConfigCallback(configCallBack -> configCallBack.addInterceptorLast(interceptor)));

    }
}
