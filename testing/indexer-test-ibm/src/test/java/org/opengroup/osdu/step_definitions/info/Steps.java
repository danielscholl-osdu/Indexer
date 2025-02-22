package org.opengroup.osdu.step_definitions.info;

import cucumber.api.Scenario;
import cucumber.api.java.Before;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.opengroup.osdu.common.info.InfoSteps;
import org.opengroup.osdu.util.Config;
import org.opengroup.osdu.util.IBMHTTPClient;

public class Steps extends InfoSteps {

  public Steps() {
    super(new IBMHTTPClient());
  }

  @Before
  public void before(Scenario scenario) {
    this.scenario = scenario;
    this.httpClient = new IBMHTTPClient();
  }

  /******************Inputs being set**************/

  @When("^I send get request to version info endpoint$")
  public void i_send_get_request_to_version_info_endpoint() {
    super.i_send_get_request_to_version_info_endpoint();
  }

  @When("^I send get request to version info endpoint with trailing slash$")
  public void i_send_get_request_to_version_info_endpoint_with_trailing_slash() {
    super.i_send_get_request_to_version_info_endpoint_with_trailing_slash();
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
