package com.ax.template.authblueprint.search;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.util.ArrayList;
import java.util.List;

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
    @Tag("SEARCH")
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
    @Tag("SEARCH")
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
    @Tag("SEARCH")
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

    /**
     * SEARCH-RANK-001 — deterministic, page-stable ordering: relevance DESC with id as the
     * MANDATORY tiebreaker. Indexing 5 documents with IDENTICAL content gives every hit the
     * same ts_rank for query "hello", so ONLY the id tiebreaker establishes a total order. The
     * union of hits across page boundaries must contain every matching id exactly once (no
     * duplicate, no gap), and an identical repeated request must return a byte-identical order.
     * This is the search-domain realization of pagination-l0 PAGE-STABLE-SORT-001.
     */
    @Test
    @Tag("SEARCH")
    @Tag("SEARCH-RANK-001")
    void rank_001_tiedRelevanceIsPageStableViaIdTiebreaker() {
        String token = SearchTestSupport.obtainToken(
            SearchTestSupport.freshEmail("search-rank-001"), "MEMBER");

        // 5 documents, identical content → identical relevance score for query "hello"
        List<String> indexed = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            indexed.add(given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("{\"content\":\"hello hello hello\"}")
            .when().post("/api/v1/search/index")
            .then().statusCode(201).extract().path("id"));
        }

        List<String> firstPass = pageThrough(token);
        // no duplicate id, no missing id across page boundaries
        assertThat(firstPass).doesNotHaveDuplicates();
        assertThat(firstPass).containsExactlyInAnyOrderElementsOf(indexed);

        // identical repeated request → byte-identical hit ordering (page-stable via id tiebreaker)
        List<String> secondPass = pageThrough(token);
        assertThat(secondPass).containsExactlyElementsOf(firstPass);
    }

    /** Collect hit ids across size-2 pages 0..2 (5 tied hits → 2 + 2 + 1). */
    private List<String> pageThrough(String token) {
        List<String> ids = new ArrayList<>();
        for (int page = 0; page < 3; page++) {
            ids.addAll(given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("{\"query\":\"hello\",\"page\":" + page + ",\"size\":2}")
            .when().post("/api/v1/search")
            .then().statusCode(200).extract().jsonPath().getList("hits.id", String.class));
        }
        return ids;
    }
}
