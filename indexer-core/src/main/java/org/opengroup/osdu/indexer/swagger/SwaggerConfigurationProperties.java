package org.opengroup.osdu.indexer.swagger;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "swagger")
public class SwaggerConfigurationProperties {

    private String apiTitle;
    private String apiDescription;
    private String apiVersion;
    private String contactName;
    private String contactEmail;
    private String licenseName;
    private String licenseUrl;
    private String apiServerUrl;
    private boolean apiServerFullUrlEnabled;

}