package org.opengroup.osdu.common;

import cucumber.api.DataTable;
import org.opengroup.osdu.models.Setup;
import org.opengroup.osdu.models.schema.PersistentSchemaTestIndex;
import org.opengroup.osdu.util.ElasticUtils;
import org.opengroup.osdu.util.HTTPClient;

import java.util.List;
import java.util.Map;

public class SchemaServiceRecordSteps extends RecordSteps {
    private static boolean runStatefulScenario = false;

    public SchemaServiceRecordSteps(HTTPClient httpClient, ElasticUtils elasticUtils) {
        super(httpClient, elasticUtils);
    }

    public void the_schema_is_created_with_the_following_kind(DataTable dataTable) {
        if(!SchemaServiceRecordSteps.runStatefulScenario) {
            List<Setup> inputList = dataTable.asList(Setup.class);
            inputList.forEach(this::setup);
            super.addShutDownHook();
        }
    }

    public void i_set_scenarios_as_stateful(boolean stateful) throws Throwable {
        SchemaServiceRecordSteps.runStatefulScenario = stateful;
    }

    private void setup(Setup input) {
        PersistentSchemaTestIndex testIndex = new PersistentSchemaTestIndex(super.elasticUtils, super.httpClient, this);
        testIndex.setIndex(generateActualName(input.getIndex(), super.getTimeStamp()));
        testIndex.setSchemaFile(input.getSchemaFile());
        testIndex.setHttpClient(super.httpClient);
        testIndex.setupSchema();
        testIndex.setKind(testIndex.getSchemaModel().getSchemaInfo().getSchemaIdentity().getId());

        super.getInputIndexMap().put(testIndex.getKind(), testIndex);

        deleteIndex(testIndex.getKind());
    }

    private void deleteIndex(String kind) {
        this.indexerClientUtil.deleteIndex(kind);
    }

    @Override
    protected String generateRecordId(Map<String, Object> testRecord) {
        return generateActualIdWithoutTs(testRecord.get("id").toString(), testRecord.get("kind").toString());
    }

    @Override
    public void i_ingest_records_with_the_for_a_given(String record, String dataGroup, String kind) {
        super.i_ingest_records_with_the_for_a_given(record.replaceFirst("_schema", ""), dataGroup, kind);
    }
    @Override
    public void i_should_get_object_in_search_response_without_hints_in_schema(String objectField, String index, String recordFile, String acl, String kind)
            throws Throwable {
        super.i_should_get_object_in_search_response_without_hints_in_schema(objectField, index, recordFile, acl, kind);
    }
    @Override
    public void i_should_get_the_documents_for_the_in_the_Elastic_Search_by_AsIngestedCoordinates (
            int expectedCount, String index, Double topPointX, Double bottomPointX, String pointX, Double topPointY, Double bottomPointY, String pointY) throws Throwable {
        super.i_should_get_the_documents_for_the_in_the_Elastic_Search_by_AsIngestedCoordinates(expectedCount, index, topPointX, bottomPointX, pointX, topPointY, bottomPointY, pointY);
    }

}
