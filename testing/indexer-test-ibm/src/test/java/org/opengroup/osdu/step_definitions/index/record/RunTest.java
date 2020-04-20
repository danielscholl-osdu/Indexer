package org.opengroup.osdu.step_definitions.index.record;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "classpath:features/indexrecord/IndexRecord.feature",
        glue = {"classpath:org.opengroup.osdu.step_definitions/index/record"},
        plugin = {"pretty", "junit:target/cucumber-reports/TEST-indexrecord.xml"})
public class RunTest {
}