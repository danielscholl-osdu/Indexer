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

import com.google.gson.reflect.TypeToken;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opengroup.osdu.core.common.http.IUrlFetchService;
import org.opengroup.osdu.core.common.model.http.HttpResponse;
import org.opengroup.osdu.core.common.model.indexer.RecordInfo;
import org.opengroup.osdu.core.common.provider.interfaces.IRequestInfo;
import org.opengroup.osdu.indexer.service.impl.SchemaServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;

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

        String recordChangedMessages = "[{\"id\":\"tenant1:doc:1dbf528e0e0549cab7a08f29fbfc8465\",\"kind\":\"tenant1:testindexer1528919679710:well:1.0.0\",\"op\":\"purge\"}," +
                "{\"id\":\"tenant1:doc:15e790a69beb4d789b1f979e2af2e813\",\"kind\":\"tenant1:testindexer1528919679710:well:1.0.0\",\"op\":\"create\"}]";

        when(this.requestInfo.getHeadersMap()).thenReturn(new HashMap<>());

        Type listType = new TypeToken<List<RecordInfo>>() {}.getType();
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
