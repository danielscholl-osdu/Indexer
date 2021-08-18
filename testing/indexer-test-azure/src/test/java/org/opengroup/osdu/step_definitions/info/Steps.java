package org.opengroup.osdu.step_definitions.info;

import cucumber.api.Scenario;
import cucumber.api.java.Before;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.opengroup.osdu.common.info.InfoSteps;
import org.opengroup.osdu.util.AzureHTTPClient;
import org.opengroup.osdu.util.Config;

public class Steps extends InfoSteps {

  public Steps() {
    super(new AzureHTTPClient());
  }

  @Before
  public void before(Scenario scenario) {
    this.scenario = scenario;
    this.httpClient = new AzureHTTPClient();
  }

  /******************Inputs being set**************/

  @When("^I send get request to version info endpoint")
  public void i_send_get_request_to_version_info_endpoint() {
    super.i_send_get_request_to_version_info_endpoint();
  }

  /******************Assert final response**************/

  @Then("^I should get version info in response$")
  public void i_should_get_version_info_in_response() {
    super.i_should_get_version_info_in_response();
  }

  @Override
  protected String getHttpMethod() {
    return "GET";
  }

  @Override
  protected String getApi() {
    return Config.getIndexerBaseURL() + "info";
  }
}