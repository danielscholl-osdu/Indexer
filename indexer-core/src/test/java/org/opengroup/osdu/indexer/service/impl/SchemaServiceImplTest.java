// Copyright 2017-2020, Schlumberger
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
// limitations under the License.package org.opengroup.osdu.indexer.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.opengroup.osdu.core.common.http.IUrlFetchService;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.provider.interfaces.IRequestInfo;
import org.opengroup.osdu.indexer.schema.converter.SchemaToStorageFormatImpl;
import org.opengroup.osdu.indexer.service.StorageService;
import org.opengroup.osdu.indexer.service.impl.SchemaServiceImpl;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
public class SchemaServiceImplTest {

    private ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
    private JaxRsDpsLog jaxRsDpsLog = Mockito.mock(JaxRsDpsLog.class);

    @Spy
    private SchemaToStorageFormatImpl schemaToStorageFormat = new SchemaToStorageFormatImpl(objectMapper, jaxRsDpsLog, null);

    @Mock
    private IUrlFetchService urlFetchService;

    @Mock
    private IRequestInfo requestInfo;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private SchemaServiceImpl sut;

    @Test
    public void test_empty_schema() throws UnsupportedEncodingException, URISyntaxException {
        org.opengroup.osdu.core.common.model.http.HttpResponse httpResponse =
                mock(org.opengroup.osdu.core.common.model.http.HttpResponse.class);
        when(httpResponse.getResponseCode()).thenReturn(HttpStatus.SC_OK);
        when(httpResponse.getBody()).thenReturn("{}");

        when(urlFetchService.sendRequest(any())).thenReturn(httpResponse);
        String schema = sut.getSchema("fake");
        Assert.assertEquals("{\n" +
                "  \"kind\" : \"fake\",\n" +
                "  \"schema\" : [ ]\n" +
                "}", schema);

    }
}