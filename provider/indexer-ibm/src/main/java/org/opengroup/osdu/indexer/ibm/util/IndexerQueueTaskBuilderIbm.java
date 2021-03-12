/* Licensed Materials - Property of IBM              */		
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/
 
package org.opengroup.osdu.indexer.ibm.util;

import static org.opengroup.osdu.core.common.Constants.WORKER_RELATIVE_URL;

import java.util.Map;

import javax.inject.Inject;

import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.core.ibm.messagebus.IMessageFactory;
import org.opengroup.osdu.indexer.util.IndexerQueueTaskBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

@Primary
@Component
public class IndexerQueueTaskBuilderIbm extends IndexerQueueTaskBuilder {
	private static final Logger logger = LoggerFactory.getLogger(IndexerQueueTaskBuilderIbm.class);
	
	@Inject
	IMessageFactory mq;
	
	private Gson gson;
	private final static String RETRY_STRING = "retry";
	private final static String ERROR_CODE = "errorCode";
	private final static String ERROR_MESSAGE = "errorMessage";

	@Inject
	public void init() {
		gson = new Gson();
	}

	@Override
	public void createWorkerTask(String payload, DpsHeaders headers) {
		createTask(payload, headers);
	}

	@Override
	public void createReIndexTask(String payload, DpsHeaders headers) {
		createTask(payload, headers);
	}
	
	//used by reindexer api
	@Override
	public void createWorkerTask(String payload, Long countdownMillis, DpsHeaders headers) {
		createTask(payload, headers);
    }

	private void createTask(String payload, DpsHeaders headers) {

		try {
			RecordChangedMessages receivedPayload = gson.fromJson(payload, RecordChangedMessages.class);

			Map<String, String> attributes = receivedPayload.getAttributes();
			int retryCount = 0;
			if (attributes.containsKey(RETRY_STRING)) {
				retryCount = Integer.parseInt(attributes.get(RETRY_STRING));
				retryCount++;
			} else {
				retryCount = 1;
			}
			attributes.put(RETRY_STRING, String.valueOf(retryCount));
			attributes.put(ERROR_CODE, "999"); //error code TBD 
			attributes.put(ERROR_MESSAGE, "Indexer could not process record");
			receivedPayload.setAttributes(attributes);

			// incase if we need to shift logic from indexer-queue-ibm/subscriber.java
			/*
			 * if(Integer.parseInt(receivedPayload.getAttributes().get(RETRY_STRING))>3) {
			 * //add DLQ in IMessageFactory
			 * 
			 * mq.sendMessage("DLQ", gson.toJson(receivedPayload)); }
			 */
			logger.info("Message send back to queue : " + receivedPayload);
			mq.sendMessage(IMessageFactory.DEFAULT_QUEUE_NAME, gson.toJson(receivedPayload));
		} catch (JsonSyntaxException e) {
			logger.error("JsonSyntaxException in IndexerQueueTaskBuilderIbm " + e.toString());
			e.printStackTrace();
		} catch (NumberFormatException e) {
			logger.error("NumberFormatException in IndexerQueueTaskBuilderIbm " + e.toString());
			e.printStackTrace();
		} catch (Exception e) {
			logger.error("Exception in IndexerQueueTaskBuilderIbm " + e.toString());
			e.printStackTrace();
		}

	}
}