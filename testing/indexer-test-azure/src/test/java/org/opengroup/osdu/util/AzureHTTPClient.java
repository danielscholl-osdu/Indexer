// Copyright © Microsoft Corporation
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

package org.opengroup.osdu.util;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import lombok.ToString;
import lombok.extern.java.Log;

import javax.ws.rs.core.Response;
import java.net.SocketTimeoutException;
import java.util.Map;
@Log
@ToString
public class AzureHTTPClient extends HTTPClient {

    private static String token = null;

    @Override
    public synchronized String getAccessToken() {
        if(token == null) {
            try {
                token = "Bearer " + JwtTokenUtil.getAccessToken();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return token;
    }
    public ClientResponse send(String httpMethod, String url, String payLoad, Map<String, String> headers, String token) {
        ClientResponse response;
        System.out.println("in Azure send method");
        String correlationId = java.util.UUID.randomUUID().toString();
        log.info(String.format("Request correlation id: %s", correlationId));
        headers.put(HEADER_CORRELATION_ID, correlationId);
        Client client = getClient();
        client.setReadTimeout(300000);
        client.setConnectTimeout(300000);
        log.info(String.format("httpMethod: %s", httpMethod));
        log.info(String.format("payLoad: %s", payLoad));
        log.info(String.format("headers: %s", headers));
        log.info(String.format("URL: %s", url));
        WebResource webResource = client.resource(url);
        log.info("waiting on response in azure send");
        int retryCount = 2;
        try{
            response = this.getClientResponse(httpMethod, payLoad, webResource, headers, token);
            while (retryCount > 0) {
                if (response.getStatusInfo().getFamily().equals(Response.Status.Family.valueOf("SERVER_ERROR"))) {
                    log.info(String.format("got resoponse : %s", response.getStatusInfo()));
                    Thread.sleep(5000);
                    log.info(String.format("Retrying --- "));
                    response = this.getClientResponse(httpMethod, payLoad, webResource, headers, token);
                } else
                    break;
                retryCount--;
            }
            System.out.println("sending response from azure send method");
            return response;
        } catch (Exception e) {
            if (e.getCause() instanceof SocketTimeoutException) {
                System.out.println("Retrying in case of socket timeout exception");
                return this.getClientResponse(httpMethod, payLoad, webResource, headers, token);
            }
            e.printStackTrace();
            throw new AssertionError("Error: Send request error", e);
        }
    }
}
