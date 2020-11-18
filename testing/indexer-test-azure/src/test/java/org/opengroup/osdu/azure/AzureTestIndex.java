package org.opengroup.osdu.azure;

import com.sun.jersey.api.client.ClientResponse;
import org.opengroup.osdu.models.TestIndex;
import org.opengroup.osdu.util.ElasticUtils;

import javax.ws.rs.HttpMethod;

import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.opengroup.osdu.util.Config.getSchemaBaseURL;
import static org.opengroup.osdu.util.Config.getStorageBaseURL;

public class AzureTestIndex extends TestIndex {

    private static final Logger LOGGER = Logger.getLogger(AzureTestIndex.class.getName());

    public AzureTestIndex(ElasticUtils elasticUtils) {
        super(elasticUtils);
    }

    @Override
    public void setupSchema() {
        ClientResponse clientResponse = super.getHttpClient().send(HttpMethod.POST, getSchemaBaseURL() + "schemas", super.getStorageSchemaFromJson(), super.getHeaders(), super.getHttpClient().getAccessToken());
        if (clientResponse.getType() != null)
            LOGGER.info(String.format("Response status: %s, type: %s", clientResponse.getStatus(), clientResponse.getType().toString()));
    }

    @Override
    public void deleteSchema(String kind) {
        ClientResponse clientResponse = super.getHttpClient().send(HttpMethod.DELETE, getStorageBaseURL() + "schemas/" + kind, null, super.getHeaders(), super.getHttpClient().getAccessToken());
        assertEquals(204, clientResponse.getStatus());
        if (clientResponse.getType() != null)
            LOGGER.info(String.format("Response status: %s, type: %s", clientResponse.getStatus(), clientResponse.getType().toString()));
    }
}
