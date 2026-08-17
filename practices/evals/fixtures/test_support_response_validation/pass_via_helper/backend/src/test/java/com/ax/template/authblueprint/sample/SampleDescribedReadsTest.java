package com.ax.template.authblueprint.sample;

import com.ax.template.authblueprint.common.HttpExtract;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * PASS half of detector (D) — BACKLOG P2-119. A caller-side test class reading response values
 * in every shape this tree actually uses to DESCRIBE a response first. None of these may be
 * reported: a guard that flagged them would be flagging the description itself, and the 1404
 * already-described reads on the live tree would all have to be rewritten for no gain.
 *
 * <p>Its presence here is load-bearing in the opposite direction from the fail fixtures: they
 * prove the detector fires, this proves it does not fire on correct code.
 */
class SampleDescribedReadsTest {

    /** Form 1 — the status pinned inside the read's own chain. */
    void inChain() {
        String id = given().header("Content-Type", "application/json")
            .when().post("/api/sample/things")
            .then().statusCode(201).extract().path("id");
        assertThat(id).isNotBlank();
    }

    /** Form 2 — the status asserted on the local, in a neighbouring statement. */
    void onTheLocal() {
        ExtractableResponse<Response> created = given().header("Content-Type", "application/json")
            .when().post("/api/sample/things")
            .then().extract();
        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(created.jsonPath().getString("id")).isNotBlank();
        assertThat(created.jsonPath().getString("status")).isEqualTo("NEW");
    }

    /** Form 2b — the same, via {@code getStatusCode()} and a chained {@code then()}. */
    void onTheLocalViaThen() {
        Response fetched = given().when().get("/api/sample/things/1");
        fetched.then().statusCode(200);
        assertThat(fetched.jsonPath().getString("id")).isNotBlank();

        Response other = given().when().get("/api/sample/things/2");
        assertThat(other.getStatusCode()).isEqualTo(200);
        assertThat(other.jsonPath().getString("id")).isNotBlank();
    }

    /** Form 3 — a same-file helper that pins the status; every read through it is described. */
    void viaSameFileHelper() {
        assertThat(fetch("1").jsonPath().getString("status")).isEqualTo("NEW");
        ExtractableResponse<Response> again = fetch("1");
        assertThat(again.jsonPath().getString("status")).isEqualTo("NEW");
    }

    /** Form 4 — straight through the helper, which describes the response itself. */
    void viaHttpExtract() {
        Response resp = given().when().get("/api/sample/things/1").then().extract().response();
        assertThat((String) HttpExtract.path(resp, "id", "GET /api/sample/things/1")).isNotBlank();
        assertThat((String) HttpExtract.pathAt(resp, 200, "status", "GET /api/sample/things/1"))
            .isEqualTo("NEW");
    }

    /** Reading the STATUS is not reading a body value — it is how a response gets described. */
    void readingTheStatusIsNotAViolation() {
        int status = given().when().get("/api/sample/things/1").then().extract().statusCode();
        assertThat(status).isEqualTo(200);
    }

    /** A parameter's provenance is the CALL SITES' property; declared residue, not reported. */
    private static String statusOf(ExtractableResponse<Response> resp) {
        return resp.jsonPath().getString("status");
    }

    void viaParameter() {
        assertThat(statusOf(fetch("1"))).isEqualTo("NEW");
    }

    private ExtractableResponse<Response> fetch(String id) {
        return given().when().get("/api/sample/things/" + id).then().statusCode(200).extract();
    }
}
