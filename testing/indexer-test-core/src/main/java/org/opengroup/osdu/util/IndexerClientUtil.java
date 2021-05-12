package org.opengroup.osdu.util;

import static org.opengroup.osdu.util.Config.getDataPartitionIdTenant1;
import static org.opengroup.osdu.util.Config.getIndexerBaseURL;

import com.google.gson.Gson;
import com.sun.jersey.api.client.ClientResponse;
import java.util.Map;
import javax.ws.rs.HttpMethod;
import lombok.extern.java.Log;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;

@Log
public class IndexerClientUtil {

    private final String purgeMessage = "{\"data\":\"[{\\\"kind\\\":\\\"%s\\\",\\\"op\\\":\\\"purge_schema\\\"}]\",\"attributes\":{\"account-id\":\"%s\"}}";

    private final HTTPClient httpClient;
    private Map<String, String> headers;

    public IndexerClientUtil(HTTPClient httpClient) {
        this.httpClient = httpClient;
        headers = httpClient.getCommonHeader();
    }

    public void deleteIndex(String kind) {
        String url = getIndexerBaseURL() + "index-cleanup";
        log.info("URL: " + url);
        ClientResponse response = httpClient.send(HttpMethod.POST, url, convertMessageIntoJson(kind), headers, httpClient.getAccessToken());
        log.info(response.toString());
    }

    private String convertMessageIntoJson(String kind) {
        RecordChangedMessages
            recordChangedMessages = (new Gson()).fromJson(String.format(purgeMessage, kind, getDataPartitionIdTenant1()), RecordChangedMessages.class);
        return new Gson().toJson(recordChangedMessages);
    }
}
