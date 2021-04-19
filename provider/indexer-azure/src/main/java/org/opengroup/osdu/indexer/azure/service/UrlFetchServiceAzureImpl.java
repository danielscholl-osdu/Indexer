package org.opengroup.osdu.indexer.azure.service;
import org.opengroup.osdu.core.common.http.FetchServiceHttpRequest;
import org.opengroup.osdu.core.common.http.IUrlFetchService;
import org.opengroup.osdu.core.common.http.UrlFetchServiceImpl;
import org.opengroup.osdu.core.common.model.http.HttpResponse;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import java.net.URISyntaxException;

@Service
@RequestScope
@Primary
public class UrlFetchServiceAzureImpl implements IUrlFetchService {

    private RetryPolicy policy;
    private UrlFetchServiceImpl urlFetchService;

    public UrlFetchServiceAzureImpl()
    {
        policy = new RetryPolicy();
        urlFetchService = new UrlFetchServiceImpl();
    }
    public UrlFetchServiceAzureImpl(RetryPolicy policy,UrlFetchServiceImpl urlFetchService)
    {
        this.policy=policy;
        this.urlFetchService=urlFetchService;
    }

    @Override
    public HttpResponse sendRequest(FetchServiceHttpRequest httpRequest) throws URISyntaxException {
        HttpResponse output;
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        if(isGetStorageRecords(stackTraceElements))
        {
            output = policy.retryFunction(httpRequest);
            if(output!=null)
            {
                return output;
            }
        }
        output=superSendRequest(httpRequest);
        return output;
    }
    protected HttpResponse superSendRequest(FetchServiceHttpRequest httpRequest) throws URISyntaxException
    {
        HttpResponse output=urlFetchService.sendRequest(httpRequest);
        return output;
    }

    private boolean isGetStorageRecords(StackTraceElement[] stElements)
    {
        if(stElements==null)
        {
            return false;
        }
        for (int i=1; i<stElements.length; i++)
        {
            if(stElements[i].getMethodName().equals("getRecords"))
            {
                return true;
            }
        }
        return false;
    }

}
