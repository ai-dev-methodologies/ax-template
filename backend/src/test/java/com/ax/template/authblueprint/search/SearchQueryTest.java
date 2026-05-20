package com.ax.template.authblueprint.search;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

/**
 * QUERY family — SEARCH-QUERY-001 (paginated result page),
 * SEARCH-QUERY-002 (Korean), SEARCH-QUERY-003 (blank → 400 RFC 7807).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SearchQueryTest {

    @LocalServerPort int port;

    @Autowired SearchIndexDocumentRepository repository;

    @BeforeEach
    void setup() {
        SearchTestSupport.useRandomPort(port);
        repository.deleteAll();
    }

    @Test
    @Tag("search")
    @Tag("SEARCH-QUERY-001")
    void query_001_returnsPaginatedResultPage() {
        String token = SearchTestSupport.obtainToken(
            SearchTestSupport.freshEmail("search-query-001"), "MEMBER");

        for (int i = 0; i < 3; i++) {
            given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("{\"content\":\"hello world doc " + i + "\"}")
            .when().post("/api/v1/search/index")
            .then().statusCode(201);
        }

        // Search returns the result page with the required fields.
        Long totalHits = given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("{\"query\":\"hello\",\"page\":0,\"size\":20}")
        .when().post("/api/v1/search")
        .then().statusCode(200)
            .body("hits.size()", org.hamcrest.Matchers.greaterThanOrEqualTo(1))
            .body("page", org.hamcrest.Matchers.equalTo(0))
            .body("size", org.hamcrest.Matchers.equalTo(20))
            .body("processingTimeMs", org.hamcrest.Matchers.notNullValue())
            .extract().jsonPath().getLong("totalHits");

        assertThat(totalHits).isGreaterThanOrEqualTo(3L);
    }

    @Test
    @Tag("search")
    @Tag("SEARCH-QUERY-002")
    void query_002_koreanSubstringMatches() {
        String token = SearchTestSupport.obtainToken(
            SearchTestSupport.freshEmail("search-query-002"), "MEMBER");

        // Korean content — '강남역 결제 완료'.
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("{\"content\":\"강남역 결제 완료\"}")
        .when().post("/api/v1/search/index")
        .then().statusCode(201);

        // Korean query containing substrings of the indexed content.
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("{\"query\":\"강남\"}")
        .when().post("/api/v1/search")
        .then().statusCode(200)
            .body("totalHits", org.hamcrest.Matchers.greaterThanOrEqualTo(1));

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("{\"query\":\"결제\"}")
        .when().post("/api/v1/search")
        .then().statusCode(200)
            .body("totalHits", org.hamcrest.Matchers.greaterThanOrEqualTo(1));
    }

    @Test
    @Tag("search")
    @Tag("SEARCH-QUERY-003")
    void query_003_blankQueryReturns400WithProblemDetail() {
        String token = SearchTestSupport.obtainToken(
            SearchTestSupport.freshEmail("search-query-003"), "MEMBER");

        // Empty string → 400.
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("{\"query\":\"\"}")
        .when().post("/api/v1/search")
        .then().statusCode(400)
            .body("type", containsString("validation-error"));

        // Whitespace-only → 400.
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("{\"query\":\"   \"}")
        .when().post("/api/v1/search")
        .then().statusCode(400)
            .body("type", containsString("validation-error"));
    }
}
