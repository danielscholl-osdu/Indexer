package org.opengroup.osdu.azure;

import org.opengroup.osdu.models.TestIndex;
import org.opengroup.osdu.util.ElasticUtils;
import org.opengroup.osdu.util.FileHandler;
import org.opengroup.osdu.util.HTTPClient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class AzureTestIndex extends TestIndex {

    private static final Logger LOGGER = Logger.getLogger(AzureTestIndex.class.getName());
    private final SchemaServiceClient schemaServiceClient;
    private SchemaModel schemaModel;

    public AzureTestIndex(ElasticUtils elasticUtils, HTTPClient client) {
        super(elasticUtils);
        this.schemaServiceClient = new SchemaServiceClient(client);
    }

    @Override
    public void setupSchema() {
        this.schemaModel = readSchemaFromJson();
        SchemaIdentity schemaIdentity = schemaModel.getSchemaInfo().getSchemaIdentity();
        LOGGER.log(Level.INFO, "Setting up the schema={0}", schemaIdentity);
        schemaServiceClient.createIfNotExist(schemaModel);
        LOGGER.log(Level.INFO, "Finished setting up the schema={0}", schemaIdentity);
    }

    @Override
    public void deleteSchema(String kind) {
        // The DELETE API is not supported in the Schema service.
        // In order not to overwhelm a DB with a lots of test schemas
        // the integration tests create/update a schema per schema file if the schema does not exists
        // If a developer updates the schema manually, the developer is supposed to update its version as well
    }

    private SchemaModel readSchemaFromJson(){
        try {
            return FileHandler.readFile(getSchemaFile(), SchemaModel.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public SchemaModel getSchemaModel() {
        return schemaModel;
    }

    @Override
    protected String getSchemaFile() {
        return super.getSchemaFile() + ".json";
    }
}
