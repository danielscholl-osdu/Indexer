package org.opengroup.osdu.indexer.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AugmenterSetting {
    private static final String PROPERTY_NAME =  "index-augmenter-enabled";

    @Autowired
    private BooleanFeatureFlagClient booleanFeatureFlagClient;

    public boolean isEnabled() {
        return booleanFeatureFlagClient.isEnabled(PROPERTY_NAME, false);
    }
}
