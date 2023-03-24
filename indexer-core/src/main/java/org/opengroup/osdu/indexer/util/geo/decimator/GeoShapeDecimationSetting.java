/*
 * Copyright © Schlumberger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.indexer.util.geo.decimator;

import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.partition.*;
import org.opengroup.osdu.core.common.util.IServiceAccountJwtClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class GeoShapeDecimationSetting {
    private static final String PROPERTY_NAME =  "indexer-decimation-enabled";

    @Lazy
    @Autowired
    private DecimationSettingCache cache;

    @Autowired
    private JaxRsDpsLog logger;

    @Autowired
    private DpsHeaders headers;

    @Autowired
    private IPartitionFactory factory;

    @Autowired
    private IServiceAccountJwtClient tokenService;

    public boolean isDecimationEnabled() {
        String dataPartitionId = headers.getPartitionId();
        String cacheKey = String.format("%s-%s", dataPartitionId, PROPERTY_NAME);
        if (cache != null && cache.containsKey(cacheKey))
            return cache.get(cacheKey);

        boolean decimationEnabled = true;
        try {
            PartitionInfo partitionInfo = getPartitionInfo(dataPartitionId);
            decimationEnabled = getDecimationSetting(partitionInfo);
        } catch (Exception e) {
            this.logger.error(String.format("PartitionService: Error getting %s for dataPartition with Id: %s. Turn on the feature flag by default.", PROPERTY_NAME, dataPartitionId), e);
        }
        this.cache.put(cacheKey, decimationEnabled);
        return decimationEnabled;
    }

    private PartitionInfo getPartitionInfo(String dataPartitionId) {
        try {
            DpsHeaders partitionHeaders = DpsHeaders.createFromMap(headers.getHeaders());
            partitionHeaders.put(DpsHeaders.AUTHORIZATION, this.tokenService.getIdToken(dataPartitionId));

            IPartitionProvider partitionProvider = this.factory.create(partitionHeaders);
            PartitionInfo partitionInfo = partitionProvider.get(dataPartitionId);
            return partitionInfo;
        } catch (PartitionException e) {
            throw new AppException(HttpStatus.SC_FORBIDDEN, "Service unavailable", String.format("Error getting partition info for data-partition: %s", dataPartitionId), e);
        }
    }

    private boolean getDecimationSetting(PartitionInfo partitionInfo) {
        if(partitionInfo == null || partitionInfo.getProperties() == null)
            return true;

        if(partitionInfo.getProperties().containsKey(PROPERTY_NAME)) {
            Property property = partitionInfo.getProperties().get(PROPERTY_NAME);
            return Boolean.parseBoolean((String)property.getValue());
        }
        return true;
    }
}
