package com.ax.template.authblueprint.practices;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Black-box HTTP integration test for PRACTICES-PERS-005: soft-delete via @SQLDelete + @Where.
 *
 * <p>Verifies that:
 * <ol>
 *   <li>A newly created record appears in GET /soft-deleted-records</li>
 *   <li>After DELETE, the record does NOT appear in GET (filtered by @Where)</li>
 *   <li>Multiple records: only active ones are returned after selective soft-delete</li>
 * </ol>
 *
 * <p>Uses RestAssured (black-box HTTP) per PRACTICES-TEST-001.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Tag("PRACTICES")
@Tag("PRACTICES-PERS-005")
class BaseEntitySoftDeleteIT {

    @LocalServerPort
    private int port;

    @Autowired
    private SoftDeletedRecordRepository repository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        repository.deleteAllInBatch();
    }

    @Test
    void practices_PERS_005_newRecordIsVisibleInList() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("label", "active-record"))
                .when()
                .post("/practices/demo/soft-deleted-records")
                .then()
                .statusCode(201);

        Response r = given().when().get("/practices/demo/soft-deleted-records")
                .then().statusCode(200).extract().response();
        List<Map<String, Object>> body = r.jsonPath().getList("$");
        assertThat(body)
                .as("Newly created record must appear in the active list")
                .hasSize(1);
        assertThat(body.get(0).get("label")).isEqualTo("active-record");
    }

    @Test
    void practices_PERS_005_softDeletedRecordDisappearsFromList() {
        Response createResponse = given()
                .contentType(ContentType.JSON)
                .body(Map.of("label", "to-be-deleted"))
                .when()
                .post("/practices/demo/soft-deleted-records")
                .then()
                .statusCode(201)
                .extract().response();

        String id = createResponse.jsonPath().getString("id");
        assertThat(id).isNotBlank();

        List<Map<String, Object>> before = given().when()
                .get("/practices/demo/soft-deleted-records")
                .then().statusCode(200).extract().jsonPath().getList("$");
        assertThat(before).hasSize(1);

        given().when()
                .delete("/practices/demo/soft-deleted-records/" + id)
                .then()
                .statusCode(204);

        List<Map<String, Object>> after = given().when()
                .get("/practices/demo/soft-deleted-records")
                .then().statusCode(200).extract().jsonPath().getList("$");
        assertThat(after)
                .as("Soft-deleted record must be excluded by @Where filter")
                .isEmpty();
    }

    @Test
    void practices_PERS_005_softDeletePreservesOtherRecords() {
        String id1 = createRecord("keep-1");
        String id2 = createRecord("delete-me");
        createRecord("keep-2");

        given().when()
                .delete("/practices/demo/soft-deleted-records/" + id2)
                .then()
                .statusCode(204);

        List<Map<String, Object>> active = given().when()
                .get("/practices/demo/soft-deleted-records")
                .then().statusCode(200).extract().jsonPath().getList("$");
        assertThat(active)
                .as("Only non-deleted records should be returned")
                .hasSize(2);

        List<String> labels = active.stream()
                .map(m -> (String) m.get("label"))
                .toList();
        assertThat(labels).containsExactlyInAnyOrder("keep-1", "keep-2");
        assertThat(labels).doesNotContain("delete-me");
    }

    @Test
    void practices_PERS_005_createdByIsPopulatedWhenPrincipalIsSet() {
        // Arrange: set an authenticated principal in the SecurityContext so that
        // AuditorAware<String> can populate @CreatedBy on persist.
        var auth = new UsernamePasswordAuthenticationToken(
                "test-user", null,
                List.of(new SimpleGrantedAuthority("ROLE_MEMBER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            // Act: save a record while the SecurityContext has a principal
            var record = new SoftDeletedRecord();
            record.setLabel("audit-check");
            var saved = repository.save(record);
            // Reload from DB to confirm the persisted value
            var reloaded = repository.findById(saved.getId()).orElseThrow();

            // Assert: @CreatedBy was populated from the SecurityContext principal
            assertThat(reloaded.getCreatedBy())
                    .as("@CreatedBy must be set to the authenticated principal name after persist")
                    .isEqualTo("test-user");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private String createRecord(String label) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("label", label))
                .when()
                .post("/practices/demo/soft-deleted-records")
                .then()
                .statusCode(201)
                .extract().jsonPath().getString("id");
    }
}
