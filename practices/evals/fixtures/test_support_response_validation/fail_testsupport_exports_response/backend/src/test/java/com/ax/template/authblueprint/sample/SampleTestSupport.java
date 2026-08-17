package com.ax.template.authblueprint.sample;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

/**
 * FAIL fixture for detector (C) ONLY — BACKLOG P2-119.
 *
 * <p>ISOLATION IS THE POINT. Every method here uses the sanctioned shapes for (A) and (B): the
 * only {@code .extract()} calls are TERMINAL (they read nothing, so (A) permits them), there is
 * no {@code .path(} / {@code .pathAt(} / {@code .jsonPath(} anywhere (so (B) cannot fire), and
 * there is no non-TestSupport test class in this tree at all (so (D) has nothing to scan). The
 * ONLY reason this fixture exits 1 is that these methods hand a rest-assured RESPONSE out of a
 * TestSupport helper — which is exactly what makes the single-anchor kill-proof possible here,
 * where the 2026-08-17 {@code fail_blind_extract} fixture could not have one.
 *
 * <p>WHY IT IS A DEFECT: the caller receiving this object is a Compliance test, a file detectors
 * (A) and (B) never scan. It can read every field of the body without ever asserting a status —
 * and on this tree that read then reports nothing when the response was a 401 problem+json (it
 * yields a silent null) or carried no Content-Type at all (the P3-144 preimage).
 */
public final class SampleTestSupport {

    private SampleTestSupport() {}

    /** Exports the whole ExtractableResponse — the P2-119 residue, in its original form. */
    public static ExtractableResponse<Response> submit(String token) {
        return given().header("Authorization", "Bearer " + token)
            .when().post("/api/sample/submit")
            .then().extract();
    }

    /** The same defect one type down: a bare {@code Response} escapes just as undescribed. */
    public static Response fetch(String token, String id) {
        return given().header("Authorization", "Bearer " + token)
            .when().get("/api/sample/things/" + id);
    }

    /** Returning a plain value is the sanctioned shape and must NOT be reported. */
    public static String freshEmail(String prefix) {
        return prefix + "@example.com";
    }

    /** Neither must a void method that only pins a status. */
    public static void submitOk(String token) {
        given().header("Authorization", "Bearer " + token)
            .when().post("/api/sample/submit")
            .then().statusCode(200);
    }
}
