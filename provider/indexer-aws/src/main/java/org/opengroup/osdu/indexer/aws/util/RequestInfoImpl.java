// Copyright © Amazon Web Services
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

package org.opengroup.osdu.indexer.aws.util;

import lombok.extern.java.Log;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.provider.interfaces.IRequestInfo;
import org.opengroup.osdu.core.common.search.Preconditions;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import javax.inject.Inject;
import java.util.HashSet;
import java.util.Map;
import static org.opengroup.osdu.core.common.model.http.DpsHeaders.AUTHORIZATION;

@Primary
@Log
@Component
public class RequestInfoImpl implements IRequestInfo {

    @Inject
    private DpsHeaders headersMap;

    @Inject
    private AwsServiceAccountAuthToken awsServiceAccountAuthToken;

    private static final HashSet<String> FORBIDDEN_FROM_LOGGING = new HashSet<>();
    static {
        FORBIDDEN_FROM_LOGGING.add(DpsHeaders.AUTHORIZATION);
        FORBIDDEN_FROM_LOGGING.add(DpsHeaders.ON_BEHALF_OF);
    }

    /**
     * Get list of current headers
     * @return DpsHeaders headers
     */
    @Override
    public DpsHeaders getHeaders() {
        if (headersMap == null) {
            log.warning("Headers Map DpsHeaders is null");
            // throw to prevent null reference exception below
            throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Invalid Headers", "Headers Map DpsHeaders is null");
        }
        DpsHeaders headers = this.getCoreServiceHeaders(headersMap.getHeaders());
        return headers;
    }

    /**
     * get partition id and fallback to account id
     * @return Partition ID
     */
    @Override
    public String getPartitionId() {
        return getHeaders().getPartitionIdWithFallbackToAccountId();
    }

    /**
     * get map of the current headers
     * @return Map<String, String> headers
     */
    @Override
    public Map<String, String> getHeadersMap() {
        return getHeaders().getHeaders();
    }

    @Override
    public Map<String, String> getHeadersMapWithDwdAuthZ() {
        return getHeadersMap();
    }

    @Override
    public DpsHeaders getHeadersWithDwdAuthZ() {
        DpsHeaders ret = getHeaders();
        ret.put(AUTHORIZATION, this.checkOrGetAuthorizationHeader());
        return ret;
    }


    private DpsHeaders getCoreServiceHeaders(Map<String, String> input) {
        Preconditions.checkNotNull(input, "input headers cannot be null");

        DpsHeaders output = DpsHeaders.createFromMap(input);

        return output;
    }

    @Override
    public boolean isCronRequest() {
        return false;
    }

    @Override
    public boolean isTaskQueueRequest() {
        return false;
    }

    private String checkOrGetAuthorizationHeader() {
        return "Bearer " + this.awsServiceAccountAuthToken.getAuthToken();
    }

}
