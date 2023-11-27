/** Copyright 2017-2019, Schlumberger
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.opengroup.osdu.util;

import static org.junit.Assert.assertEquals;

import org.apache.http.HttpStatus;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.jersey.api.client.ClientResponse;

import javax.ws.rs.HttpMethod;

public class LegalTagUtilsAws {

    private HTTPClient httpClient;
    public LegalTagUtilsAws(HTTPClient httpClient) {
        this.httpClient = httpClient;
    }
    public String createRandomName() {
        return Config.getDataPartitionIdTenant1() + "-" + System.currentTimeMillis();
    }

    public ClientResponse create(String legalTagName) throws Exception {
        return this.create("US", legalTagName, "2099-01-25", "Public Domain Data");
    }

    protected ClientResponse create(String countryOfOrigin, String name, String expDate, String dataType)
            throws Exception {
        String body = getBody(countryOfOrigin, name, expDate, dataType);
        ClientResponse response = this.httpClient.send(HttpMethod.POST, String.format("%s%s",getLegalUrl(), "legaltags"), body, httpClient.getCommonHeader(), httpClient.getAccessToken());

        assertEquals(HttpStatus.SC_CREATED, response.getStatus());
        Thread.sleep(100);
        return response;
    }

    public ClientResponse delete(String legalTagName) {
        return this.httpClient.send(HttpMethod.DELETE ,getLegalUrl(), "legaltags/" + legalTagName,httpClient.getCommonHeader(), httpClient.getAccessToken());
    }

    protected static String getLegalUrl() {
        String legalUrl = System.getProperty("LEGAL_URL", System.getenv("LEGAL_URL"));
        if (legalUrl == null || legalUrl.contains("-null")) {
            legalUrl = "https://os-legal-dot-opendes.appspot.com/api/legal/v1/";
        }
        return legalUrl;
    }

    protected static String getBody(String countryOfOrigin, String name, String expDate, String dataType) {

        JsonArray coo = new JsonArray();
        coo.add(countryOfOrigin);

        JsonObject properties = new JsonObject();
        properties.add("countryOfOrigin", coo);
        properties.addProperty("contractId", "A1234");
        properties.addProperty("expirationDate", expDate);
        properties.addProperty("dataType", dataType);
        properties.addProperty("originator", "MyCompany");
        properties.addProperty("securityClassification", "Public");
        properties.addProperty("exportClassification", "EAR99");
        properties.addProperty("personalData", "No Personal Data");

        JsonObject tag = new JsonObject();
        tag.addProperty("name", name);
        tag.addProperty("description", "test for " + name);
        tag.add("properties", properties);

        return tag.toString();
    }
}
