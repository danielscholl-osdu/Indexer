package org.opengroup.osdu.indexer.azure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Builder;
import lombok.Data;

@Data
@Component
@Builder
@ConfigurationProperties(prefix = "azure.storage.client.retry")
public class RetryPolicyConfig {

    private int MAX_ATTEMPTS;
    private int INITIAL_DELAY;

}
