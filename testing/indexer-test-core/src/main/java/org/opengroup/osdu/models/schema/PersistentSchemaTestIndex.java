package org.opengroup.osdu.models.schema;

import org.opengroup.osdu.common.SchemaServiceRecordSteps;
import org.opengroup.osdu.models.TestIndex;
import org.opengroup.osdu.util.ElasticUtils;
import org.opengroup.osdu.util.FileHandler;
import org.opengroup.osdu.util.HTTPClient;
import org.opengroup.osdu.util.SchemaServiceClient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class PersistentSchemaTestIndex extends TestIndex {

    private static final Logger LOGGER = Logger.getLogger(PersistentSchemaTestIndex.class.getName());
    private final SchemaServiceClient schemaServiceClient;
    private final SchemaServiceRecordSteps recordSteps;
    private SchemaModel schemaModel;

    public PersistentSchemaTestIndex(ElasticUtils elasticUtils, HTTPClient client, SchemaServiceRecordSteps recordSteps) {
        super(elasticUtils);
        this.schemaServiceClient = new SchemaServiceClient(client);
        this.recordSteps = recordSteps;
    }

    @Override
    public void setupSchema() {
        this.schemaModel = readSchemaFromJson();
        SchemaIdentity schemaIdentity = schemaModel.getSchemaInfo().getSchemaIdentity();
        LOGGER.log(Level.INFO, "Read the schema={0}", schemaIdentity);
        schemaIdentity.setAuthority(recordSteps.generateActualName(schemaIdentity.getAuthority()));
        LOGGER.log(Level.INFO, "Updated the schema={0}", schemaIdentity);
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
