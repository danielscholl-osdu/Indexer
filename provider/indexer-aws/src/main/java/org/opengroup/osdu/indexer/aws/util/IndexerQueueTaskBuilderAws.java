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

package org.opengroup.osdu.indexer.aws.util;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.aws.sqs.AmazonSQSConfig;
import org.opengroup.osdu.indexer.util.IndexerQueueTaskBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Primary
@Component
public class IndexerQueueTaskBuilderAws extends IndexerQueueTaskBuilder {

    private AmazonSQS sqsClient;

    @Value("${aws.region}")
    private String region;

    @Value("${aws.sqs.queue}")
    private String queueName;

    @Inject
    public void init() {
        AmazonSQSConfig sqsConfig = new AmazonSQSConfig(region);
        sqsClient = sqsConfig.AmazonSQS();
    }

    @Override
    public void createWorkerTask(String payload, DpsHeaders headers) {
        createTask(payload, headers);
    }

    @Override
    public void createReIndexTask(String payload,DpsHeaders headers) {
        createTask(payload, headers);
    }

    private void createTask(String payload, DpsHeaders headers) {
        String queueUrl = sqsClient.getQueueUrl(queueName).getQueueUrl();
        SendMessageRequest messageRequest = new SendMessageRequest().withQueueUrl(queueUrl).withMessageBody(payload);
        sqsClient.sendMessage(messageRequest);
    }
}
