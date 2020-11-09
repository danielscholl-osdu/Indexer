package org.opengroup.osdu.step_definitions.index.cleanup;

import cucumber.api.DataTable;
import cucumber.api.Scenario;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import java.io.IOException;
import lombok.extern.java.Log;
import org.opengroup.osdu.common.CleanupIndiciesSteps;
import org.opengroup.osdu.util.GCPHTTPClient;

@Log
public class Steps extends CleanupIndiciesSteps {

    public Steps() {
        super(new GCPHTTPClient());
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

    @Then("^I check that the index for \"(.*?)\" has been created$")
    public void i_check_that_the_index_for_has_been_created(String kind) throws IOException, InterruptedException {
        super.i_check_that_the_index_for_has_been_created(kind);
    }

    @Then("^I should delete the records I created earlier$")
    public void i_should_delete_the_records_for_i_created_earlier() {
        super.i_should_delete_the_records_for_i_created_earlier();
    }

    @Then("^I should delete the schema for \"(.*?)\" I created earlier$")
    public void i_should_delete_the_schema_for_i_created_earlier(String kind) {
        super.i_should_delete_the_schema_for_i_created_earlier(kind);
    }

    @Then("^I should check that the index for \"(.*?)\" has not been deleted$")
    public void i_should_check_that_the_index_for_has_not_been_deleted(String kind) throws IOException, InterruptedException {
        super.i_should_check_that_the_index_for_has_not_been_deleted(kind);
    }

    @Then("^I should to run cleanup of indexes for \"(.*?)\" and \"(.*?)\"$")
    public void i_should_to_run_cleanup_of_indexes_for_and(String kind, String message) {
        super.i_should_to_run_cleanup_of_indexes_for_and(kind, message);
    }

    @Then("^I should check that the index for \"(.*?)\" has been deleted$")
    public void i_should_check_that_the_index_for_has_been_deleted(String kind) throws IOException, InterruptedException {
        super.i_should_check_that_the_index_for_has_been_deleted(kind);
    }
}