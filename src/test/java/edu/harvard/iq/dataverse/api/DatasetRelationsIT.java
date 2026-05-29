package edu.harvard.iq.dataverse.api;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static jakarta.ws.rs.core.Response.Status.CREATED;
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

        // Ensure relation types exist
        String relationTypeJson = Json.createObjectBuilder()
                .add("name", "isRelatedTo")
                .add("displayName", "Is related to")
                .add("inverseName", "isRelatedTo")
                .build().toString();
        UtilIT.addDatasetRelationType(relationTypeJson, apiTokenSuperuser);
        
        String relationTypeJson2 = Json.createObjectBuilder()
                .add("name", "isSupplementTo")
                .add("displayName", "Is supplement to")
                .add("inverseName", "isSupplementedBy")
                .add("inverseDisplayName", "Is supplemented by")
                .build().toString();
        UtilIT.addDatasetRelationType(relationTypeJson2, apiTokenSuperuser);

        String relationTypeJson3 = Json.createObjectBuilder()
                .add("name", "isCitedBy")
                .add("displayName", "Is cited by")
                .add("inverseName", "cites")
                .add("inverseDisplayName", "Cites")
                .build().toString();
        UtilIT.addDatasetRelationType(relationTypeJson3, apiTokenSuperuser);
    }

    @Test
    public void testDatasetRelationDeduplication() {
        // Create Dataset A
        String dataverseAlias = UtilIT.createRandomCollectionGetAlias(apiTokenSuperuser);
        UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());
        Response createDatasetA = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser);
        String pidA = UtilIT.getDatasetPersistentIdFromResponse(createDatasetA);
        
        // Create Dataset B
        Response createDatasetB = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser);
        String pidB = UtilIT.getDatasetPersistentIdFromResponse(createDatasetB);
        UtilIT.publishDatasetViaNativeApi(pidB, "major", apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());

        // Define relation A (draft) -> B
        JsonArray relations = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add("relatedDatasetPid", pidB)
                        .add("relationTypeName", "isCitedBy"))
                .build();
        UtilIT.replaceDatasetRelations(pidA, relations.toString(), apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());

        // Publish A v1.0
        UtilIT.publishDatasetViaNativeApi(pidA, "major", apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());

        // Define the SAME relation in inverse direction: B (draft) -> A
        JsonArray relationsInverse = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add("relatedDatasetPid", pidA)
                        .add("relationTypeName", "cites"))
                .build();
        UtilIT.replaceDatasetRelations(pidB, relationsInverse.toString(), apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());

        // Publish A v1.0
        UtilIT.publishDatasetViaNativeApi(pidB, "major", apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());

        // Create A v2 (draft) and redefine the SAME relation plus one more
        String externalUrl = "https://example.org/dataset/12345";
        JsonArray relationsNew = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add("relatedDatasetPid", pidB)
                        .add("relationTypeName", "isCitedBy"))
                .add(Json.createObjectBuilder()
                        .add("externalIdentifier", externalUrl)
                        .add("identifierScheme", "URL")
                        .add("relationTypeName", "isRelatedTo"))
                .build();
        UtilIT.replaceDatasetRelations(pidA, relationsNew.toString(), apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());

        // The relation "A is cited by B" is now present 3 times: in A v1, B v1, A draft
        // Plus the additional relation "A is related to https://example.org/dataset/12345"

        // Verify only those two relations are listed for A (draft) (no duplicates)
        UtilIT.listDatasetRelations(pidA, ":draft", null, null, null, null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(2))
                .body("data", hasSize(2))
                .body("data.relatedDatasetPid", hasItem(pidB))
                .body("data.externalIdentifier", hasItem(externalUrl));

        // Also verify counts (grouped by relation type by default)
        UtilIT.getDatasetRelationCounts(pidA, ":draft", null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("data", hasSize(2))
                .body("data[0].relationType.name", equalTo("isCitedBy"))
                .body("data[0].count", equalTo(1))
                .body("data[1].relationType.name", equalTo("isRelatedTo"))
                .body("data[1].count", equalTo(1));
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
        UtilIT.listDatasetRelations(pidB, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(1))
                .body("data", hasSize(1))
                .body("data[0].relatedDatasetPid", equalTo(pidA))
                .body("data[0].relationType.name", equalTo("isRelatedTo"));

        // Verify relation is not listed when requesting relations for Dataset A (v1)
        // since Dataset B, where the relation was defined, is still in Draft status
        UtilIT.listDatasetRelations(pidA, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(0))
                .body("data", hasSize(0));

        // Publish Dataset B (=> v1.0)
        UtilIT.publishDatasetViaNativeApi(pidB, "major", apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());

        // Verify relation is now listed when requesting relations for Dataset A
        UtilIT.listDatasetRelations(pidA, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(1))
                .body("data", hasSize(1))
                .body("data[0].relatedDatasetPid", equalTo(pidB))
                .body("data[0].relationType.name", equalTo("isRelatedTo"));

        // Verify relation is listed when requesting relations for Dataset B v1.0
        UtilIT.listDatasetRelations(pidB, "1.0", null, null, null, null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(1))
                .body("data", hasSize(1))
                .body("data[0].relatedDatasetPid", equalTo(pidA))
                .body("data[0].relationType.name", equalTo("isRelatedTo"));

        // Remove the relation in new version of Dataset B (draft)
        UtilIT.replaceDatasetRelations(pidB, "[]", apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());

        // Verify relation is still listed when requesting relations for Dataset A
        // Because Dataset B is still in draft mode
        UtilIT.listDatasetRelations(pidA, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(1))
                .body("data", hasSize(1))
                .body("data[0].relatedDatasetPid", equalTo(pidB))
                .body("data[0].relationType.name", equalTo("isRelatedTo"));

        // Verify relation is still listed when requesting relations for Dataset B without a token
        // Because without a token, users cannot see Dataset B's draft
        UtilIT.listDatasetRelations(pidB)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(1))
                .body("data", hasSize(1))
                .body("data[0].relatedDatasetPid", equalTo(pidA))
                .body("data[0].relationType.name", equalTo("isRelatedTo"));

        // Verify relation is not listed when requesting relations for Dataset B's draft specifically
        UtilIT.listDatasetRelations(pidB, ":draft", null, null, null, null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(0))
                .body("data", hasSize(0));

        // Publish Dataset B (=> v2.0)
        UtilIT.publishDatasetViaNativeApi(pidB, "major", apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());

        // Verify relation is NO LONGER listed when requesting relations for Dataset A
        // Because Dataset B is now at v2 and v2 has no relation
        UtilIT.listDatasetRelations(pidA, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(0))
                .body("data", hasSize(0));

        // Verify relation is STILL listed when requesting relations for Dataset B v1 specifically
        UtilIT.listDatasetRelations(pidB, "1.0", null, null, null, null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(1))
                .body("data", hasSize(1))
                .body("data[0].relatedDatasetPid", equalTo(pidA))
                .body("data[0].relationType.name", equalTo("isRelatedTo"));

        // Verify relation is NOT listed when requesting relations for Dataset B v2
        UtilIT.listDatasetRelations(pidB, "2.0", null, null, null, null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(0))
                .body("data", hasSize(0));

        // Verify relation is NOT listed when requesting relations for Dataset B
        UtilIT.listDatasetRelations(pidB, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(0))
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
        UtilIT.listDatasetRelations(pidA, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(1))
                .body("data", hasSize(1))
                .body("data[0].externalIdentifier", equalTo(externalUrl))
                .body("data[0].identifierScheme", equalTo("URL"))
                .body("data[0].relationType.name", equalTo("isRelatedTo"));

        // Add external relation with datasetType
        String externalUrlWithDocType = "https://example.org/dataset/67890";
        JsonArray relationsWithDocType = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add("externalIdentifier", externalUrlWithDocType)
                        .add("identifierScheme", "URL")
                        .add("datasetType", "Document")
                        .add("relationTypeName", "isRelatedTo"))
                .build();

        UtilIT.replaceDatasetRelations(pidA, relationsWithDocType.toString(), apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());

        // Verify external relation with datasetType is listed
        UtilIT.listDatasetRelations(pidA, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(1))
                .body("data", hasSize(1))
                .body("data[0].externalIdentifier", equalTo(externalUrlWithDocType))
                .body("data[0].relatedDatasetType.displayName", equalTo("Document"));

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

        UtilIT.listDatasetRelations(pidA, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(2))
                .body("data", hasSize(2))
                .body("data.relatedDatasetPid", hasItem(pidB))
                .body("data.externalIdentifier", hasItem("doi:10.1234/5678"))
                .body("data.identifierScheme", hasItem("DOI"))
                .body("data.relatedDatasetType.name", hasItem("dataset"))
                .body("data.relatedDatasetType.displayName", hasItem("Dataset"));
    }

    @Test
    public void testListDatasetRelationsFilteringByRelationTypeAndVersion() {
        String dataverseAlias = UtilIT.createRandomCollectionGetAlias(apiTokenSuperuser);
        UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());

        // Dataset A - will have relations in multiple versions
        Response createDatasetA = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser);
        String pidA = UtilIT.getDatasetPersistentIdFromResponse(createDatasetA);

        // Dataset B
        Response createDatasetB = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser);
        String pidB = UtilIT.getDatasetPersistentIdFromResponse(createDatasetB);

        // Dataset C
        Response createDatasetC = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser);
        String pidC = UtilIT.getDatasetPersistentIdFromResponse(createDatasetC);

        // Version 1.0 of Dataset A:
        // Relation 1: A -> B (isRelatedTo)
        JsonArray relationsV1 = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add("relatedDatasetPid", pidB)
                        .add("relationTypeName", "isRelatedTo"))
                .build();
        UtilIT.replaceDatasetRelations(pidA, relationsV1.toString(), apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());
        UtilIT.publishDatasetViaNativeApi(pidA, "major", apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());

        // Version 2.0 of Dataset A:
        // Relation 1: A -> B (isRelatedTo) - kept from V1
        // Relation 2: A -> C (isSupplementTo)
        JsonArray relationsV2 = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add("relatedDatasetPid", pidB)
                        .add("relationTypeName", "isRelatedTo"))
                .add(Json.createObjectBuilder()
                        .add("relatedDatasetPid", pidC)
                        .add("relationTypeName", "isSupplementTo"))
                .build();
        // This will create a draft for v2.0
        UtilIT.replaceDatasetRelations(pidA, relationsV2.toString(), apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());
        // Publish v2.0
        UtilIT.publishDatasetViaNativeApi(pidA, "major", apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());

        // Current state:
        // V1.0: 1 relation (isRelatedTo)
        // v2.0: 2 relations (isRelatedTo, isSupplementTo)

        // 1. Filter by Version AND Type (V1.0, isRelatedTo) -> Expect 1
        UtilIT.listDatasetRelations(pidA, "1.0", List.of("isRelatedTo"), null, null, null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(1));

        // 2. Filter by Version AND Type (V1.0, isSupplementTo) -> Expect 0
        UtilIT.listDatasetRelations(pidA, "1.0", List.of("isSupplementTo"), null, null, null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(0));

        // 3. Filter by Version AND Type (v2.0, isSupplementTo) -> Expect 1
        UtilIT.listDatasetRelations(pidA, "2.0", List.of("isSupplementTo"), null, null, null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(1));

        // 4. Filter by Version only (V1.0) -> Expect 1
        UtilIT.listDatasetRelations(pidA, "1.0", null, null, null, null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(1));

        // 5. Filter by Version only (v2.0) -> Expect 2
        UtilIT.listDatasetRelations(pidA, "2.0", null, null, null, null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(2));

        // 6. Filter by Type only (isSupplementTo) -> Expect 1 (from latest published v2.0)
        UtilIT.listDatasetRelations(pidA, null, List.of("isSupplementTo"), null, null, null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(1));

        // 7. Filter by Type only (isRelatedTo) -> Expect 1 (from latest published v2.0)
        UtilIT.listDatasetRelations(pidA, null, List.of("isRelatedTo"), null, null, null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(1));

        // 8. Filter by Types only (isRelatedTo, isSupplementTo) -> Expect 2 (from latest published v2.0)
        UtilIT.listDatasetRelations(pidA, null, Arrays.asList("isRelatedTo", "isSupplementTo"), null, null, null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(2));

        // 9. No filters (latest version) -> Expect 2
        UtilIT.listDatasetRelations(pidA, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(2));
    }

    @Test
    public void testListDatasetRelationsFilteringByDatasetType() {
        String dataverseAlias = UtilIT.createRandomCollectionGetAlias(apiTokenSuperuser);
        UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());

        // Ensure 'software' type exists in the system
        Response getDatasetType = UtilIT.getDatasetType("software");
        String typeFound = JsonPath.from(getDatasetType.getBody().asString()).getString("data.name");
        if (!("software".equals(typeFound))) {
            JsonObject softwareTypeJson = Json.createObjectBuilder()
                    .add("name", "software")
                    .add("displayName", "Software")
                    .add("description", "Software Dataset Type")
                    .build();
            UtilIT.addDatasetType(softwareTypeJson.toString(), apiTokenSuperuser);
        }

        // We need to ensure 'software' type is allowed in this collection
        Response setAllowed = UtilIT.setCollectionAttribute(dataverseAlias, "allowedDatasetTypes", "dataset,software", apiTokenSuperuser);
        setAllowed.then().assertThat().statusCode(OK.getStatusCode());

        // Dataset A (Source) - type 'dataset' (default)
        Response createDatasetA = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser);
        String pidA = UtilIT.getDatasetPersistentIdFromResponse(createDatasetA);

        // Dataset B (Target 1) - type 'dataset' (default)
        Response createDatasetB = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser);
        String pidB = UtilIT.getDatasetPersistentIdFromResponse(createDatasetB);

        // Dataset C (Target 2) - type 'software'
        String softwareJson = UtilIT.getDatasetJson("doc/sphinx-guides/source/_static/api/dataset-create-software.json");
        Response createDatasetC = UtilIT.createDataset(dataverseAlias, softwareJson, apiTokenSuperuser);
        createDatasetC.then().assertThat().statusCode(201);
        String pidC = UtilIT.getDatasetPersistentIdFromResponse(createDatasetC);

        // Create relations: A -> B (isRelatedTo), A -> C (isRelatedTo), A -> External (isRelatedTo)
        JsonArray relations = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add("relatedDatasetPid", pidB)
                        .add("relationTypeName", "isRelatedTo"))
                .add(Json.createObjectBuilder()
                        .add("relatedDatasetPid", pidC)
                        .add("relationTypeName", "isRelatedTo"))
                .add(Json.createObjectBuilder()
                        .add("externalIdentifier", "doi:10.1234/external")
                        .add("identifierScheme", "DOI")
                        .add("relationTypeName", "isRelatedTo")
                        .add("relationSource", "external"))
                .build();
        UtilIT.replaceDatasetRelations(pidA, relations.toString(), apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());
        UtilIT.publishDatasetViaNativeApi(pidA, "major", apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());

        // No type filter -> Expect 3
        UtilIT.listDatasetRelations(pidA, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(3));

        // Filter by datasetType=dataset -> Expect 1 (Dataset B)
        UtilIT.listDatasetRelations(pidA, null, null, Arrays.asList("dataset"), null, null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(1))
                .body("data[0].relatedDatasetPid", equalTo(pidB));

        // Filter by datasetType=software -> Expect 1 (Dataset C)
        UtilIT.listDatasetRelations(pidA, null, null, Arrays.asList("software"), null, null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(1))
                .body("data[0].relatedDatasetPid", equalTo(pidC));

        // Filter by both -> Expect 2 (B and C)
        UtilIT.listDatasetRelations(pidA, null, null, Arrays.asList("dataset", "software"), null, null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(2))
                .body("data.relatedDatasetPid", hasItems(pidB, pidC));

        // Filter by a non-existent type -> Expect 0
        UtilIT.listDatasetRelations(pidA, null, null, Arrays.asList("workflow"), null, null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(0));

        // Also check counts grouped by dataset type
        UtilIT.getDatasetRelationCounts(pidA, null, "datasetType", apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("data", hasSize(2))
                .body("data[0].datasetType.name", equalTo("dataset"))
                .body("data[0].count", equalTo(1))
                .body("data[1].datasetType.name", equalTo("software"))
                .body("data[1].count", equalTo(1));
    }

    @Test
    public void testDatasetRelationCounts() {
        String dataverseAlias = UtilIT.createRandomCollectionGetAlias(apiTokenSuperuser);
        UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());

        // Create main Dataset A
        Response createDatasetA = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser);
        String pidA = UtilIT.getDatasetPersistentIdFromResponse(createDatasetA);

        // Create other datasets to relate to
        String[] pids = new String[7];
        for (int i = 0; i < 7; i++) {
            Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser);
            pids[i] = UtilIT.getDatasetPersistentIdFromResponse(createDataset);
        }

        // Setup relations:
        // isCitedBy: 3 relations
        // isRelatedTo: 2 relations
        // isSupplementTo: 2 relations
        // (Alphabetical: isCitedBy < isRelatedTo < isSupplementTo)
        // Expected order:
        // 1. isCitedBy (count: 3)
        // 2. isRelatedTo (count: 2)
        // 3. isSupplementTo (count: 2)

        JsonArray relations = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("relatedDatasetPid", pids[0]).add("relationTypeName", "isCitedBy"))
                .add(Json.createObjectBuilder().add("relatedDatasetPid", pids[1]).add("relationTypeName", "isCitedBy"))
                .add(Json.createObjectBuilder().add("relatedDatasetPid", pids[2]).add("relationTypeName", "isCitedBy"))
                .add(Json.createObjectBuilder().add("relatedDatasetPid", pids[3]).add("relationTypeName", "isRelatedTo"))
                .add(Json.createObjectBuilder().add("relatedDatasetPid", pids[4]).add("relationTypeName", "isRelatedTo"))
                .add(Json.createObjectBuilder().add("relatedDatasetPid", pids[5]).add("relationTypeName", "isSupplementTo"))
                .add(Json.createObjectBuilder().add("relatedDatasetPid", pids[6]).add("relationTypeName", "isSupplementTo"))
                .build();

        UtilIT.replaceDatasetRelations(pidA, relations.toString(), apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());

        // Check counts and sorting
        UtilIT.getDatasetRelationCounts(pidA, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("data", hasSize(3))
                // 1st: isCitedBy (count 3)
                .body("data[0].relationType.name", equalTo("isCitedBy"))
                .body("data[0].count", equalTo(3))
                // 2nd: isRelatedTo (count 2)
                .body("data[1].relationType.name", equalTo("isRelatedTo"))
                .body("data[1].count", equalTo(2))
                // 3rd: isSupplementTo (count 2)
                .body("data[2].relationType.name", equalTo("isSupplementTo"))
                .body("data[2].count", equalTo(2));
    }

    @Test
    public void testDatasetRelationsOrdering() {
        String dataverseAlias = UtilIT.createRandomCollectionGetAlias(apiTokenSuperuser);
        UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());

        // Create Dataset A
        Response createDatasetA = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser);
        String pidA = UtilIT.getDatasetPersistentIdFromResponse(createDatasetA);

        // Create Dataset B
        Response createDatasetB = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser);
        String pidB = UtilIT.getDatasetPersistentIdFromResponse(createDatasetB);

        // Create Dataset C
        Response createDatasetC = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser);
        String pidC = UtilIT.getDatasetPersistentIdFromResponse(createDatasetC);

        // Define relation B -> A (defined on B)
        JsonArray relationsB = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add("relatedDatasetPid", pidA)
                        .add("relationTypeName", "isRelatedTo"))
                .build();
        UtilIT.replaceDatasetRelations(pidB, relationsB.toString(), apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());
        UtilIT.publishDatasetViaNativeApi(pidB, "major", apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());

        // Define relation A -> C (defined on A)
        JsonArray relationsA = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add("relatedDatasetPid", pidC)
                        .add("relationTypeName", "isRelatedTo"))
                .build();
        UtilIT.replaceDatasetRelations(pidA, relationsA.toString(), apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());

        // List relations for A
        // Expecting:
        // 1. A -> C (defined on A)
        // 2. B -> A (defined on B)
        
        Response listResponse = UtilIT.listDatasetRelations(pidA, apiTokenSuperuser);
        listResponse.then().assertThat().statusCode(OK.getStatusCode())
                .body("data", hasSize(2));

        // We want relations defined ON dataset A to come first:
        // Relation A -> C is defined on A (definitionPointPid should be pidA)
        // Relation B -> A is defined on B (definitionPointPid should be pidB)

        List<String> definitionPointPids = listResponse.jsonPath().getList("data.definitionPointPid");
        assertEquals(pidA, definitionPointPids.get(0));
        assertEquals(pidB, definitionPointPids.get(1));
    }

    @Test
    public void testListDatasetRelationsFilteringBySource() {
        String dataverseAlias = UtilIT.createRandomCollectionGetAlias(apiTokenSuperuser);
        UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());

        // Dataset A
        Response createDatasetA = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser);
        String pidA = UtilIT.getDatasetPersistentIdFromResponse(createDatasetA);

        // Dataset B
        Response createDatasetB = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiTokenSuperuser);
        String pidB = UtilIT.getDatasetPersistentIdFromResponse(createDatasetB);

        // Create 1 internal and 1 external relation
        JsonArray relations = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add("relatedDatasetPid", pidB)
                        .add("relationTypeName", "isRelatedTo"))
                .add(Json.createObjectBuilder()
                        .add("externalIdentifier", "https://example.org/1")
                        .add("identifierScheme", "URL")
                        .add("relationTypeName", "isRelatedTo"))
                .build();
        UtilIT.replaceDatasetRelations(pidA, relations.toString(), apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());

        // No filter -> Expect 2
        UtilIT.listDatasetRelations(pidA, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(2));

        // Filter by source=internal -> Expect 1
        UtilIT.listDatasetRelations(pidA, null, null, null, List.of("internal"), null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(1))
                .body("data[0].relatedDatasetPid", equalTo(pidB));

        // Filter by source=external -> Expect 1
        UtilIT.listDatasetRelations(pidA, null, null, null, List.of("external"), null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(1))
                .body("data[0].externalIdentifier", equalTo("https://example.org/1"));

        // Filter by both -> Expect 2
        UtilIT.listDatasetRelations(pidA, null, null, null, Arrays.asList("internal", "external"), null, null, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("totalCount", equalTo(2));
    }

    @Test
    public void testDatasetRelationsViaVersionApis() {
        String dataverseAlias = UtilIT.createRandomCollectionGetAlias(apiTokenSuperuser);
        UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiTokenSuperuser).then().assertThat().statusCode(OK.getStatusCode());

        // Create dataset with relations via POST api/dataverses/%s/datasets
        String externalUrl1 = "https://example.org/dataset/1";
        JsonObject datasetData = Json.createObjectBuilder()
                .add("datasetVersion", Json.createObjectBuilder()
                        .add("metadataBlocks", Json.createObjectBuilder()
                                .add("citation", Json.createObjectBuilder()
                                        .add("fields", Json.createArrayBuilder()
                                                .add(Json.createObjectBuilder()
                                                        .add("typeName", "title")
                                                        .add("multiple", false)
                                                        .add("typeClass", "primitive")
                                                        .add("value", "Dataset with Relations"))
                                                .add(Json.createObjectBuilder()
                                                        .add("typeName", "author")
                                                        .add("multiple", true)
                                                        .add("typeClass", "compound")
                                                        .add("value", Json.createArrayBuilder()
                                                                .add(Json.createObjectBuilder()
                                                                        .add("authorName", Json.createObjectBuilder()
                                                                                .add("typeName", "authorName")
                                                                                .add("multiple", false)
                                                                                .add("typeClass", "primitive")
                                                                                .add("value", "Lastname, Firstname")))))
                                                .add(Json.createObjectBuilder()
                                                        .add("typeName", "datasetContact")
                                                        .add("multiple", true)
                                                        .add("typeClass", "compound")
                                                        .add("value", Json.createArrayBuilder()
                                                                .add(Json.createObjectBuilder()
                                                                        .add("datasetContactEmail", Json.createObjectBuilder()
                                                                                .add("typeName", "datasetContactEmail")
                                                                                .add("multiple", false)
                                                                                .add("typeClass", "primitive")
                                                                                .add("value", "test@example.edu")))))
                                                .add(Json.createObjectBuilder()
                                                        .add("typeName", "dsDescription")
                                                        .add("multiple", true)
                                                        .add("typeClass", "compound")
                                                        .add("value", Json.createArrayBuilder()
                                                                .add(Json.createObjectBuilder()
                                                                        .add("dsDescriptionValue", Json.createObjectBuilder()
                                                                                .add("typeName", "dsDescriptionValue")
                                                                                .add("multiple", false)
                                                                                .add("typeClass", "primitive")
                                                                                .add("value", "Description text")))))
                                                .add(Json.createObjectBuilder()
                                                        .add("typeName", "subject")
                                                        .add("multiple", true)
                                                        .add("typeClass", "controlledVocabulary")
                                                        .add("value", Json.createArrayBuilder().add("Agricultural Sciences"))))))
                        .add("relations", Json.createArrayBuilder()
                                .add(Json.createObjectBuilder()
                                        .add("externalIdentifier", externalUrl1)
                                        .add("identifierScheme", "URL")
                                        .add("relationTypeName", "isRelatedTo"))))
                .build();

        Response createDataset = UtilIT.createDataset(dataverseAlias, datasetData.toString(), apiTokenSuperuser);
        createDataset.then().assertThat().statusCode(CREATED.getStatusCode());
        String pid = UtilIT.getDatasetPersistentIdFromResponse(createDataset);

        // Read dataset and verify relations are present via GET api/datasets/:persistentId/versions/:draft
        UtilIT.getDatasetVersion(pid, ":draft", apiTokenSuperuser, false, false, false, false)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("data.relations", hasSize(1))
                .body("data.relations[0].externalIdentifier", equalTo(externalUrl1))
                .body("data.relations[0].relationType.name", equalTo("isRelatedTo"));

        // Update dataset with NEW relations via PUT api/datasets/:persistentId/versions/:draft
        String externalUrl2 = "https://example.org/dataset/2";
        
        // Reuse the existing data and replace relations
        JsonObject updatedVersionData = Json.createObjectBuilder(datasetData.getJsonObject("datasetVersion"))
                .add("relations", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("externalIdentifier", externalUrl2)
                                .add("identifierScheme", "URL")
                                .add("relationTypeName", "isSupplementTo")))
                .build();

        UtilIT.updateDatasetMetadataViaNative(pid, updatedVersionData, apiTokenSuperuser)
                .then().assertThat().statusCode(OK.getStatusCode());

        // Read again and verify relations are updated
        UtilIT.getDatasetVersion(pid, ":draft", apiTokenSuperuser, false, false, false, false)
                .then().assertThat().statusCode(OK.getStatusCode())
                .body("data.relations", hasSize(1))
                .body("data.relations[0].externalIdentifier", equalTo(externalUrl2))
                .body("data.relations[0].relationType.name", equalTo("isSupplementTo"));
    }
}
