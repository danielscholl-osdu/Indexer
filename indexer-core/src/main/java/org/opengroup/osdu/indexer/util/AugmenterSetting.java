package org.opengroup.osdu.indexer.util;

import org.opengroup.osdu.core.common.feature.IFeatureFlag;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.indexer.util.geo.decimator.FeatureFlagCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class AugmenterSetting {
    private static final String PROPERTY_NAME =  "index-augmenter-enabled";

    @Lazy
    @Autowired
    private FeatureFlagCache cache;

    @Autowired
    private DpsHeaders headers;

    @Autowired
    private IFeatureFlag iFeatureFlag;

    public boolean isEnabled() {
        String dataPartitionId = headers.getPartitionId();
        String cacheKey = String.format("%s-%s", dataPartitionId, PROPERTY_NAME);
        if (cache != null && cache.containsKey(cacheKey))
            return cache.get(cacheKey);

        boolean isEnabled = iFeatureFlag.isFeatureEnabled(PROPERTY_NAME);
        this.cache.put(cacheKey, isEnabled);
        return isEnabled;
    }
}
