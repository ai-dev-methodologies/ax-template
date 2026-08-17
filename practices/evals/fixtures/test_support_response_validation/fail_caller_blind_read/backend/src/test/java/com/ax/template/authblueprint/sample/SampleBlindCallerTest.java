package com.ax.template.authblueprint.sample;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * FAIL fixture for detector (D) ONLY — BACKLOG P2-119.
 *
 * <p>ISOLATION IS THE POINT. There is NO {@code *TestSupport.java} anywhere in this fixture tree,
 * so detectors (A), (B) and (C) — all three scoped to that filename — have nothing to scan and
 * cannot fire. Every violation below is a caller-side read of a response whose status nobody
 * asserted, which only (D) sees. That is what makes the single-anchor kill-proof possible.
 *
 * <p>Three origins are represented, because (D) resolves the response object rather than pattern
 * matching a line: an inline chain, a local, and a same-file helper that forgot to pin its status.
 * Any one of them alone would be enough to exit 1; all three are here so a partial regression in
 * the resolver is still visible in the violation COUNT.
 */
class SampleBlindCallerTest {

    /** Origin: an inline chain with no {@code statusCode(...)} anywhere in it. */
    void inlineChain() {
        String id = given().header("Content-Type", "application/json")
            .when().post("/api/sample/things")
            .then().extract().path("id");
        assertThat(id).isNotBlank();
    }

    /** Origin: a local whose status is never asserted — the neighbouring-statement form, absent. */
    void undescribedLocal() {
        ExtractableResponse<Response> created = given().header("Content-Type", "application/json")
            .when().post("/api/sample/things")
            .then().extract();
        assertThat(created.jsonPath().getString("id")).isNotBlank();
    }

    /** Origin: a same-file helper whose own chain never pins a status. */
    void viaUndescribedHelper() {
        assertThat(fetch("1").jsonPath().getString("status")).isEqualTo("NEW");
    }

    /** Correct code that must NOT be counted: the status IS pinned in this chain. */
    void described() {
        String id = given().when().get("/api/sample/things/1")
            .then().statusCode(200).extract().path("id");
        assertThat(id).isNotBlank();
    }

    private ExtractableResponse<Response> fetch(String id) {
        return given().when().get("/api/sample/things/" + id).then().extract();
    }
}
