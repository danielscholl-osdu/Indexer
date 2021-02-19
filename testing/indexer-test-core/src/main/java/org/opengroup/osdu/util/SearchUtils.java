package org.opengroup.osdu.util;

import static javax.ws.rs.HttpMethod.POST;

import java.util.logging.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.jersey.api.client.ClientResponse;

public class SearchUtils {

  private static final String TAG_QUERY_TEMPLATE = "tags.%s:%s";
  private static final String SEARCH_PAYLOAD_TEMPLATE = "{\n"
      + "  \"kind\": \"%s\",\n"
      + "  \"limit\": 30,\n"
      + "  \"offset\": 0,\n"
      + "  \"query\": \"%s\",\n"
      + "  \"queryAsOwner\": false,\n"
      + "  \"returnedFields\": [\n"
      + "    \"id\"\n"
      + "  ]\n"
      + "}";

  public static int fetchRecordsBySearchQuery(HTTPClient httpClient, String kind, String tagKey, String tagValue) {
    String url = Config.getSearchBaseURL() + "query";
    String tagQuery = String.format(TAG_QUERY_TEMPLATE, tagKey, tagValue);
    String payload = String.format(SEARCH_PAYLOAD_TEMPLATE, kind, tagQuery);
    ClientResponse clientResponse =
        httpClient.send(POST, url, payload, httpClient.getCommonHeader(), httpClient.getAccessToken());
    String stringResponse = clientResponse.getEntity(String.class);
    JsonObject result = new JsonParser().parse(stringResponse).getAsJsonObject();
    return result.get("totalCount").getAsInt();
  }
}
