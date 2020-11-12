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

package org.opengroup.osdu.indexer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.opengroup.osdu.core.common.http.IUrlFetchService;
import org.opengroup.osdu.core.common.model.http.HttpResponse;
import org.opengroup.osdu.core.common.provider.interfaces.IRequestInfo;
import org.opengroup.osdu.indexer.schema.converter.SchemaToStorageFormatImpl;
import org.opengroup.osdu.indexer.service.impl.SchemaServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(SpringRunner.class)
public class SchemaServiceTest {

    private ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    @Spy
    private SchemaToStorageFormatImpl schemaToStorageFormat = new SchemaToStorageFormatImpl(objectMapper);

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

        String validSchemaFromSchemaService = "{\n" +
                "   \"data\":{\n" +
                "      \"allOf\":[\n" +
                "         {\n" +
                "            \"type\":\"object\",\n" +
                "            \"properties\":{\n" +
                "               \"WellID\":{\n" +
                "                  \"type\":\"string\",\n" +
                "                  \"pattern\":\"^srn:<namespace>:master-data\\\\/Well:[^:]+:[0-9]*$\"\n" +
                "               }\n" +
                "            }\n" +
                "         }\n" +
                "      ]\n" +
                "   }\n" +
                "}";
        String kind = "tenant:test:test:1.0.0";

        HttpResponse httpResponse = new HttpResponse();
        httpResponse.setResponseCode(HttpStatus.OK.value());
        httpResponse.setBody(validSchemaFromSchemaService);

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
