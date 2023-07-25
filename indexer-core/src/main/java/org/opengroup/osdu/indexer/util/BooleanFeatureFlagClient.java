package org.opengroup.osdu.indexer.util;

import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.partition.*;
import org.opengroup.osdu.core.common.util.IServiceAccountJwtClient;
import org.opengroup.osdu.indexer.cache.FeatureFlagCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class BooleanFeatureFlagClient {
    private static final String TOKEN_PREFIX = "Bearer ";

    @Lazy
    @Autowired
    private FeatureFlagCache cache;

    @Autowired
    private JaxRsDpsLog logger;

    @Autowired
    private DpsHeaders headers;

    @Autowired
    private IPartitionFactory factory;

    @Autowired
    private IServiceAccountJwtClient tokenService;

    public boolean isEnabled(String featureName, boolean defaultValue) {
        String dataPartitionId = headers.getPartitionId();
        String cacheKey = String.format("%s-%s", dataPartitionId, featureName);
        if (cache != null && cache.containsKey(cacheKey))
            return cache.get(cacheKey);

        boolean isEnabled = defaultValue;
        try {
            PartitionInfo partitionInfo = getPartitionInfo(dataPartitionId);
            isEnabled = getFeatureValue(partitionInfo, featureName, defaultValue);
        } catch (Exception e) {
            this.logger.error(String.format("PartitionService: Error getting %s for dataPartition with Id: %s. Turn on the feature flag by default.", featureName, dataPartitionId), e);
        }
        this.cache.put(cacheKey, isEnabled);
        return isEnabled;
    }

    private PartitionInfo getPartitionInfo(String dataPartitionId) throws PartitionException {
        try {
            DpsHeaders partitionHeaders = DpsHeaders.createFromMap(headers.getHeaders());
            partitionHeaders.put(DpsHeaders.AUTHORIZATION, this.tokenService.getIdToken(dataPartitionId));

            IPartitionProvider partitionProvider = this.factory.create(partitionHeaders);
            PartitionInfo partitionInfo = partitionProvider.get(dataPartitionId);
            return partitionInfo;
        } catch (PartitionException e) {
            if (e.getResponse() != null) {
                logger.error(String.format("Error getting partition info for data-partition: %s. Message: %s. ResponseCode: %s.", dataPartitionId, e.getResponse().getBody(), e.getResponse().getResponseCode()), e);
            } else {
                logger.error(String.format("Error getting partition info for data-partition: %s.", dataPartitionId), e);
            }
            throw e;
        }
    }

    private boolean getFeatureValue(PartitionInfo partitionInfo, String featureName, boolean defaultValue) {
        if(partitionInfo == null || partitionInfo.getProperties() == null)
            return defaultValue;

        if(partitionInfo.getProperties().containsKey(featureName)) {
            Property property = partitionInfo.getProperties().get(featureName);
            return Boolean.parseBoolean((String)property.getValue());
        }
        return defaultValue;
    }
}
