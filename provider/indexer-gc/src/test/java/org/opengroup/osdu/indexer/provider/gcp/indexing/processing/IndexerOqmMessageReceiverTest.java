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

package org.opengroup.osdu.indexer.provider.gcp.indexing.processing;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
import org.opengroup.osdu.core.auth.TokenProvider;
import org.opengroup.osdu.core.gcp.oqm.model.OqmAckReplier;
import org.opengroup.osdu.core.gcp.oqm.model.OqmMessage;
import org.opengroup.osdu.indexer.provider.gcp.indexing.scope.ThreadDpsHeaders;

@RunWith(Theories.class)
public class IndexerOqmMessageReceiverTest {

    protected ThreadDpsHeaders dpsHeaders = Mockito.mock(ThreadDpsHeaders.class);

    protected TokenProvider tokenProvider = Mockito.mock(TokenProvider.class);

    protected OqmAckReplier ackReplier = Mockito.mock(OqmAckReplier.class);

    protected IndexerOqmMessageReceiver receiver;

    @Before
    public void setUp(){
      IndexerOqmMessageReceiver indexerOqmMessageReceiver = new IndexerOqmMessageReceiver(
          dpsHeaders, tokenProvider) {
        @Override
        protected void sendMessage(OqmMessage oqmMessage) throws Exception {
            //do nothing
        }
      };
      receiver = Mockito.spy(indexerOqmMessageReceiver);
    }

    @DataPoints("NOT_VALID_EVENTS")
    public static List<String> notValidEvents() {
        return ImmutableList.of(
            "/test-events/empty-data-event.json",
            "/test-events/empty-attributes-event.json"
        );
    }

    @Theory
    public void shouldNotConsumeNotValidEvent(@FromDataPoints("NOT_VALID_EVENTS") String fileName)
        throws Exception {
        OqmMessage oqmMessage = ReadFromFileUtil.readEventFromFile(fileName);
        receiver.receiveMessage(oqmMessage, ackReplier);
        verify(ackReplier).ack();
        verify(receiver, never()).sendMessage(any());
    }
}
