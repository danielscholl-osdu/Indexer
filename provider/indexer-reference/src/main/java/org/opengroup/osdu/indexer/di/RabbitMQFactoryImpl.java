/*
 * Copyright 2020 Google LLC
 * Copyright 2020 EPAM Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.indexer.di;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;
import javax.annotation.PostConstruct;
import org.opengroup.osdu.indexer.messagebus.IMessageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RabbitMQFactoryImpl implements IMessageFactory {

	private static final Logger LOG = LoggerFactory.getLogger(RabbitMQFactoryImpl.class);

	@Value("${mb.rabbitmq.uri}")
	private String uri;

	private Channel channel;

	@PostConstruct
	private void init() {
		ConnectionFactory factory = new ConnectionFactory();
		try {
			LOG.debug("RabbitMQ Channel " + uri);
			factory.setUri(uri);
			factory.setAutomaticRecoveryEnabled(true);
			Connection conn = factory.newConnection();
			this.channel = conn.createChannel();
			LOG.debug("RabbitMQ Channel was created.");
			for (String queue : Arrays.asList(DEFAULT_QUEUE_NAME, INDEXER_QUEUE_NAME, LEGAL_QUEUE_NAME)) {
				channel.queueDeclare(queue, true, false, false, null);
				LOG.debug("Queue [" + queue + "] was declared.");
			}
		} catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException | IOException | TimeoutException e) {
			LOG.error(e.getMessage(), e);
		}

	}

	@Override
	public void sendMessage(String msg) {
		this.sendMessage("records", msg);
	}

	@Override
	public void sendMessage(String queueName, String msg) {
		String queueNameWithPrefix = queueName;
		try {
			channel.basicPublish("", queueNameWithPrefix, null, msg.getBytes());
			LOG.info(" [x] Sent '" + msg + "' to queue [" + queueNameWithPrefix + "]");
		} catch (IOException e) {
			LOG.error("Unable to publish message to [" + queueNameWithPrefix + "]");
			LOG.error(e.getMessage(), e);
		}
	}
}
