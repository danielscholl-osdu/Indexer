// Copyright © Microsoft Corporation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.step_definitions.index.record;

import lombok.extern.java.Log;

import cucumber.api.Scenario;
import cucumber.api.java.Before;
import cucumber.api.DataTable;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.opengroup.osdu.azure.AzureTestIndex;
import org.opengroup.osdu.azure.SchemaIdentity;
import org.opengroup.osdu.azure.SchemaModel;
import org.opengroup.osdu.common.RecordSteps;
import org.opengroup.osdu.models.Setup;
import org.opengroup.osdu.util.AzureHTTPClient;
import org.opengroup.osdu.util.ElasticUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Log
public class Steps extends RecordSteps {

    // the mappings in format (kind from feature file : kind from json file)
    private Map<Setup, AzureTestIndex> indexMappings = new HashMap<>();

    public Steps() {
        super(new AzureHTTPClient(), new ElasticUtils());
    }

    @Before
    public void before(Scenario scenario) {
        this.scenario = scenario;
        this.httpClient = new AzureHTTPClient();
    }

    @Given("^the schema is created with the following kind$")
    public void the_schema_is_created_with_the_following_kind(DataTable dataTable) {
        List<Setup> inputList = dataTable.asList(Setup.class);
        inputList.forEach(this::createSchema);
        super.addShutDownHook();

    }

    private String calculateIndex(String commonIndex) {
        SchemaModel schemaModel = indexMappings.entrySet().stream()
                .filter(p -> p.getKey().getIndex().equalsIgnoreCase(commonIndex))
                .findFirst().orElseThrow(RuntimeException::new)
                .getValue().getSchemaModel();
        SchemaIdentity schemaIdentity = schemaModel.getSchemaInfo().getSchemaIdentity();
        return schemaIdentity.getAuthority() + "-testindex" + super.getTimeStamp() + "-" + schemaIdentity.getEntityType() + "-"
                + schemaIdentity.getSchemaVersionMajor() + "." + schemaIdentity.getSchemaVersionMinor() + "." + schemaIdentity.getSchemaVersionPatch();
    }

    private void createSchema(Setup input) {
        AzureTestIndex testIndex = new AzureTestIndex(super.elasticUtils, super.httpClient);
        testIndex.setIndex(generateActualName(input.getIndex(), super.getTimeStamp()));
        testIndex.setSchemaFile(input.getSchemaFile());
        testIndex.setHttpClient(super.httpClient);
        testIndex.setupSchema();
        testIndex.setKind(testIndex.getSchemaModel().getSchemaInfo().getSchemaIdentity().getId());

        this.indexMappings.put(input, testIndex);
        super.getInputIndexMap().put(testIndex.getKind(), testIndex);
    }


    protected String generateRecordId(Map<String, Object> testRecord) {
        String tenant = testRecord.get("id").toString().replaceFirst("tenant1", "opendes");
        return generateActualName(tenant, super.getTimeStamp());
    }

    @When("^I ingest records with the \"(.*?)\" with \"(.*?)\" for a given \"(.*?)\"$")
    public void i_ingest_records_with_the_for_a_given(String record, String dataGroup, String kind) {
        Map.Entry<Setup, AzureTestIndex> setup = indexMappings.entrySet().stream()
                .filter(p -> p.getKey().getKind().equals(kind))
                .findFirst().orElseThrow(RuntimeException::new);
        String id = setup.getValue().getSchemaModel().getSchemaInfo().getSchemaIdentity().getId();
        super.i_ingest_records_with_the_for_a_given(record, dataGroup, id);
    }

    @Then("^I should get the (\\d+) documents for the \"([^\"]*)\" in the Elastic Search$")
    public void i_should_get_the_documents_for_the_in_the_Elastic_Search(int expectedCount, String index) throws Throwable {
        String actualIndex = calculateIndex(index);
        super.i_should_get_the_documents_for_the_in_the_Elastic_Search(expectedCount, actualIndex);
    }

    @Then("^I should get the elastic \"(.*?)\" for the \"([^\"]*)\" and \"([^\"]*)\" in the Elastic Search$")
    public void i_should_get_the_elastic_for_the_tenant_testindex_timestamp_well_in_the_Elastic_Search(String expectedMapping, String type, String index) throws Throwable {
        super.i_should_get_the_elastic_for_the_tenant_testindex_timestamp_well_in_the_Elastic_Search(expectedMapping, type, index);
    }

    @Then("^I should get the (\\d+) documents for the \"([^\"]*)\" in the Elastic Search with out \"(.*?)\"$")
    public void iShouldGetTheNumberDocumentsForTheIndexInTheElasticSearchWithOutSkippedAttribute(int expectedCount, String index, String skippedAttributes) throws Throwable {
        super.iShouldGetTheNumberDocumentsForTheIndexInTheElasticSearchWithOutSkippedAttribute(expectedCount, index, skippedAttributes);
    }

}