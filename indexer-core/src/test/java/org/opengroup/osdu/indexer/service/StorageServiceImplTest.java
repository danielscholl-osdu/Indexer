package org.opengroup.osdu.indexer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.http.IUrlFetchService;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.http.HttpResponse;
import org.opengroup.osdu.core.common.model.indexer.Records;
import org.opengroup.osdu.core.common.provider.interfaces.IRequestInfo;
import org.opengroup.osdu.indexer.config.IndexerConfigurationProperties;

import java.net.URISyntaxException;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class StorageServiceImplTest {

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();
    @Mock
    private IUrlFetchService urlFetchService;
    @Mock
    private IRequestInfo requestInfo;
    @Mock
    private IndexerConfigurationProperties configurationProperties;
    @InjectMocks
    private StorageServiceImpl sut;

    @Test
    public void should_parse_long_integer_values_as_integer_types() throws URISyntaxException {
        when(this.requestInfo.getHeaders()).thenReturn(new DpsHeaders());

        String body = "{\"records\":[{\"id\":\"id1\",\"version\":0,\"data\":{\"long_int\":1000000000000000000000000,\"int\":123}}],\"notFound\":[],\"conversionStatuses\":[],\"missingRetryRecords\":[]}";

        HttpResponse httpResponse = mock(HttpResponse.class);
        when(httpResponse.getBody()).thenReturn(body);
        when(this.urlFetchService.sendRequest(any())).thenReturn(httpResponse);

        Records rec = sut.getRecords(Collections.singletonList("id1"));
        assertEquals(1, rec.getRecords().size());
        assertEquals("1000000000000000000000000", rec.getRecords().get(0).getData().get("long_int").toString());
        assertEquals("123", rec.getRecords().get(0).getData().get("int").toString());
    }
}