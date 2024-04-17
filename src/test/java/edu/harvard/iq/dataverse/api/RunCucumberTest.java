package edu.harvard.iq.dataverse.api;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;

@Suite
@IncludeEngines("cucumber")
@SelectPackages("edu.harvard.iq.dataverse")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "edu.harvard.iq.dataverse")
public class RunCucumberTest {
}