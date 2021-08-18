package org.opengroup.osdu.step_definitions.info;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
    features = "classpath:features/info/Info.feature",
    glue = {"classpath:org.opengroup.osdu.step_definitions/info"},
    plugin = {"pretty", "junit:target/cucumber-reports/TEST-info.xml"})
public class RunTest {}