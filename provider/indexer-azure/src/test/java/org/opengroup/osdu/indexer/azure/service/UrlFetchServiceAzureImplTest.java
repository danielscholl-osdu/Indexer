package org.opengroup.osdu.indexer.azure.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.opengroup.osdu.core.common.http.FetchServiceHttpRequest;
import org.opengroup.osdu.core.common.http.UrlFetchServiceImpl;
import org.opengroup.osdu.core.common.model.http.HttpResponse;;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import java.net.URISyntaxException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(fullyQualifiedNames = "org.opengroup.osdu.indexer.azure.service.UrlFetchServiceAzureImpl")
public class UrlFetchServiceAzureImplTest {

    @InjectMocks
    private HttpResponse response;
    private UrlFetchServiceImpl urlFetchService;
    private FetchServiceHttpRequest httpRequest;
    private RetryPolicy retryPolicy;

    @Before
    public void setUp(){
        urlFetchService =mock(UrlFetchServiceImpl.class,Mockito.RETURNS_DEEP_STUBS);
        retryPolicy = mock(RetryPolicy.class,Mockito.RETURNS_DEEP_STUBS);
        httpRequest = mock(FetchServiceHttpRequest.class,Mockito.RETURNS_DEEP_STUBS);
    }
    private static final String json3="{\n" +
            " \"records\" :[],\n" +
            " \"conversionStatuses\":[]\n" +
            "}";

    @Test
    public void sendRequest_result_shouldBe_equalTo_superSendRequest_result() throws URISyntaxException {
        UrlFetchServiceAzureImpl spy = spy(new UrlFetchServiceAzureImpl());
        response.setBody(json3);
        doReturn(response).when(spy).superSendRequest(any());
        HttpResponse result = spy.sendRequest(httpRequest);
        assert(result==response);
    }

    @Test
    public void retryFunction_shouldBeCalled_when_isGetStorageRecords_returnsTrue() throws Exception {
        UrlFetchServiceAzureImpl mock= spy(new UrlFetchServiceAzureImpl(retryPolicy,urlFetchService));
        response.setBody(json3);
        when(urlFetchService.sendRequest(httpRequest)).thenReturn(response);
        doReturn(true).when(mock, "isGetStorageRecords", ArgumentMatchers.any());
        mock.sendRequest(httpRequest);
        verify(retryPolicy,times(1)).retryFunction(httpRequest);
    }
    @Test
    public void etryFunction_shouldBeCalled_when_isGetStorageRecords_returnsTrue() throws Exception {
        UrlFetchServiceAzureImpl mock= spy(new UrlFetchServiceAzureImpl(retryPolicy,urlFetchService));
        response.setBody(json3);
        when(urlFetchService.sendRequest(httpRequest)).thenReturn(response);
        doReturn(false).when(mock, "isGetStorageRecords", ArgumentMatchers.any());
        mock.sendRequest(httpRequest);
        verify(retryPolicy,times(0)).retryFunction(httpRequest);
    }

}
