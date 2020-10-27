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

package org.opengroup.osdu.indexer.azure.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opengroup.osdu.core.common.http.IUrlFetchService;
import org.opengroup.osdu.core.common.model.http.HttpResponse;
import org.opengroup.osdu.core.common.provider.interfaces.IRequestInfo;
import org.opengroup.osdu.indexer.service.impl.SchemaServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(SpringRunner.class)
public class SchemaServiceTest {

    @Mock
    private IUrlFetchService urlFetchService;
    @Mock
    private IRequestInfo requestInfo;
    @InjectMocks
    private SchemaServiceImpl sut;

    @Before
    public void setup() {
        when(this.requestInfo.getHeadersMap()).thenReturn(new HashMap<>());
    }

    @Test
    public void should_returnValidResponse_givenValidKind_getSchemaByKind() throws Exception {

        String validSchemaFromStorage = "{" +
                "  \"kind\": \"tenant:test:test:1.0.0\"," +
                "  \"schema\": [" +
                "    {" +
                "      \"path\": \"msg\"," +
                "      \"kind\": \"string\"" +
                "    }," +
                "    {" +
                "      \"path\": \"references.entity\"," +
                "      \"kind\": \"string\"" +
                "    }" +
                "  ]," +
                "  \"ext\": null" +
                "}";
        String kind = "tenant:test:test:1.0.0";

        HttpResponse httpResponse = new HttpResponse();
        httpResponse.setResponseCode(HttpStatus.OK.value());
        httpResponse.setBody(validSchemaFromStorage);

        when(this.urlFetchService.sendRequest(ArgumentMatchers.any())).thenReturn(httpResponse);

        String recordSchemaResponse = this.sut.getSchema(kind);

        assertNotNull(recordSchemaResponse);
    }

    @Test
    public void should_returnNullResponse_givenAbsentKind_getSchemaByKind() throws Exception {

        String kind = "tenant:test:test:1.0.0";

        HttpResponse httpResponse = new HttpResponse();
        httpResponse.setResponseCode(HttpStatus.NOT_FOUND.value());

        when(this.urlFetchService.sendRequest(ArgumentMatchers.any())).thenReturn(httpResponse);

        String recordSchemaResponse = this.sut.getSchema(kind);

        assertNull(recordSchemaResponse);
    }

}
