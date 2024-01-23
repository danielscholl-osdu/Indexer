/*
 *  Copyright 2020-2023 Google LLC
 *  Copyright 2020-2023 EPAM Systems, Inc
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

package org.opengroup.osdu.indexer.indexing.processing;

import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.Before;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.FromDataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opengroup.osdu.auth.TokenProvider;
import org.opengroup.osdu.oqm.core.model.OqmAckReplier;
import org.opengroup.osdu.oqm.core.model.OqmMessage;
import org.opengroup.osdu.indexer.api.RecordIndexerApi;
import org.opengroup.osdu.indexer.indexing.scope.ThreadDpsHeaders;

@RunWith(Theories.class)
public class RecordsChangedMessageReceiverTest {

  protected ThreadDpsHeaders dpsHeaders = Mockito.mock(ThreadDpsHeaders.class);

  protected TokenProvider tokenProvider = Mockito.mock(TokenProvider.class);

  protected OqmAckReplier ackReplier = Mockito.mock(OqmAckReplier.class);

  private RecordIndexerApi recordIndexerApi = Mockito.mock(RecordIndexerApi.class);

  private RecordsChangedMessageReceiver receiver;

  @Before
  public void setUp() {
    receiver = new RecordsChangedMessageReceiver(dpsHeaders, tokenProvider, recordIndexerApi);
  }

  @DataPoints("VALID_EVENTS")
  public static List<String> validEvents() {
    return ImmutableList.of(
        "/test-events/storage-index-event.json"
    );
  }

  @Theory
  public void shouldReceiveValidEvent(
      @FromDataPoints("VALID_EVENTS") String fileName) throws Exception {
    OqmMessage oqmMessage = ReadFromFileUtil.readEventFromFile(fileName);
    receiver.receiveMessage(oqmMessage, ackReplier);
    verify(ackReplier).ack();
  }
}
