package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.api.UtilIT;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

import java.util.ArrayList;
import java.util.List;

import static jakarta.ws.rs.core.Response.Status.CREATED;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class StepDefinitions {
    private String superuserApiToken;
    private String superuserUsername;

    private List<String> dataverseAliases = new ArrayList();

    // @TODO what if multiple datasets are created by a scenario?
    private Integer datasetId;
    private String datasetPid;

    @Before
    public void setupSuperuser() {
        Response createSuperUser = UtilIT.createRandomUser();
        createSuperUser.prettyPrint();
        createSuperUser.then().assertThat().statusCode(OK.getStatusCode());
        superuserUsername = UtilIT.getUsernameFromResponse(createSuperUser);
        superuserApiToken = UtilIT.getApiTokenFromResponse(createSuperUser);
        Response makeSuperuser = UtilIT.makeSuperUser(superuserUsername);
        makeSuperuser.prettyPrint();
        makeSuperuser.then().assertThat().statusCode(OK.getStatusCode());
    }

    @After
    public void teardownSuperuser() {
        Response deleteUserResponse = UtilIT.deleteUser(superuserUsername);
        deleteUserResponse.prettyPrint();
        assertEquals(200, deleteUserResponse.getStatusCode());
    }

    @After
    public void teardownDataverses() {
        if (datasetId != null) {
            Response deleteDataset = UtilIT.deleteDatasetViaNativeApi(datasetId, superuserApiToken);
            deleteDataset.prettyPrint();
            deleteDataset.then().assertThat().statusCode(OK.getStatusCode());
        }

        for(String dataverseAlias : dataverseAliases) {
            Response deleteDataverse = UtilIT.deleteDataverse(dataverseAlias, superuserApiToken);
            deleteDataverse.prettyPrint();
            deleteDataverse.then().assertThat().statusCode(OK.getStatusCode());
        }
    }

    @Given("a dataverse with alias {string} configured with a {word} PID provider")
    public void a_dataverse_configured_with_a_pid_provider(String dataverseAlias, String pidProvider) {
        dataverseAliases.add(dataverseAlias);
        Response createDataverse = UtilIT.createDataverse(dataverseAlias, null, superuserApiToken);
        createDataverse.prettyPrint();
        createDataverse.then().assertThat().statusCode(CREATED.getStatusCode());

        Response setDefaultPidGenerator;
        switch (pidProvider) {
            case "Permalink":
                setDefaultPidGenerator = UtilIT.setDefaultPidGeneratorForDataverse(dataverseAlias, "permalink", superuserApiToken);
                break;
            case "DOI":
                setDefaultPidGenerator = UtilIT.setDefaultPidGeneratorForDataverse(dataverseAlias, "doi", superuserApiToken);
                break;
            default:
                throw new io.cucumber.java.PendingException();
        }
        setDefaultPidGenerator.prettyPrint();
        setDefaultPidGenerator.then().assertThat().statusCode(OK.getStatusCode());

        Response publishDataverse = UtilIT.publishDataverseViaNativeApi(dataverseAlias, superuserApiToken);
        publishDataverse.prettyPrint();
        assertEquals(OK.getStatusCode(), publishDataverse.getStatusCode());
    }

    @Given("an unpublished dataset in the dataverse with alias {string}")
    public void an_unpublished_dataset_in_the_dataverse_with_alias(String dataverseAlias) {
        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, superuserApiToken);
        createDataset.prettyPrint();
        createDataset.then().assertThat().statusCode(CREATED.getStatusCode());
        if (datasetId != null) {
            // cannot create more than one dataset within a scenario
            throw new io.cucumber.java.PendingException();
        }
        datasetId = UtilIT.getDatasetIdFromResponse(createDataset);
        datasetPid = JsonPath.from(createDataset.asString()).getString("data.persistentId");
    }

    @When("I move that dataset to the dataverse with alias {string}")
    public void i_move_that_dataset_to_the_dataverse_with_alias(String dataverseAlias) {
        Response moveDataset = UtilIT.moveDataset(datasetId.toString(), dataverseAlias, superuserApiToken);
        moveDataset.prettyPrint();
        moveDataset.then().assertThat().statusCode(OK.getStatusCode());
    }

    @When("I publish that dataset")
    public void i_publish_that_dataset() {
        Response publishDataset = UtilIT.publishDatasetViaNativeApi(datasetPid, "major", superuserApiToken);
        publishDataset.prettyPrint();
        publishDataset.then().assertThat().statusCode(OK.getStatusCode());
    }

    @Then("the dataset's PID should be a {word}")
    public void the_datasets_pid_should_be_a(String pidType) {
        Response getDatasetVersions = UtilIT.getDatasetVersions(datasetId.toString(), superuserApiToken);
        getDatasetVersions.prettyPrint();
        getDatasetVersions.then().assertThat().statusCode(OK.getStatusCode());

        JsonPath createdDataset = JsonPath.from(getDatasetVersions.body().asString());
        String datasetPidAfterPublish = createdDataset.getString("data[0].datasetPersistentId");
        switch (pidType) {
            case "Permalink":
                assertEquals(true, datasetPidAfterPublish.startsWith("perma"));
                break;
            case "DOI":
                assertEquals(true, datasetPidAfterPublish.startsWith("doi"));
                break;
            default:
                throw new io.cucumber.java.PendingException();
        }
    }
}
