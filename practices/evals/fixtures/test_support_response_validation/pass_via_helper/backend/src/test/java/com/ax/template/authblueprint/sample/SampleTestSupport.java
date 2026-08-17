package com.ax.template.authblueprint.sample;

import com.ax.template.authblueprint.common.HttpExtract;
import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

/**
 * PASS fixture for test_support_response_validation_guard.
 *
 * <p>This javadoc deliberately spells the forbidden shape — {@code .then().extract().path(
 * "accessToken")} — because the real ApprovalWorkflowTestSupport documents the pattern it was
 * moved off. If the guard scanned comments it would report this line, so its presence here is
 * the regression test for comment blanking.
 */
public final class SampleTestSupport {

    private SampleTestSupport() {}

    /** Sanctioned handoff: the whole response goes to the helper, which validates before reading. */
    public static String obtainToken(String email) {
        Response login = given()
            .header("Content-Type", "application/json")
            .body("{\"email\":\"" + email + "\"}")
        .when().post("/api/auth/email/login")
        .then().extract().response();
        return HttpExtract.path(login, "accessToken", "POST /api/auth/email/login (obtainToken)");
    }

    /** A pinned non-2xx read is still a helper read. */
    public static String errorCode(String token) {
        Response denied = given().header("Authorization", "Bearer " + token)
            .when().get("/api/sample/denied")
            .then().extract().response();
        return HttpExtract.pathAt(denied, 403, "code", "GET /api/sample/denied (code)");
    }

    /** A terminal {@code .extract()} reads no value — it hands the response to the caller. */
    public static ExtractableResponse<Response> submit(String token) {
        return given().header("Authorization", "Bearer " + token)
            .when().post("/api/sample/submit")
            .then().extract();
    }

    public static void useRandomPort(int port) {
        RestAssured.port = port;
    }
}
