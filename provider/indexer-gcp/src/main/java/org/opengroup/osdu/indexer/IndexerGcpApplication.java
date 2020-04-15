package org.opengroup.osdu.indexer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication(exclude = { SecurityAutoConfiguration.class, ManagementWebSecurityAutoConfiguration.class })
@Configuration
@ComponentScan({"org.opengroup.osdu"})
public class IndexerGcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(IndexerGcpApplication.class, args);
    }

}
