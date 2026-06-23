package com.ax.template.authblueprint.queryguard;

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
 * query-field-allowlist-l0 compliance — verified against the live queryguard reference workload.
 * The invariant: a list endpoint that accepts client-supplied SORT and FILTER field names bounds
 * them by a per-resource allowlist; a sort/filter naming a non-allowlisted field (or a bad
 * direction/operator) is a 422 that NAMES the offending field — never silently ignored, never
 * forwarded into a Sort/Specification; the response is a bounded PageEnvelope.
 * Spec: specs/query-field-allowlist-l0.yaml (OWASP API3:2023 + CWE-89 + CWE-639).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("QUERYGUARD")
class QueryGuardComplianceTest {

    @LocalServerPort int port;
    String member;

    @BeforeEach
    void setup() {
        QueryGuardTestSupport.useRandomPort(port);
        member = QueryGuardTestSupport.obtainToken(QueryGuardTestSupport.freshEmail("qg-member"), "MEMBER");
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────
    private String createItem(String name, String status, long priceMinor, String internalNotes) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"name\":\"" + name + "\",\"status\":\"" + status + "\","
                + "\"priceMinor\":" + priceMinor + ",\"internalNotes\":\"" + internalNotes + "\"}")
        .when().post("/api/query-guard/items").then().statusCode(201).extract().path("id");
    }

    private ExtractableResponse<Response> list(String query) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/query-guard/items" + query).thenReturn().then().extract();
    }

    private void seedThree() {
        createItem("Alpha", "ACTIVE", 300L, "secret-a");
        createItem("Bravo", "DRAFT", 100L, "secret-b");
        createItem("Charlie", "ARCHIVED", 200L, "secret-c");
    }

    // ── QUERY-ALLOWLIST-SORT-001 — sortable field + asc/desc direction; non-allowlisted → 422 ──
    @Test @Tag("QUERY-ALLOWLIST-SORT-001")
    void sort_allowlistedField_sorts_nonAllowlisted_is422NamingField() {
        seedThree();

        // sort=name asc → 200, ascending by name
        ExtractableResponse<Response> asc = list("?sort=name&direction=asc");
        assertThat(asc.statusCode()).isEqualTo(200);
        List<String> names = asc.jsonPath().getList("data.name");
        assertThat(names).contains("Alpha", "Bravo", "Charlie");
        assertThat(names.indexOf("Alpha")).isLessThan(names.indexOf("Bravo"));

        // sort=priceMinor desc → 200, descending
        ExtractableResponse<Response> desc = list("?sort=priceMinor&direction=desc");
        assertThat(desc.statusCode()).isEqualTo(200);
        List<Integer> prices = desc.jsonPath().getList("data.priceMinor");
        assertThat(prices.get(0)).isGreaterThanOrEqualTo(prices.get(prices.size() - 1));

        // sort=password → 422 QUERY_FIELD_NOT_SORTABLE naming the field
        ExtractableResponse<Response> bad = list("?sort=password&direction=asc");
        assertThat(bad.statusCode()).isEqualTo(422);
        assertThat(bad.jsonPath().getString("code")).isEqualTo("QUERY_FIELD_NOT_SORTABLE");
        assertThat(bad.jsonPath().getString("detail")).contains("password");
    }

    // ── QUERY-ALLOWLIST-SORT-001 — a direction outside {asc,desc} → 422 ──
    @Test @Tag("QUERY-ALLOWLIST-SORT-001")
    void sort_invalidDirection_is422() {
        seedThree();
        ExtractableResponse<Response> bad = list("?sort=name&direction=sideways");
        assertThat(bad.statusCode()).isEqualTo(422);
        assertThat(bad.jsonPath().getString("code")).isEqualTo("QUERY_DIRECTION_INVALID");
        assertThat(bad.jsonPath().getString("detail")).contains("sideways");
    }

    // ── QUERY-ALLOWLIST-FILTER-001 — filterable field + safe operator; non-allowlisted → 422 ──
    @Test @Tag("QUERY-ALLOWLIST-FILTER-001")
    void filter_allowlistedField_filters_nonAllowlisted_is422NamingField() {
        seedThree();

        // filter=status:eq:ACTIVE → 200, only ACTIVE rows
        ExtractableResponse<Response> filtered = list("?filter=status:eq:ACTIVE");
        assertThat(filtered.statusCode()).isEqualTo(200);
        List<String> statuses = filtered.jsonPath().getList("data.status");
        assertThat(statuses).isNotEmpty().allMatch("ACTIVE"::equals);

        // filter=priceMinor:gte:200 → 200, only >= 200
        ExtractableResponse<Response> byPrice = list("?filter=priceMinor:gte:200");
        assertThat(byPrice.statusCode()).isEqualTo(200);
        List<Integer> prices = byPrice.jsonPath().getList("data.priceMinor");
        assertThat(prices).isNotEmpty().allMatch(p -> p >= 200);

        // filter=internalNotes:eq:x → 422 QUERY_FIELD_NOT_FILTERABLE naming the field
        ExtractableResponse<Response> bad = list("?filter=internalNotes:eq:secret-a");
        assertThat(bad.statusCode()).isEqualTo(422);
        assertThat(bad.jsonPath().getString("code")).isEqualTo("QUERY_FIELD_NOT_FILTERABLE");
        assertThat(bad.jsonPath().getString("detail")).contains("internalNotes");
    }

    // ── QUERY-ALLOWLIST-FILTER-001 — an operator outside the closed safe set → 422 ──
    @Test @Tag("QUERY-ALLOWLIST-FILTER-001")
    void filter_invalidOperator_is422() {
        seedThree();
        // a SQL-fragment-shaped operator → 422 QUERY_OPERATOR_INVALID, never executed
        ExtractableResponse<Response> bad = list("?filter=name:DROP TABLE:x");
        assertThat(bad.statusCode()).isEqualTo(422);
        assertThat(bad.jsonPath().getString("code")).isEqualTo("QUERY_OPERATOR_INVALID");
    }

    // ── QUERY-ALLOWLIST-MAPPING-001 — only PUBLIC names resolve; the 422 names the public field ──
    @Test @Tag("QUERY-ALLOWLIST-MAPPING-001")
    void mapping_onlyPublicNamesResolve() {
        seedThree();
        // the keystone target 'internalNotes' has no sortable mapping → 422 not-sortable
        ExtractableResponse<Response> bad = list("?sort=internalNotes&direction=asc");
        assertThat(bad.statusCode()).isEqualTo(422);
        assertThat(bad.jsonPath().getString("code")).isEqualTo("QUERY_FIELD_NOT_SORTABLE");
        assertThat(bad.jsonPath().getString("detail")).contains("internalNotes");

        // a default (no sort param) list still succeeds — sortable surface is well-defined
        assertThat(list("").statusCode()).isEqualTo(200);
    }

    // ── QUERY-ALLOWLIST-PAGE-001 — bounded PageEnvelope with the five pagination members ──
    @Test @Tag("QUERY-ALLOWLIST-PAGE-001")
    void list_returnsBoundedPageEnvelope() {
        seedThree();
        ExtractableResponse<Response> resp = list("?page=0&size=2");
        assertThat(resp.statusCode()).isEqualTo(200);
        assertThat(resp.jsonPath().getList("data")).isNotNull();
        assertThat(resp.jsonPath().getInt("pagination.page")).isEqualTo(0);
        assertThat(resp.jsonPath().getInt("pagination.pageSize")).isEqualTo(2);
        assertThat(resp.jsonPath().getLong("pagination.totalElements")).isGreaterThanOrEqualTo(3L);
        assertThat(resp.jsonPath().getInt("pagination.totalPages")).isGreaterThanOrEqualTo(2);
        assertThat(resp.jsonPath().getBoolean("pagination.hasMore")).isTrue();

        // an oversized page size is rejected (OffsetPageSupport clamp → 400 PAGE_SIZE_INVALID)
        assertThat(list("?size=5000").statusCode()).isEqualTo(400);

        // the internalNotes field is never carried out in the DTO
        ExtractableResponse<Response> any = list("?size=10");
        assertThat(any.jsonPath().getList("data.internalNotes").stream().filter(x -> x != null).count())
            .as("internalNotes is @JsonIgnore — never serialized").isEqualTo(0L);
    }

    // ── QUERY-ALLOWLIST-KEYSTONE-001 — keystone: malicious sort/filter field → 422 named, never executed ──
    @Test @Tag("QUERY-ALLOWLIST-KEYSTONE-001")
    void keystone_maliciousField_is422Named_neverPassedThrough() {
        seedThree();

        // a private property name
        ExtractableResponse<Response> pw = list("?sort=password&direction=asc");
        assertThat(pw.statusCode()).isEqualTo(422);
        assertThat(pw.jsonPath().getString("code")).isEqualTo("QUERY_FIELD_NOT_SORTABLE");
        assertThat(pw.jsonPath().getString("detail")).contains("password");

        // an internal-only field
        ExtractableResponse<Response> notes = list("?filter=internalNotes:eq:secret-a");
        assertThat(notes.statusCode()).isEqualTo(422);
        assertThat(notes.jsonPath().getString("code")).isEqualTo("QUERY_FIELD_NOT_FILTERABLE");

        // a SQL fragment as the sort field — rejected by name, not neutralized after the fact
        ExtractableResponse<Response> sqli =
            given().header("Authorization", "Bearer " + member)
                .when().get("/api/query-guard/items?sort=name%3B%20DROP%20TABLE%20catalog_items&direction=asc")
                .thenReturn().then().extract();
        assertThat(sqli.statusCode()).isEqualTo(422);
        assertThat(sqli.jsonPath().getString("code")).isEqualTo("QUERY_FIELD_NOT_SORTABLE");

        // none of the rejections deleted/altered data — the rows are still all listable
        assertThat(list("?size=50").jsonPath().getLong("pagination.totalElements"))
            .as("the malicious queries never executed against the table").isGreaterThanOrEqualTo(3L);
    }

    // ── AuthZ — every endpoint requires a JWT ──
    @Test @Tag("QUERY-ALLOWLIST-KEYSTONE-001")
    void list_withoutToken_is401() {
        assertThat(given().when().get("/api/query-guard/items").thenReturn().statusCode()).isEqualTo(401);
    }
}
