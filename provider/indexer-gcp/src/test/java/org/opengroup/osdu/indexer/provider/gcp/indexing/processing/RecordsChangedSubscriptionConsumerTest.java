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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.junit.Before;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.FromDataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.indexer.RecordReindexRequest;
import org.opengroup.osdu.core.common.model.search.CloudTaskRequest;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.indexer.api.RecordIndexerApi;
import org.opengroup.osdu.indexer.api.ReindexApi;

@RunWith(Theories.class)
public class RecordsChangedSubscriptionConsumerTest {

    private final Gson gson = new Gson();

    private DpsHeaders dpsHeaders = Mockito.mock(DpsHeaders.class);

    private RecordIndexerApi recordIndexerApi = Mockito.mock(RecordIndexerApi.class);

    private ReindexApi reindexApi = Mockito.mock(ReindexApi.class);

    private RecordsChangedSubscriptionConsumer consumer;

    @Before
    public void setUp() {
        consumer = new RecordsChangedSubscriptionConsumer(dpsHeaders, recordIndexerApi, reindexApi);
    }

    @DataPoints("REINDEX_TASKS")
    public static ImmutableList<String> reindexEvents() {
        return ImmutableList.of(
            "/test-events/formatted-as-cloud-task-reindex-event.json"
        );
    }

    @DataPoints("INDEX_TASKS")
    public static ImmutableList<String> indexEvents() {
        return ImmutableList.of(
            "/test-events/formatted-as-cloud-task-indexer-reprocess-event.json",
            "/test-events/formatted-as-cloud-task-storage-event.json"
        );
    }

    @Theory
    public void shouldProcessReindexEvents(@FromDataPoints("REINDEX_TASKS") String fileName) throws IOException {
        CloudTaskRequest cloudTaskRequest = readCloudTaskFromFile(fileName);
        consumer.consume(cloudTaskRequest);
        RecordReindexRequest recordReindexRequest = gson.fromJson(cloudTaskRequest.getMessage(), RecordReindexRequest.class);
        verify(reindexApi).reindex(recordReindexRequest, false);
    }

    @Theory
    public void shouldProcessIndexEvents(@FromDataPoints("INDEX_TASKS") String fileName) throws Exception {
        CloudTaskRequest cloudTaskRequest = readCloudTaskFromFile(fileName);
        consumer.consume(cloudTaskRequest);
        ArgumentCaptor<RecordChangedMessages> captor = ArgumentCaptor.forClass(RecordChangedMessages.class);
        verify(recordIndexerApi).indexWorker(captor.capture());
        RecordChangedMessages expectedMessages = this.gson.fromJson(cloudTaskRequest.getMessage(), RecordChangedMessages.class);
        RecordChangedMessages actualMessages = captor.getValue();
        assertEquals(expectedMessages.getData(),actualMessages.getData());
    }

    private CloudTaskRequest readCloudTaskFromFile(String filename) {
        InputStream resourceAsStream = this.getClass().getResourceAsStream(filename);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(resourceAsStream));
        JsonReader reader = new JsonReader(bufferedReader);
        return gson.fromJson(reader, CloudTaskRequest.class);
    }
}
