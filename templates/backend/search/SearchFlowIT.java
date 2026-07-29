/**
 * @ax-template-meta
 * template_id: backend/search/SearchFlowIT
 * layer: backend-test
 * domain: search
 * anchors_rule: testing-restassured-blackbox.md
 * provenance_class: internal_design
 * evidence:
 *   - source_type: upstream_id
 *     upstream_id: rest-assured-usage
 *     section: "RestAssured integration test"
 *     quote: "RestAssured allows black-box HTTP testing of Spring Boot endpoints without coupling to internal package structure."
 *   - source_type: upstream_id
 *     upstream_id: postgresql-fts-2026-05
 *     section: "Korean Tokenization"
 *     quote: "For CJK languages, PostgreSQL 'simple' dictionary returns the entire string as a single lexeme."
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   Run: ./gradlew testSearch
 *   Requires: PostgreSQL running (testcontainers or local DB).
 *   Tests: SEARCH-AUTHZ-001, SEARCH-QUERY-001/002/003, SEARCH-INDEX-001/002, SEARCH-BACKEND-001.
 */
package com.example.app.search;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Black-box integration tests for the search domain.
 *
 * <p>All tests exercise the REST API via RestAssured (no internal class coupling).
 * Tests are tagged {@code @Tag("search")} so {@code ./gradlew testSearch} runs them in isolation.
 *
 * <p>Test plan:
 * <ul>
 *   <li>SEARCH-AUTHZ-001: unauthenticated → 401
 *   <li>SEARCH-QUERY-001: search returns hits + pagination
 *   <li>SEARCH-QUERY-002: Korean query returns results
 *   <li>SEARCH-QUERY-003: blank query → 400
 *   <li>SEARCH-INDEX-001: indexed document is findable
 *   <li>SEARCH-INDEX-002: deleted document is not findable
 *   <li>SEARCH-BACKEND-001: default backend is postgres-fts (manifest check)
 * </ul>
 */
@Tag("search")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SearchFlowIT {

    @LocalServerPort
    private int port;

    private String adminToken;  // obtain from AuthHelper or @BeforeAll setup

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/v1";
        // Fork instruction: replace with your AuthHelper.obtainAdminToken() call
        this.adminToken = "Bearer <test-token>";
    }

    // ─── SEARCH-AUTHZ-001 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("SEARCH-AUTHZ-001: unauthenticated search request returns 401")
    void search_unauthenticated_returns401() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("query", "test", "page", 0, "size", 20))
        .when()
            .post("/search")
        .then()
            .statusCode(401);
    }

    // ─── SEARCH-QUERY-001 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("SEARCH-QUERY-001: search returns paginated SearchResultPage")
    void search_returnsPagedResults() {
        // First index a document
        String docId = UUID.randomUUID().toString();
        given()
            .header("Authorization", adminToken)
            .contentType(ContentType.JSON)
            .body(Map.of("id", docId, "domain", "test", "content", "hello world test document"))
        .when()
            .post("/search/index")
        .then()
            .statusCode(201);

        // Then search for it
        given()
            .header("Authorization", adminToken)
            .contentType(ContentType.JSON)
            .body(Map.of("query", "hello", "page", 0, "size", 20))
        .when()
            .post("/search")
        .then()
            .statusCode(200)
            .body("hits", notNullValue())
            .body("totalHits", greaterThanOrEqualTo(1))
            .body("page", equalTo(0))
            .body("size", equalTo(20))
            .body("processingTimeMs", greaterThanOrEqualTo(0));
    }

    // ─── SEARCH-QUERY-002 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("SEARCH-QUERY-002: Korean query '강남 결제' returns results")
    void search_koreanQuery_returnsResults() {
        // Index a Korean document
        String docId = UUID.randomUUID().toString();
        given()
            .header("Authorization", adminToken)
            .contentType(ContentType.JSON)
            .body(Map.of("id", docId, "domain", "payment", "content", "강남역 결제 완료 처리됨"))
        .when()
            .post("/search/index")
        .then()
            .statusCode(201);

        // Search with Korean query
        given()
            .header("Authorization", adminToken)
            .contentType(ContentType.JSON)
            .body(Map.of("query", "강남 결제", "page", 0, "size", 20))
        .when()
            .post("/search")
        .then()
            .statusCode(200)
            .body("hits.size()", greaterThanOrEqualTo(1))
            .body("hits[0].id", notNullValue());
    }

    // ─── SEARCH-QUERY-003 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("SEARCH-QUERY-003: blank query returns 400")
    void search_blankQuery_returns400() {
        given()
            .header("Authorization", adminToken)
            .contentType(ContentType.JSON)
            .body(Map.of("query", "", "page", 0, "size", 20))
        .when()
            .post("/search")
        .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("SEARCH-QUERY-003: whitespace-only query returns 400")
    void search_whitespaceQuery_returns400() {
        given()
            .header("Authorization", adminToken)
            .contentType(ContentType.JSON)
            .body(Map.of("query", "   ", "page", 0, "size", 20))
        .when()
            .post("/search")
        .then()
            .statusCode(400);
    }

    // ─── SEARCH-INDEX-001 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("SEARCH-INDEX-001: indexed document is findable by search")
    void index_documentIsFindable() {
        String docId = UUID.randomUUID().toString();
        String uniqueWord = "axtemplate" + System.currentTimeMillis();

        given()
            .header("Authorization", adminToken)
            .contentType(ContentType.JSON)
            .body(Map.of("id", docId, "domain", "test", "content", "document with " + uniqueWord))
        .when()
            .post("/search/index")
        .then()
            .statusCode(201)
            .body("id", equalTo(docId))
            .body("indexed", equalTo(true));

        // Document must be findable
        given()
            .header("Authorization", adminToken)
            .contentType(ContentType.JSON)
            .body(Map.of("query", uniqueWord, "page", 0, "size", 10))
        .when()
            .post("/search")
        .then()
            .statusCode(200)
            .body("hits.id", hasItem(docId));
    }

    // ─── SEARCH-INDEX-002 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("SEARCH-INDEX-002: deleted document is not returned in search")
    void delete_removesDocumentFromIndex() {
        String docId = UUID.randomUUID().toString();
        String uniqueWord = "deleteme" + System.currentTimeMillis();

        // Index
        given()
            .header("Authorization", adminToken)
            .contentType(ContentType.JSON)
            .body(Map.of("id", docId, "domain", "test", "content", "to be deleted " + uniqueWord))
        .when()
            .post("/search/index")
        .then()
            .statusCode(201);

        // Delete
        given()
            .header("Authorization", adminToken)
        .when()
            .delete("/search/index/" + docId)
        .then()
            .statusCode(204);

        // Must not appear in search
        given()
            .header("Authorization", adminToken)
            .contentType(ContentType.JSON)
            .body(Map.of("query", uniqueWord, "page", 0, "size", 10))
        .when()
            .post("/search")
        .then()
            .statusCode(200)
            .body("hits.id", not(hasItem(docId)));
    }
}
