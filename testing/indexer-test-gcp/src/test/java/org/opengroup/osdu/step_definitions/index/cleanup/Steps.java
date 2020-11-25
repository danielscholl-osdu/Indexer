/*
  Copyright 2020 Google LLC
  Copyright 2020 EPAM Systems, Inc

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

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
    public void theSchemaIsCreatedWithTheFollowingKind(DataTable dataTable) {
        super.theSchemaIsCreatedWithTheFollowingKind(dataTable);
    }

    @When("^I ingest records with the \"(.*?)\" with \"(.*?)\" for a given \"(.*?)\"$")
    public void iIngestRecordsWithTheforAGiven(String record, String dataGroup, String kind) {
        super.iIngestRecordsWithTheforAGiven(record, dataGroup, kind);
    }

    @Then("^I check that the index for \"(.*?)\" has been created$")
    public void iCheckThatTheIndexForHasBeenCreated(String kind) throws IOException, InterruptedException {
        super.iCheckThatTheIndexForHasBeenCreated(kind);
    }

    @Then("^I should delete the records I created earlier$")
    public void iShouldDeleteTheRecordsForICreatedEarlier() {
        super.iShouldDeleteTheRecordsForICreatedEarlier();
    }

    @Then("^I should delete the schema for \"(.*?)\" I created earlier$")
    public void iShouldDeleteTheSchemaForICreatedEarlier(String kind) {
        super.iShouldDeleteTheSchemaForICreatedEarlier(kind);
    }

    @Then("^I should check that the index for \"(.*?)\" has not been deleted$")
    public void iShouldCheckThetTheIndexforHasNotBeenDeleted(String kind) throws IOException, InterruptedException {
        super.iShouldCheckThetTheIndexforHasNotBeenDeleted(kind);
    }

    @Then("^I should to run cleanup of indexes for \"(.*?)\" and \"(.*?)\"$")
    public void iShouldToRunCleanupOfIndexesForAnd(String kind, String message) {
        super.iShouldToRunCleanupOfIndexesForAnd(kind, message);
    }

    @Then("^I should check that the index for \"(.*?)\" has been deleted$")
    public void iShouldCheckThatTheIndexForHasBeenDeleted(String kind) throws IOException, InterruptedException {
        super.iShouldCheckThatTheIndexForHasBeenDeleted(kind);
    }
}