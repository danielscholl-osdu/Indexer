//  Copyright © Microsoft Corporation
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.opengroup.osdu.indexer.azure.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.resilience4j.retry.RetryConfig;
import lombok.Data;
import lombok.extern.java.Log;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * This class handles retry configuration logic for calls made to <prefix>/storage/v2/query/records:batch
 * to resolve intermittent CosmosDb Not found issue
 */

@Log
@Component
@Data
@ConfigurationProperties(prefix = "azure.storage.client.retry")
public class RetryPolicy {

    @Autowired
    private JaxRsDpsLog logger;

    private int attempts = 3;
    private int waitDuration = 1000;
    private final String RECORD_NOT_FOUND = "notFound";

    /**
     * @return RetryConfig with 3 attempts and 1 sec wait time
     */
    public RetryConfig retryConfig() {
        return RetryConfig.<HttpResponse>custom()
                .maxAttempts(attempts)
                .waitDuration(Duration.ofMillis(waitDuration))
                .retryOnResult(response -> isRetryRequired(response))
                .build();
    }

    /**
     * Unfound records get listed under a JsonArray "notFound" in the http json response
     * @param response
     * @return if there are elements in "notFound" returns true, else false
     */
    private boolean isRetryRequired(HttpResponse response) {
        if (response == null || response.getBody().isEmpty()) {
            return false;
        }
        JsonObject jsonObject = new JsonParser().parse(response.getBody()).getAsJsonObject();
        JsonElement notFoundElement = (JsonArray) jsonObject.get(RECORD_NOT_FOUND);
        if (notFoundElement == null ||
                !notFoundElement.isJsonArray() ||
                notFoundElement.getAsJsonArray().size() == 0 ||
                notFoundElement.getAsJsonArray().isJsonNull()) {
            return false;
        }
        log.info("Retry is set true");
        return true;
    }
}
