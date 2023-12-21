package org.opengroup.osdu.indexer.swagger;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        info = @Info(
                title = "${api.title}",
                description = "${api.description}",
                version = "${api.version}",
                contact = @Contact(name = "${api.contact.name}", email = "${api.contact.email}"),
                license = @License(name = "${api.license.name}", url = "${api.license.url}")),
        security = @SecurityRequirement(name = "Authorization"),
        tags = {
                @Tag(name = "cleanup-indicies-api", description = "Cleanup Indicies API"),
                @Tag(name = "partition-setup-api", description = "Partition Setup API"),
                @Tag(name = "reindex-api", description = "Reindex API"),
                @Tag(name = "health-check-api", description = "Health Check API"),
                @Tag(name = "info", description = "Version info endpoint")
        }
)
@SecurityScheme(name = "Authorization", scheme = "bearer", bearerFormat = "Authorization", type = SecuritySchemeType.HTTP)
@Configuration
public class SwaggerConfiguration {

    @Bean
    public OperationCustomizer operationCustomizer() {
        return (operation, handlerMethod) -> {
            Parameter dataPartitionId = new Parameter()
                    .name(DpsHeaders.DATA_PARTITION_ID)
                    .description("Data Partition Id")
                    .in("header")
                    .required(true)
                    .schema(new StringSchema());
            // PartitionSetupApi - provisionPartition endpoint has 'data-partition-id' header defined at method level.
            if(!operation.getOperationId().equals("provisionPartition"))
                return operation.addParametersItem(dataPartitionId);
            else
                return operation;
        };
    }

}
