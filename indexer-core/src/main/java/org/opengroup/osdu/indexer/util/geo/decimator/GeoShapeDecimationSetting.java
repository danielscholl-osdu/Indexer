package org.opengroup.osdu.indexer.util.geo.decimator;

import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.partition.PartitionInfo;
import org.opengroup.osdu.core.common.partition.Property;
import org.opengroup.osdu.indexer.service.IPartitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class GeoShapeDecimationSetting {
    private static final String PROPERTY_NAME =  "indexer-decimation-enabled";

    @Autowired(required = false)
    private IPartitionService partitionService;

    @Lazy
    @Autowired
    private DecimationSettingCache cache;

    @Autowired
    private JaxRsDpsLog logger;

    @Autowired
    private DpsHeaders headers;

    public boolean isDecimationEnabled() {
        if (partitionService == null)
            return false;

        String dataPartitionId = headers.getPartitionId();
        String cacheKey = String.format("%s-%s", dataPartitionId, PROPERTY_NAME);
        if (cache != null && cache.containsKey(cacheKey))
            return cache.get(cacheKey);

        boolean decimationEnabled = false;
        try {
            PartitionInfo partitionInfo = partitionService.getPartitionInfo();
            decimationEnabled = getDecimationSetting(partitionInfo);
        } catch (Exception e) {
            this.logger.error(String.format("PartitionService: Error getting %s for dataPartition with Id: %s", PROPERTY_NAME, dataPartitionId), e);
        }

        this.cache.put(cacheKey, decimationEnabled);
        return decimationEnabled;
    }


    private boolean getDecimationSetting(PartitionInfo partitionInfo) {
        if(partitionInfo == null || partitionInfo.getProperties() == null)
            return false;

        if(partitionInfo.getProperties().containsKey(PROPERTY_NAME)) {
            Property property = partitionInfo.getProperties().get(PROPERTY_NAME);
            return Boolean.parseBoolean((String)property.getValue());
        }
        return false;
    }
}
