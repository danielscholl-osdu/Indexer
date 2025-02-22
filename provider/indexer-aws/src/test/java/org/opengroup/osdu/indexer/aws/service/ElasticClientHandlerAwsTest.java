/**
* Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
* 
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*      http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.opengroup.osdu.indexer.aws.service;

import org.junit.Test;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.boot.test.context.SpringBootTest;

import org.opengroup.osdu.indexer.aws.IndexerAwsApplication;
import org.elasticsearch.client.RestClientBuilder;

import javax.net.ssl.SSLContext;


@RunWith(MockitoJUnitRunner.class)
@SpringBootTest(classes = {IndexerAwsApplication.class})
public class ElasticClientHandlerAwsTest {
    
    @InjectMocks
    private ElasticClientHandlerAws handler = new ElasticClientHandlerAws();

    @Mock
    SSLContext sslContext;

    @Test
    public void createClientBuilder() throws Exception {
        
        // Act
        RestClientBuilder builder = handler.createClientBuilder("localhost", "Bearer", 6469, "protocolScheme", "tls");
        RestClientBuilder updatedBuilder = builder.setPathPrefix("Prefix");
        // Assert
        String cleanPathPrefix = builder.cleanPathPrefix("Prefix");

        Assert.assertEquals("/Prefix", cleanPathPrefix);
        Assert.assertEquals(builder, updatedBuilder);

    }

}

