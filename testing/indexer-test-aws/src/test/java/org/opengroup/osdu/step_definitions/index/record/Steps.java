// Copyright © Amazon Web Services
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
import org.opengroup.osdu.core.common.model.legal.Legal;
import org.opengroup.osdu.util.AWSHTTPClient;

import cucumber.api.Scenario;
import cucumber.api.java.Before;
import cucumber.api.DataTable;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.opengroup.osdu.util.ElasticUtilsAws;
import org.opengroup.osdu.util.LegalTagUtilsAws;
import org.opengroup.osdu.common.SchemaServiceRecordSteps;

import java.util.HashSet;
import java.util.Set;

import static org.opengroup.osdu.util.Config.getOtherRelevantDataCountries;

@Log
public class Steps extends SchemaServiceRecordSteps {

    protected LegalTagUtilsAws legalTagUtils;
    private String legalTagName;

    public Steps() {
        super(new AWSHTTPClient(), new ElasticUtilsAws());
    }

    @Before
    public void before(Scenario scenario) throws Exception {
        this.scenario = scenario;
        this.httpClient = new AWSHTTPClient();
        legalTagUtils = new LegalTagUtilsAws(this.httpClient);
        this.legalTagName = this.legalTagUtils.createRandomName();
        this.legalTagUtils.create(this.legalTagName);
    }

    @Override
    public void tearDown() {
        super.tearDown();
        this.legalTagUtils.delete(this.legalTagName);
    }

    @Override
    protected Legal generateLegalTag() {
        Legal legal = new Legal();
        Set<String> legalTags = new HashSet<>();
        legalTags.add(this.legalTagName);
        legal.setLegaltags(legalTags);
        Set<String> otherRelevantCountries = new HashSet<>();
        otherRelevantCountries.add(getOtherRelevantDataCountries());
        legal.setOtherRelevantDataCountries(otherRelevantCountries);
        return legal;
    }

    @Given("^the schema is created with the following kind$")
    public void the_schema_is_created_with_the_following_kind(DataTable dataTable){
        super.the_schema_is_created_with_the_following_kind(dataTable);
    }

    @When("^I ingest records with the \"(.*?)\" with \"(.*?)\" for a given \"(.*?)\"$")
    public void i_ingest_records_with_the_for_a_given(String record, String dataGroup, String kind) {
        super.i_ingest_records_with_the_for_a_given(record, dataGroup, kind);
    }

    @When("^I create index with \"(.*?)\" for a given \"(.*?)\" and \"(.*?)\"$")
    public void i_create_index_with_mapping_file_for_a_given_kind(String mappingFile, String index, String kind) throws Throwable {
        super.i_create_index_with_mapping_file_for_a_given_kind(mappingFile, index, kind);
    }

    @Then("^I should get the (\\d+) documents for the \"([^\"]*)\" in the Elastic Search$")
    public void i_should_get_the_documents_for_the_in_the_Elastic_Search(int expectedCount, String index) throws Throwable {
        super.i_should_get_the_documents_for_the_in_the_Elastic_Search(expectedCount, index);
    }

    @Then("^I should not get any documents for the \"([^\"]*)\" in the Elastic Search$")
    public void i_should_not_get_any_documents_for_the_index_in_the_Elastic_Search(String index) throws Throwable {
        super.i_should_not_get_any_documents_for_the_index_in_the_Elastic_Search(index);
    }

    @Then("^I should get the elastic \"(.*?)\" for the \"([^\"]*)\" and \"([^\"]*)\" in the Elastic Search$")
    public void i_should_get_the_elastic_for_the_tenant_testindex_timestamp_well_in_the_Elastic_Search(String expectedMapping, String kind, String index) throws Throwable {
        super.i_should_get_the_elastic_for_the_tenant_testindex_timestamp_well_in_the_Elastic_Search(expectedMapping, kind, index);
    }

    @Then("^I can validate indexed meta attributes for the \"([^\"]*)\" and given \"([^\"]*)\"$")
    public void i_can_validate_indexed_meta_attributes(String index, String kind) throws Throwable {
        super.i_can_validate_indexed_attributes(index, kind);
    }

    @Then("^I should get the (\\d+) documents for the \"([^\"]*)\" in the Elastic Search with out \"(.*?)\"$")
    public void iShouldGetTheNumberDocumentsForTheIndexInTheElasticSearchWithOutSkippedAttribute(int expectedCount, String index, String skippedAttributes) throws Throwable {
        super.iShouldGetTheNumberDocumentsForTheIndexInTheElasticSearchWithOutSkippedAttribute(expectedCount, index, skippedAttributes);
    }

    @Then("^I should be able to search (\\d+) record with index \"([^\"]*)\" by tag \"([^\"]*)\" and value \"([^\"]*)\"$")
    public void iShouldBeAbleToSearchRecordByTagKeyAndTagValue(int expectedNumber, String index, String tagKey, String tagValue) throws Throwable {
        super.iShouldBeAbleToSearchRecordByTagKeyAndTagValue(index, tagKey, tagValue, expectedNumber);
    }

    @Then("^I clean up the index of the extended kinds \"([^\"]*)\" in the Elastic Search$")
    public void iShouldCleanupIndicesOfExtendedKinds(String extendedKinds) throws Throwable {
        super.iShouldCleanupIndicesOfExtendedKinds(extendedKinds);
    }

    @Then("^I should be able to search (\\d+) record with index \"([^\"]*)\" by extended data field \"([^\"]*)\" and value \"([^\"]*)\"$")
    public void iShouldBeAbleToSearchRecordByFieldAndFieldValue(int expectedNumber, String index, String fieldKey, String fieldValue) throws Throwable {
        super.iShouldBeAbleToSearchRecordByFieldAndFieldValue(index, fieldKey, fieldValue, expectedNumber);
    }

    @Then("^I should be able search (\\d+) documents for the \"([^\"]*)\" by bounding box query with points \\((-?\\d+), (-?\\d+)\\) and  \\((-?\\d+), (-?\\d+)\\) on field \"(.*?)\"$")
    public void i_should_get_the_documents_for_the_in_the_Elastic_Search_by_geoQuery (
            int expectedCount, String index, Double topLatitude, Double topLongitude, Double bottomLatitude, Double bottomLongitude, String field) throws Throwable {
        super.i_should_get_the_documents_for_the_in_the_Elastic_Search_by_geoQuery(expectedCount, index, topLatitude, topLongitude, bottomLatitude, bottomLongitude, field);
    }

    @Then("^I should be able search (\\d+) documents for the \"([^\"]*)\" by nested \"([^\"]*)\" and properties \\(\"([^\"]*)\", (\\d+)\\) and  \\(\"([^\"]*)\", \"([^\"]*)\"\\)$")
    public void i_should_get_the_documents_for_the_in_the_Elastic_Search_by_nestedQuery(
        int expectedCount, String index, String path, String firstNestedProperty, String firstNestedValue, String secondNestedProperty,
        String secondNestedValue) throws Throwable {
        super.i_should_get_the_documents_for_the_in_the_Elastic_Search_by_nestedQuery(expectedCount, index, path, firstNestedProperty, firstNestedValue,
            secondNestedProperty, secondNestedValue);
    }

    @Then("^I should be able search (\\d+) documents for the \"([^\"]*)\" by flattened inner properties \\(\"([^\"]*)\", \"([^\"]*)\"\\)$")
    public void i_should_be_able_search_documents_for_the_by_flattened_inner_properties(int expectedCount, String index, String flattenedField,
        String flattenedFieldValue) throws Throwable {
        super.i_should_be_able_search_documents_for_the_by_flattened_inner_properties(expectedCount, index, flattenedField, flattenedFieldValue);

    }

    @Then("^I should get \"([^\"]*)\" in response, without hints in schema for the \"([^\"]*)\" that present in the \"([^\"]*)\" with \"([^\"]*)\" for a given \"([^\"]*)\"$")
    public void i_should_get_object_in_search_response_without_hints_in_schema(String objectInnerField, String index, String recordFile, String acl, String kind)
        throws Throwable {
        super.i_should_get_object_in_search_response_without_hints_in_schema(objectInnerField ,index, recordFile, acl, kind);
    }
}
