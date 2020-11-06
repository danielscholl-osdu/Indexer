package org.opengroup.osdu.step_definitions.index.cleanup;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "classpath:features/indexcleanup/IndexCleanup.feature",
        glue = {"classpath:org.opengroup.osdu.step_definitions/index/cleanup"},
        plugin = {"pretty", "junit:target/cucumber-reports/TEST-indexcleanup.xml"})
public class RunTest {
}