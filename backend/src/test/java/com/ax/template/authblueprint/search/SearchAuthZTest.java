package com.ax.template.authblueprint.search;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * AUTHZ family — SEARCH-AUTHZ-001, SEARCH-AUTHZ-002.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SearchAuthZTest {

    @LocalServerPort int port;

    @Autowired SearchIndexDocumentRepository repository;

    @BeforeEach
    void setup() {
        SearchTestSupport.useRandomPort(port);
        repository.deleteAll();
    }

    @Test
    @Tag("search")
    @Tag("SEARCH-AUTHZ-001")
    void authz_001_unauthenticatedRequestsAre401() {
        given().contentType(ContentType.JSON).body("{\"query\":\"hi\"}")
            .when().post("/api/v1/search")
            .then().statusCode(401);

        given().contentType(ContentType.JSON)
            .body("{\"content\":\"hello\"}")
            .when().post("/api/v1/search/index")
            .then().statusCode(401);

        given()
            .when().delete("/api/v1/search/index/" + UUID.randomUUID())
            .then().statusCode(401);
    }

    @Test
    @Tag("search")
    @Tag("SEARCH-AUTHZ-002")
    void authz_002_crossTenantResultsAreEmpty() {
        String tokenA = SearchTestSupport.obtainToken(
            SearchTestSupport.freshEmail("search-authz-a"), "MEMBER");
        String tokenB = SearchTestSupport.obtainToken(
            SearchTestSupport.freshEmail("search-authz-b"), "MEMBER");

        // Tenant A indexes a unique document.
        String uniqueWord = "alpha" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        given()
            .header("Authorization", "Bearer " + tokenA)
            .contentType(ContentType.JSON)
            .body("{\"content\":\"tenantA secret " + uniqueWord + "\"}")
        .when().post("/api/v1/search/index")
        .then().statusCode(201);

        // Tenant B searches for the same word → MUST return zero hits.
        Integer totalHits = given()
            .header("Authorization", "Bearer " + tokenB)
            .contentType(ContentType.JSON)
            .body("{\"query\":\"" + uniqueWord + "\"}")
        .when().post("/api/v1/search")
        .then().statusCode(200)
            .extract().path("totalHits");

        assertThat(totalHits)
            .as("SEARCH-AUTHZ-002 — cross-tenant search must return 0 hits")
            .isEqualTo(0);

        // Sanity: tenant A finds its own document.
        Integer ownHits = given()
            .header("Authorization", "Bearer " + tokenA)
            .contentType(ContentType.JSON)
            .body("{\"query\":\"" + uniqueWord + "\"}")
        .when().post("/api/v1/search")
        .then().statusCode(200)
            .extract().path("totalHits");
        assertThat(ownHits).isEqualTo(1);
    }
}
