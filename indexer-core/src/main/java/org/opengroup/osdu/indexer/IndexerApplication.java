package org.opengroup.osdu.indexer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.elasticsearch.ElasticsearchRestHealthContributorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ComponentScan({"org.opengroup.osdu.core.common","org.opengroup.osdu.indexer", "org.opengroup.osdu.is"})
@SpringBootApplication(exclude = {ElasticsearchRestHealthContributorAutoConfiguration.class, SecurityAutoConfiguration.class, ManagementWebSecurityAutoConfiguration.class})
@PropertySource("classpath:swagger.properties")
public class IndexerApplication {
    public static void main( String[] args )
    {
        SpringApplication.run(IndexerApplication.class, args);
    }

}
