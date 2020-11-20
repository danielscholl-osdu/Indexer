package org.opengroup.osdu.azure;

import org.opengroup.osdu.models.TestIndex;
import org.opengroup.osdu.util.ElasticUtils;
import org.opengroup.osdu.util.FileHandler;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class AzureTestIndex extends TestIndex {

    private static final Logger LOGGER = Logger.getLogger(AzureTestIndex.class.getName());
    private static final SchemaServiceClient schemaServiceClient = new SchemaServiceClient();

    public AzureTestIndex(ElasticUtils elasticUtils) {
        super(elasticUtils);
    }

    @Override
    public void setupSchema() {
        SchemaTestModel schema = readSchemaFromJson();
        LOGGER.log(Level.INFO, "Setting up the schema={0}", schema.getSchemaIdentity());
        schemaServiceClient.createIfNotExist(schema);
        LOGGER.log(Level.INFO, "Finished setting up the schema={0}", schema.getSchemaIdentity());
    }

    @Override
    public void deleteSchema(String kind) {
        // do nothing
    }

    private SchemaTestModel readSchemaFromJson(){
        try {
            return FileHandler.readFile(getSchemaFile(), SchemaTestModel.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
