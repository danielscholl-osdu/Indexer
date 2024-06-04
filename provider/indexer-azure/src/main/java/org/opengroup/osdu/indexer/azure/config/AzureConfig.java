package org.opengroup.osdu.indexer.azure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class AzureConfig {

    @Bean
    public Duration slowIndicatorLoggingThreshold() {
        return Duration.ofSeconds(5);
    }

}
