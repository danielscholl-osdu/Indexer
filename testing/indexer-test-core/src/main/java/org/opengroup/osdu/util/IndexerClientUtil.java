package org.opengroup.osdu.util;

import com.sun.jersey.api.client.ClientResponse;
import lombok.extern.java.Log;

import javax.ws.rs.HttpMethod;
import java.util.Map;

import static org.opengroup.osdu.util.Config.getIndexerBaseURL;

@Log
public class IndexerClientUtil {

    private final HTTPClient httpClient;
    private Map<String, String> headers;

    public IndexerClientUtil(HTTPClient httpClient) {
        this.httpClient = httpClient;
        headers = httpClient.getCommonHeader();
    }

    public void deleteIndex(String kind) {
        String url = getIndexerBaseURL() + "index?kind=" + kind;
        log.info("URL: " + url);
        ClientResponse response = httpClient.send(HttpMethod.DELETE, url, "", headers, httpClient.getAccessToken());
        log.info(response.toString());
    }  
}
