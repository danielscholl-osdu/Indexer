package org.opengroup.osdu.azure;

import org.opengroup.osdu.util.Config;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.logging.Level;
import java.util.logging.Logger;


public class SchemaServiceClient {

    private static final Logger LOGGER = Logger.getLogger(AzureTestIndex.class.getName());

    private final RestTemplate template = new RestTemplate();
    private final String SCHEMA_BASE_URL = Config.getSchemaBaseURL();

    public boolean exists(SchemaIdentity identity) {
        String uri = buildSchemaUri(identity.getId());
        LOGGER.log(Level.INFO, "Checking whether the schema exists having identity={0}", identity);
        ResponseEntity<?> response = template.exchange(uri, HttpMethod.GET, null, Object.class);
        LOGGER.log(Level.INFO, "Finished checking whether the schema exists having identity={0}, response={1}", new Object[]{identity, response});
        return response.getStatusCode() == HttpStatus.OK;
    }

    public void create(SchemaTestModel schema) {
        String uri = buildSchemaUri(schema.getSchemaIdentity().getId());
        LOGGER.log(Level.INFO, "Creating the schema={0}", schema);
        template.put(uri, schema);
        LOGGER.log(Level.INFO, "Finished creating the schema={0}", schema);
    }

    public void createIfNotExist(SchemaTestModel schema) {
        if (!exists(schema.getSchemaIdentity())) {
            create(schema);
        }
    }

    private String buildSchemaUri(String id) {
        return UriComponentsBuilder.fromHttpUrl(SCHEMA_BASE_URL)
                .path("/v1/schema/{schema-id}")
                .build().expand(id).encode()
                .toUriString();
    }

}
