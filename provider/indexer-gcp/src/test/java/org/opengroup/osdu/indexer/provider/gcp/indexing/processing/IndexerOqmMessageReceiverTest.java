/*
 *  Copyright 2020-2022 Google LLC
 *  Copyright 2020-2022 EPAM Systems, Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opengroup.osdu.indexer.provider.gcp.indexing.processing;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Before;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.FromDataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opengroup.osdu.core.auth.TokenProvider;
import org.opengroup.osdu.core.common.model.search.CloudTaskRequest;
import org.opengroup.osdu.core.gcp.oqm.model.OqmAckReplier;
import org.opengroup.osdu.core.gcp.oqm.model.OqmMessage;
import org.opengroup.osdu.indexer.provider.gcp.indexing.scope.ThreadDpsHeaders;

@RunWith(Theories.class)
public class IndexerOqmMessageReceiverTest {

    private final Gson gson = new Gson();

    private ThreadDpsHeaders dpsHeaders = Mockito.mock(ThreadDpsHeaders.class);

    private SubscriptionConsumer consumer = Mockito.mock(SubscriptionConsumer.class);

    private TokenProvider tokenProvider = Mockito.mock(TokenProvider.class);

    private OqmAckReplier ackReplier = Mockito.mock(OqmAckReplier.class);

    private IndexerOqmMessageReceiver receiver;

    @Before
    public void setUp() {
        receiver = new IndexerOqmMessageReceiver(dpsHeaders, consumer, tokenProvider);
    }

    @DataPoints("VALID_EVENTS")
    public static List<ImmutablePair> validEvents() {
        return ImmutableList.of(
            ImmutablePair.of("/test-events/storage-index-event.json", "/test-events/formatted-as-cloud-task-storage-event.json"),
            ImmutablePair.of("/test-events/indexer-reprocess-event.json", "/test-events/formatted-as-cloud-task-indexer-reprocess-event.json"),
            ImmutablePair.of("/test-events/reindex-event.json", "/test-events/formatted-as-cloud-task-reindex-event.json")
        );
    }

    @DataPoints("NOT_VALID_EVENTS")
    public static List<String> notValidEvents() {
        return ImmutableList.of(
            "/test-events/empty-data-event.json",
            "/test-events/empty-attributes-event.json"
        );
    }

    @Theory
    public void shouldReceiveValidEvent(@FromDataPoints("VALID_EVENTS") ImmutablePair<String, String> pair) {
        when(consumer.consume(any())).thenReturn(true);
        OqmMessage oqmMessage = readEventFromFile(pair.getLeft());
        CloudTaskRequest cloudTaskRequest = readCloudTaskFromFile(pair.getRight());
        receiver.receiveMessage(oqmMessage, ackReplier);
        verify(consumer).consume(cloudTaskRequest);
        verify(ackReplier).ack();
    }

    @Theory
    public void shouldNotConsumeNotValidEvent(@FromDataPoints("NOT_VALID_EVENTS") String fileName) {
        OqmMessage oqmMessage = readEventFromFile(fileName);
        receiver.receiveMessage(oqmMessage, ackReplier);
        verify(ackReplier).ack();
        verify(consumer, never()).consume(any());
    }

    private OqmMessage readEventFromFile(String filename) {
        InputStream resourceAsStream = this.getClass().getResourceAsStream(filename);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(resourceAsStream));
        JsonReader reader = new JsonReader(bufferedReader);
        return gson.fromJson(reader, OqmMessage.class);
    }

    private CloudTaskRequest readCloudTaskFromFile(String filename) {
        InputStream resourceAsStream = this.getClass().getResourceAsStream(filename);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(resourceAsStream));
        JsonReader reader = new JsonReader(bufferedReader);
        return gson.fromJson(reader, CloudTaskRequest.class);
    }
}
