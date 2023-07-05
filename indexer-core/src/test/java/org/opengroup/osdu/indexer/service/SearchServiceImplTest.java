package org.opengroup.osdu.indexer.service;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opengroup.osdu.core.common.http.IUrlFetchService;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.HttpResponse;
import org.opengroup.osdu.core.common.provider.interfaces.IRequestInfo;
import org.opengroup.osdu.indexer.config.IndexerConfigurationProperties;
import org.opengroup.osdu.indexer.model.SearchRequest;
import org.opengroup.osdu.indexer.model.SearchResponse;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

import java.net.URISyntaxException;

import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
public class SearchServiceImplTest {
    @InjectMocks
    private SearchServiceImpl sut;

    @Mock
    private IUrlFetchService urlFetchService;
    @Mock
    private IRequestInfo requestInfo;
    @Mock
    private IndexerConfigurationProperties configurationProperties;
    @Mock
    private JaxRsDpsLog jaxRsDpsLog;

    private String searchHost = "http://localhost";

    @Test
    public void query_without_searchHostSetting() throws URISyntaxException {
        when(this.configurationProperties.getSearchHost()).thenReturn(null);
        SearchResponse response = sut.query(new SearchRequest());
        Assert.assertNotNull(response);
        Assert.assertNull(response.getResults());
    }

    @Test
    public void query_with_responseCode_OK() throws URISyntaxException {
        String bodyJson = "{\n" +
                "  \"results\": [\n" +
                "    {\n" +
                "      \"data\": {\n" +
                "        \"FacilityName\": \"A123\"\n" +
                "      },\n" +
                "      \"kind\": \"osdu:wks:master-data--Wellbore:1.0.0\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"data\": {\n" +
                "        \"FacilityName\": \"B123\"\n" +
                "      },\n" +
                "      \"kind\": \"osdu:wks:master-data--Wellbore:1.0.0\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"aggregations\": null,\n" +
                "  \"totalCount\": 10000\n" +
                "}";
        HttpResponse response = new HttpResponse();
        response.setResponseCode(200);
        response.setBody(bodyJson);
        when(this.configurationProperties.getSearchHost()).thenReturn(searchHost);
        when(this.urlFetchService.sendRequest(any())).thenReturn(response);
        SearchResponse searchResponse = sut.query(new SearchRequest());
        Assert.assertNotNull(searchResponse);
        Assert.assertEquals(10000,searchResponse.getTotalCount());
        Assert.assertEquals(2,searchResponse.getResults().size());
    }

    @Test
    public void query_with_cursor_with_responseCode_OK() throws URISyntaxException {
        String bodyJson = "{\n" +
                "  \"cursor\": \"509E144E7F9B81F8148327D6CB73BB6F\",\n" +
                "  \"results\": [\n" +
                "    {\n" +
                "      \"kind\": \"osdu:wks:master-data--Wellbore:1.0.0\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"kind\": \"osdu:wks:master-data--Wellbore:1.0.1\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"totalCount\": 1000\n" +
                "}";
        HttpResponse response = new HttpResponse();
        response.setResponseCode(200);
        response.setBody(bodyJson);
        when(this.configurationProperties.getSearchHost()).thenReturn(searchHost);
        when(this.urlFetchService.sendRequest(any())).thenReturn(response);
        SearchResponse searchResponse = sut.query(new SearchRequest());
        Assert.assertNotNull(searchResponse);
        Assert.assertNotNull(searchResponse.getCursor());
        Assert.assertEquals(1000,searchResponse.getTotalCount());
        Assert.assertEquals(2,searchResponse.getResults().size());
    }

    @Test
    public void query_with_responseCode_OK_EmptyResult() throws URISyntaxException {
        String bodyJson = "{\n" +
                "  \"results\": [],\n" +
                "  \"aggregations\": [],\n" +
                "  \"totalCount\": 0\n" +
                "}";
        HttpResponse response = new HttpResponse();
        response.setResponseCode(200);
        response.setBody(bodyJson);
        when(this.configurationProperties.getSearchHost()).thenReturn(searchHost);
        when(this.urlFetchService.sendRequest(any())).thenReturn(response);
        SearchResponse searchResponse = sut.query(new SearchRequest());
        Assert.assertNotNull(searchResponse);
        Assert.assertEquals(0,searchResponse.getTotalCount());
        Assert.assertEquals(0,searchResponse.getResults().size());
    }

    @Test
    public void query_with_responseCode_BadRequest() throws URISyntaxException {
        String bodyJson = "{\n" +
                "  \"code\": 400,\n" +
                "  \"reason\": \"Bad Request\",\n" +
                "  \"message\": \"Invalid parameters were given on search request\"\n" +
                "}";
        HttpResponse response = new HttpResponse();
        response.setResponseCode(200);
        response.setBody(bodyJson);
        when(this.configurationProperties.getSearchHost()).thenReturn(searchHost);
        when(this.urlFetchService.sendRequest(any())).thenReturn(response);
        SearchResponse searchResponse = sut.query(new SearchRequest());
        Assert.assertNotNull(searchResponse);
        Assert.assertNull(searchResponse.getResults());
    }

    @Test
    public void query_with_null_response() throws URISyntaxException {
        when(this.configurationProperties.getSearchHost()).thenReturn(searchHost);
        when(this.urlFetchService.sendRequest(any())).thenReturn(null);
        SearchResponse searchResponse = sut.query(new SearchRequest());
        Assert.assertNotNull(searchResponse);
        Assert.assertNull(searchResponse.getResults());
    }

    @Test
    public void query_with_exception() throws URISyntaxException {
        when(this.configurationProperties.getSearchHost()).thenReturn(searchHost);
        when(this.urlFetchService.sendRequest(any())).thenThrow(new AppException(415, "upstream server responded with unsupported media type: text/plain", "Unsupported media type" ));
        assertThrows(URISyntaxException.class, () -> sut.query(new SearchRequest()));
    }
}
