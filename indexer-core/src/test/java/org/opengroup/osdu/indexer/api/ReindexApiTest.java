// Copyright 2017-2019, Schlumberger
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

package org.opengroup.osdu.indexer.api;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.indexer.RecordReindexRequest;
import org.opengroup.osdu.indexer.logging.AuditLogger;
import org.opengroup.osdu.indexer.service.IndexSchemaService;
import org.opengroup.osdu.indexer.service.ReindexService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
public class ReindexApiTest {

    private RecordReindexRequest recordReindexRequest;

    @Mock
    private ReindexService reIndexService;
    @Mock
    private IndexSchemaService indexSchemaService;
    @Mock
    private AuditLogger auditLogger;
    @InjectMocks
    private ReindexApi sut;

    @Before
    public void setup() {
        recordReindexRequest = RecordReindexRequest.builder().kind("tenant:test:test:1.0.0").cursor("100").build();
    }

    @Test
    public void should_return200_when_valid_kind_provided() throws IOException {
        when(this.reIndexService.reindexRecords(recordReindexRequest, false)).thenReturn("something");

        ResponseEntity<?> response = sut.reindex(recordReindexRequest, false);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test(expected = AppException.class)
    public void should_throwAppException_ifUnknownExceptionCaught_reindexTest() throws IOException {
        when(this.reIndexService.reindexRecords(recordReindexRequest, false)).thenThrow(new AppException(500, "", ""));

        sut.reindex(recordReindexRequest, false);
    }

    @Test(expected = NullPointerException.class)
    public void should_throwAppException_ifNullPointerExceptionCaught_ReindexTest() throws IOException {
        when(this.reIndexService.reindexRecords(recordReindexRequest, false)).thenThrow(new NullPointerException(""));

        sut.reindex(recordReindexRequest, false);
    }
}
