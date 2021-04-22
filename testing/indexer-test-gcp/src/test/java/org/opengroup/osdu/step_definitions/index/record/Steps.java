package org.opengroup.osdu.step_definitions.index.record;

import cucumber.api.DataTable;
import cucumber.api.Scenario;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import lombok.extern.java.Log;
import org.opengroup.osdu.common.SchemaServiceRecordSteps;
import org.opengroup.osdu.util.ElasticUtils;
import org.opengroup.osdu.util.GCPHTTPClient;

@Log
public class Steps extends SchemaServiceRecordSteps {

    public Steps() {
        super(new GCPHTTPClient(), new ElasticUtils());
    }

    @Before
    public void before(Scenario scenario) {
        this.scenario = scenario;
        this.httpClient = new GCPHTTPClient();
    }

    @Given("^the schema is created with the following kind$")
    public void the_schema_is_created_with_the_following_kind(DataTable dataTable) {
        super.the_schema_is_created_with_the_following_kind(dataTable);
    }

    @When("^I ingest records with the \"(.*?)\" with \"(.*?)\" for a given \"(.*?)\"$")
    public void i_ingest_records_with_the_for_a_given(String record, String dataGroup, String kind) {
        super.i_ingest_records_with_the_for_a_given(record, dataGroup, kind);
    }

    @Then("^I should get the (\\d+) documents for the \"([^\"]*)\" in the Elastic Search$")
    public void i_should_get_the_documents_for_the_in_the_Elastic_Search(int expectedCount, String index) throws Throwable {
        super.i_should_get_the_documents_for_the_in_the_Elastic_Search(expectedCount, index);
    }

    @Then("^I should get the elastic \"(.*?)\" for the \"([^\"]*)\" and \"([^\"]*)\" in the Elastic Search$")
    public void i_should_get_the_elastic_for_the_tenant_testindex_timestamp_well_in_the_Elastic_Search(String expectedMapping, String type, String index) throws Throwable {
        super.i_should_get_the_elastic_for_the_tenant_testindex_timestamp_well_in_the_Elastic_Search(expectedMapping, type, index);
    }

    @Then("^I should get the (\\d+) documents for the \"([^\"]*)\" in the Elastic Search with out \"(.*?)\"$")
    public void iShouldGetTheNumberDocumentsForTheIndexInTheElasticSearchWithOutSkippedAttribute(int expectedCount, String index, String skippedAttributes) throws Throwable {
        super.iShouldGetTheNumberDocumentsForTheIndexInTheElasticSearchWithOutSkippedAttribute(expectedCount, index, skippedAttributes);
    }

    @Then("^I should be able to search (\\d+) record with index \"([^\"]*)\" by tag \"([^\"]*)\" and value \"([^\"]*)\"$")
    public void iShouldBeAbleToSearchRecordByTagKeyAndTagValue(int expectedNumber, String index, String tagKey, String tagValue) throws Throwable {
        super.iShouldBeAbleToSearchRecordByTagKeyAndTagValue(index, tagKey, tagValue, expectedNumber);
    }

    @Then("^I should be able search (\\d+) documents for the \"([^\"]*)\" by bounding box query with points \\((-?\\d+), (-?\\d+)\\) and  \\((-?\\d+), (-?\\d+)\\) on field \"([^\"]*)\"$")
    public void i_should_get_the_documents_for_the_in_the_Elastic_Search_by_geoQuery(
        int expectedCount, String index, Double topLatitude, Double topLongitude, Double bottomLatitude, Double bottomLongitude, String field) throws Throwable {
        super.i_should_get_the_documents_for_the_in_the_Elastic_Search_by_geoQuery(expectedCount, index, topLatitude, topLongitude, bottomLatitude, bottomLongitude, field);
    }

    @Then("^I should be able search (\\d+) documents for the \"([^\"]*)\" by nested \"([^\"]*)\" and properties \\(\"([^\"]*)\", (\\d+)\\) and  \\(\"([^\"]*)\", \"([^\"]*)\"\\)$")
    public void i_should_get_the_documents_for_the_in_the_Elastic_Search_by_nestedQuery(
        int expectedCount, String index, String path, String firstNestedProperty, String firstNestedValue, String secondNestedProperty,
        String secondNestedValue) throws Exception {
        super.i_should_get_the_documents_for_the_in_the_Elastic_Search_by_nestedQuery(expectedCount, index, path, firstNestedProperty, firstNestedValue,
            secondNestedProperty, secondNestedValue);
    }

}