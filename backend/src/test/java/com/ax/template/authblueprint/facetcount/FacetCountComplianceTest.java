package com.ax.template.authblueprint.facetcount;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * facet-count-l0 compliance — verified against the live facetcount reference workload.
 * The invariant: bucket counts are computed over exactly the caller-visible (owner-scoped)
 * query, the facet-by field is a compile-time allowlist (fail-closed 422 on a non-allowlisted
 * field), and the bucket cardinality is bounded top-K with an explicit otherCount remainder.
 * Spec: specs/facet-count-l0.yaml (OWASP API3:2023 + CWE-639).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("FACETCOUNT")
class FacetCountComplianceTest {

    @LocalServerPort int port;

    @BeforeEach
    void setup() {
        FacetCountTestSupport.useRandomPort(port);
    }

    private String createItem(String token, String category, String status) {
        return given().header("Authorization", "Bearer " + token).header("Content-Type", "application/json")
            .body("{\"category\":\"" + category + "\",\"status\":\"" + status + "\"}")
        .when().post("/api/facet-count/items").then().statusCode(201).extract().path("id");
    }

    private ExtractableResponse<Response> facets(String token, String field) {
        return given().header("Authorization", "Bearer " + token)
            .when().get("/api/facet-count/items/facets?field=" + field)
            .thenReturn().then().extract();
    }

    // ── FACET-COUNT-001 — bucket counts are scoped to the caller's OWN rows, never a wider table scan ──
    @Test @Tag("FACET-COUNT-001")
    void facets_areScopedToCallerOwnRows() {
        String tokenA = FacetCountTestSupport.obtainToken(FacetCountTestSupport.freshEmail("fc-a"), "MEMBER");
        String tokenB = FacetCountTestSupport.obtainToken(FacetCountTestSupport.freshEmail("fc-b"), "MEMBER");

        createItem(tokenA, "X", "OPEN");
        createItem(tokenA, "X", "OPEN");
        createItem(tokenA, "Y", "CLOSED");

        createItem(tokenB, "Z", "OPEN");
        createItem(tokenB, "Z", "OPEN");
        createItem(tokenB, "Z", "OPEN");
        createItem(tokenB, "Z", "OPEN");
        createItem(tokenB, "Z", "OPEN");

        ExtractableResponse<Response> aFacets = facets(tokenA, "category");
        assertThat(aFacets.statusCode()).isEqualTo(200);
        List<String> aValues = aFacets.jsonPath().getList("buckets.value", String.class);
        assertThat(aValues).contains("X", "Y").doesNotContain("Z");

        ExtractableResponse<Response> bFacets = facets(tokenB, "category");
        assertThat(bFacets.statusCode()).isEqualTo(200);
        List<String> bValues = bFacets.jsonPath().getList("buckets.value", String.class);
        assertThat(bValues).contains("Z").doesNotContain("X", "Y");
    }

    // ── FACET-ALLOWLIST-002 — a non-allowlisted field is rejected by NAME, fail-closed, before any query ──
    @Test @Tag("FACET-ALLOWLIST-002")
    void facets_nonAllowlistedField_is422NamingField() {
        String token = FacetCountTestSupport.obtainToken(FacetCountTestSupport.freshEmail("fc-bad"), "MEMBER");
        createItem(token, "X", "OPEN");

        ExtractableResponse<Response> bad = facets(token, "ownerId");
        assertThat(bad.statusCode()).isEqualTo(422);
        assertThat(bad.jsonPath().getString("code")).isEqualTo("FACET_FIELD_NOT_ALLOWED");
        assertThat(bad.jsonPath().getString("detail")).contains("ownerId");

        // an allowlisted field still succeeds — the surface is well-defined, not just deny-all
        assertThat(facets(token, "category").statusCode()).isEqualTo(200);
    }

    // ── FACET-BOUND-003 — bounded top-K + otherCount remainder; Σ(buckets) + otherCount == total ──
    @Test @Tag("FACET-BOUND-003")
    void facets_boundedTopKWithOtherCountRemainder() {
        String token = FacetCountTestSupport.obtainToken(FacetCountTestSupport.freshEmail("fc-bound"), "MEMBER");
        String[] categories = {"c1", "c2", "c3", "c4", "c5", "c6", "c7", "c8"};
        for (String category : categories) {
            createItem(token, category, "OPEN");
        }

        ExtractableResponse<Response> resp = facets(token, "category");
        assertThat(resp.statusCode()).isEqualTo(200);
        List<Integer> counts = resp.jsonPath().getList("buckets.count", Integer.class);
        assertThat(counts).hasSizeLessThanOrEqualTo(FacetCountService.MAX_BUCKETS);

        long bucketSum = counts.stream().mapToLong(Integer::longValue).sum();
        long otherCount = resp.jsonPath().getLong("otherCount");
        assertThat(otherCount).isGreaterThan(0);
        assertThat(bucketSum + otherCount).isEqualTo(categories.length);
    }

    // ── AuthZ — every endpoint requires a JWT ──
    @Test @Tag("FACET-COUNT-001")
    void facets_withoutToken_is401() {
        assertThat(given().when().get("/api/facet-count/items/facets?field=category")
            .thenReturn().statusCode()).isEqualTo(401);
    }
}
