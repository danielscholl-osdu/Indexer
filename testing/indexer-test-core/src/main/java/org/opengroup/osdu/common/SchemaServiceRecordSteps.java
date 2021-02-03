package org.opengroup.osdu.common;

import cucumber.api.DataTable;
import org.opengroup.osdu.models.Setup;
import org.opengroup.osdu.models.schema.PersistentSchemaTestIndex;
import org.opengroup.osdu.util.ElasticUtils;
import org.opengroup.osdu.util.HTTPClient;

import java.util.List;
import java.util.Map;

public class SchemaServiceRecordSteps extends RecordSteps {

    public SchemaServiceRecordSteps(HTTPClient httpClient, ElasticUtils elasticUtils) {
        super(httpClient, elasticUtils);
    }

    public void the_schema_is_created_with_the_following_kind(DataTable dataTable) {
        List<Setup> inputList = dataTable.asList(Setup.class);
        inputList.forEach(this::createSchema);
        inputList.forEach(s -> deleteIndex(generateActualNameWithoutTs(s.getIndex())));
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

    private void deleteIndex(String index) {
        this.elasticUtils.deleteIndex(index);
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
