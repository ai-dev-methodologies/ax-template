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
 * INDEX family — SEARCH-INDEX-001 (index makes it findable),
 * SEARCH-INDEX-002 (delete removes from results).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SearchIndexTest {

    @LocalServerPort int port;

    @Autowired SearchIndexDocumentRepository repository;

    @BeforeEach
    void setup() {
        SearchTestSupport.useRandomPort(port);
        repository.deleteAll();
    }

    @Test
    @Tag("SEARCH")
    @Tag("SEARCH-INDEX-001")
    void index_001_indexedDocumentIsFindable() {
        String token = SearchTestSupport.obtainToken(
            SearchTestSupport.freshEmail("search-index-001"), "MEMBER");

        String uniqueWord = "needle" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        String indexedId = given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("{\"content\":\"haystack of words including " + uniqueWord + " hidden in there\"}")
        .when().post("/api/v1/search/index")
        .then().statusCode(201)
            .extract().path("id");

        assertThat(indexedId).isNotBlank();

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("{\"query\":\"" + uniqueWord + "\"}")
        .when().post("/api/v1/search")
        .then().statusCode(200)
            .body("totalHits", org.hamcrest.Matchers.greaterThanOrEqualTo(1))
            .body("hits[0].id", org.hamcrest.Matchers.equalTo(indexedId));
    }

    @Test
    @Tag("SEARCH")
    @Tag("SEARCH-INDEX-002")
    void index_002_deletedDocumentIsNotFindable() {
        String token = SearchTestSupport.obtainToken(
            SearchTestSupport.freshEmail("search-index-002"), "MEMBER");

        String uniqueWord = "ephemeral" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        String indexedId = given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("{\"content\":\"this is " + uniqueWord + " content for deletion test\"}")
        .when().post("/api/v1/search/index")
        .then().statusCode(201)
            .extract().path("id");

        // Confirm it's findable before delete.
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("{\"query\":\"" + uniqueWord + "\"}")
        .when().post("/api/v1/search")
        .then().statusCode(200)
            .body("totalHits", org.hamcrest.Matchers.equalTo(1));

        // Delete the document.
        given()
            .header("Authorization", "Bearer " + token)
        .when().delete("/api/v1/search/index/" + indexedId)
        .then().statusCode(204);

        // Confirm not findable after delete.
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("{\"query\":\"" + uniqueWord + "\"}")
        .when().post("/api/v1/search")
        .then().statusCode(200)
            .body("totalHits", org.hamcrest.Matchers.equalTo(0));
    }
}
