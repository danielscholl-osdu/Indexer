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

package org.opengroup.osdu.util;

import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.http.AWSRequestSigningApacheInterceptor;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.opengroup.osdu.core.aws.iam.IAMConfig;

public class ElasticUtilsAws extends ElasticUtils {

    @Override
    protected RestHighLevelClient createClient(String username, String password, String host) {
        AWSCredentialsProvider credentials = new IAMConfig().amazonAWSCredentials();
        AWS4Signer signer = new AWS4Signer();
        signer.setServiceName(username);
        signer.setRegionName(password);
        HttpRequestInterceptor interceptor = new AWSRequestSigningApacheInterceptor(username, signer, credentials);
        return new RestHighLevelClient(RestClient.builder(HttpHost.create(host)).setHttpClientConfigCallback(configCallBack -> configCallBack.addInterceptorLast(interceptor)));
    }
}
