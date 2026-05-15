package edu.harvard.iq.dataverse.api;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static jakarta.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DatasetRelationsIT {

    private static String apiTokenSuperuser;

    @BeforeAll
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();

        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
        String usernameSuperuser = UtilIT.getUsernameFromResponse(createUser);
        apiTokenSuperuser = UtilIT.getApiTokenFromResponse(createUser);
        UtilIT.setSuperuserStatus(usernameSuperuser, true).then().assertThat().statusCode(OK.getStatusCode());

        // Ensure relation type exists
        String relationTypeJson = Json.createObjectBuilder()
                .add("name", "isRelatedTo")
                .add("displayName", "Is Related To")
                .add("inverseName", "isRelatedTo")
                .build().toString();
        UtilIT.addDatasetRelationType(relationTypeJson, apiTokenSuperuser);
    }

    @Test
    public void testDatasetRelationsVersionIsolation() {
        // Create Dataset A, published v1.0
        String dataverseAlias = UtilIT.createRandomCollectionGetAlias(apiTokenSuperuser);
        UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());
        Response createDatasetA = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser);
        String pidA = UtilIT.getDatasetPersistentIdFromResponse(createDatasetA);
        UtilIT.publishDatasetViaNativeApi(pidA, "major", apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());

        // Create Dataset B, draft version
        Response createDatasetB = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser);
        String pidB = UtilIT.getDatasetPersistentIdFromResponse(createDatasetB);

        // Add relation from Dataset B (draft) to Dataset A (v1.0) at dataset B (draft)
        JsonArray relations = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add("relatedDatasetPid", pidA)
                        .add("relationTypeName", "isRelatedTo"))
                .build();

        UtilIT.replaceDatasetRelations(pidB, relations.toString(), apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());

        // Verify relation is listed when requesting relations for Dataset B (draft)
        UtilIT.listDatasetRelations(pidB, null, null, null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("data", hasSize(1))
                .body("data[0].relatedDatasetPid", equalTo(pidA))
                .body("data[0].relationTypeName", equalTo("isRelatedTo"));

        // Verify relation is not listed when requesting relations for Dataset A (v1)
        // since Dataset B, where the relation was defined, is still in Draft status
        UtilIT.listDatasetRelations(pidA, null, null, null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("data", hasSize(0));

        // Publish Dataset B (=> v1.0)
        UtilIT.publishDatasetViaNativeApi(pidB, "major", apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());

        // Verify relation is now listed when requesting relations for Dataset A
        UtilIT.listDatasetRelations(pidA, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("data", hasSize(1))
                .body("data[0].relatedDatasetPid", equalTo(pidB))
                .body("data[0].relationTypeName", equalTo("isRelatedTo"));

        // Verify relation is listed when requesting relations for Dataset B v1.0
        UtilIT.listDatasetRelations(pidB, "1.0", null, null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("data", hasSize(1))
                .body("data[0].relatedDatasetPid", equalTo(pidA))
                .body("data[0].relationTypeName", equalTo("isRelatedTo"));

        // Remove the relation in new version of Dataset B (draft)
        UtilIT.replaceDatasetRelations(pidB, "[]", apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());

        // Verify relation is still listed when requesting relations for Dataset A
        // Because Dataset B is still in draft mode
        UtilIT.listDatasetRelations(pidA, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("data", hasSize(1))
                .body("data[0].relatedDatasetPid", equalTo(pidB))
                .body("data[0].relationTypeName", equalTo("isRelatedTo"));

        // Verify relation is still listed when requesting relations for Dataset B without a token
        // Because without a token, users cannot see Dataset B's draft
        UtilIT.listDatasetRelations(pidB)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("data", hasSize(1))
                .body("data[0].relatedDatasetPid", equalTo(pidA))
                .body("data[0].relationTypeName", equalTo("isRelatedTo"));

        // Verify relation is not listed when requesting relations for Dataset B's draft specifically
        UtilIT.listDatasetRelations(pidB, ":draft", null, null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("data", hasSize(0));

        // Publish Dataset B (=> v2.0)
        UtilIT.publishDatasetViaNativeApi(pidB, "major", apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());

        // Verify relation is NO LONGER listed when requesting relations for Dataset A
        // Because Dataset B is now at v2 and v2 has no relation
        UtilIT.listDatasetRelations(pidA, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("data", hasSize(0));

        // Verify relation is STILL listed when requesting relations for Dataset B v1 specifically
        UtilIT.listDatasetRelations(pidB, "1.0", null, null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("data", hasSize(1))
                .body("data[0].relatedDatasetPid", equalTo(pidA))
                .body("data[0].relationTypeName", equalTo("isRelatedTo"));

        // Verify relation is NOT listed when requesting relations for Dataset B v2
        UtilIT.listDatasetRelations(pidB, "2.0", null, null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("data", hasSize(0));

        // Verify relation is NOT listed when requesting relations for Dataset B
        UtilIT.listDatasetRelations(pidB, null, null, null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("data", hasSize(0));
    }

    @Test
    public void testExternalDatasetRelations() {
        String dataverseAlias = UtilIT.createRandomCollectionGetAlias(apiTokenSuperuser);
        UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());
        Response createDatasetA = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser);
        String pidA = UtilIT.getDatasetPersistentIdFromResponse(createDatasetA);

        // Add external relation to Dataset A (draft)
        String externalUrl = "https://example.org/dataset/12345";
        JsonArray relations = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add("externalIdentifier", externalUrl)
                        .add("identifierScheme", "URL")
                        .add("relationTypeName", "isRelatedTo"))
                .build();

        UtilIT.replaceDatasetRelations(pidA, relations.toString(), apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());

        // Verify external relation is listed
        UtilIT.listDatasetRelations(pidA, null, null, null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("data", hasSize(1))
                .body("data[0].externalIdentifier", equalTo(externalUrl))
                .body("data[0].identifierScheme", equalTo("URL"))
                .body("data[0].relationTypeName", equalTo("isRelatedTo"));

        // Add both internal and external relations
        Response createDatasetB = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser);
        String pidB = UtilIT.getDatasetPersistentIdFromResponse(createDatasetB);

        JsonArray mixedRelations = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add("relatedDatasetPid", pidB)
                        .add("relationTypeName", "isRelatedTo"))
                .add(Json.createObjectBuilder()
                        .add("externalIdentifier", "doi:10.1234/5678")
                        .add("identifierScheme", "DOI")
                        .add("relationTypeName", "isRelatedTo"))
                .build();

        UtilIT.replaceDatasetRelations(pidA, mixedRelations.toString(), apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());

        UtilIT.listDatasetRelations(pidA, null, null, null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("data", hasSize(2))
                .body("relatedDatasetPid", hasItem(pidB))
                .body("externalIdentifier", hasItem("doi:10.1234/5678"))
                .body("identifierScheme", hasItem("DOI"));
    }
}
