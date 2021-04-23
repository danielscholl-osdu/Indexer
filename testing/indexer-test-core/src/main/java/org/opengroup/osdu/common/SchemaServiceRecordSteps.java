package org.opengroup.osdu.common;

import cucumber.api.DataTable;
import java.util.List;
import java.util.Map;
import lombok.extern.java.Log;
import org.opengroup.osdu.models.Setup;
import org.opengroup.osdu.models.schema.PersistentSchemaTestIndex;
import org.opengroup.osdu.util.ElasticUtils;
import org.opengroup.osdu.util.HTTPClient;
import org.opengroup.osdu.util.IndexerClientUtil;

@Log
public class SchemaServiceRecordSteps extends RecordSteps {

    private IndexerClientUtil indexerClient;

    public SchemaServiceRecordSteps(HTTPClient httpClient, ElasticUtils elasticUtils) {
        super(httpClient, elasticUtils);
        indexerClient = new IndexerClientUtil(this.httpClient);
    }

    public void the_schema_is_created_with_the_following_kind(DataTable dataTable) {
        List<Setup> inputList = dataTable.asList(Setup.class);
        inputList.forEach(this::createSchema);
        inputList.forEach(s -> deleteIndex(generateActualNameWithoutTs(s.getKind())));
        super.addShutDownHook();
    }

    private void createSchema(Setup input) {
        PersistentSchemaTestIndex testIndex = new PersistentSchemaTestIndex(super.elasticUtils, super.httpClient, this);
        testIndex.setIndex(generateActualName(input.getIndex(), super.getTimeStamp()));
        testIndex.setSchemaFile(input.getSchemaFile());
        testIndex.setHttpClient(super.httpClient);
        testIndex.setupSchema();
        testIndex.setKind(testIndex.getSchemaModel().getSchemaInfo().getSchemaIdentity().getId());

        super.getInputIndexMap().put(testIndex.getKind(), testIndex);
    }

    private void deleteIndex(String kind) {
        indexerClient.deleteIndex(kind);
    }

    @Override
    protected String generateRecordId(Map<String, Object> testRecord) {
        return generateActualIdWithoutTs(testRecord.get("id").toString(), testRecord.get("kind").toString());
    }

    @Override
    public void i_ingest_records_with_the_for_a_given(String record, String dataGroup, String kind) {
        super.i_ingest_records_with_the_for_a_given(record.replaceFirst("_schema", ""), dataGroup, kind);
    }

}
