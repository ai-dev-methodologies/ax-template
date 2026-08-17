package com.ax.template.authblueprint.sample;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

/**
 * FAIL fixture for test_support_response_validation_guard — one instance of each forbidden shape.
 */
public final class SampleTestSupport {

    private SampleTestSupport() {}

    /** (A) chained value read: no status assertion, no content-type check, no diagnosis. */
    public static String obtainToken(String email) {
        return given()
            .header("Content-Type", "application/json")
            .body("{\"email\":\"" + email + "\"}")
        .when().post("/api/auth/email/login")
        .then().extract().path("accessToken");
    }

    /** (B) two-statement evasion of (A): the extract reads nothing, the next line does. */
    public static String resolveUserId(String token) {
        ExtractableResponse<Response> me = given().header("Authorization", "Bearer " + token)
            .when().get("/api/auth/me")
            .then().extract();
        return me.path("userId");
    }

    /**
     * (A)-only: a chained reader that (B) does not model at all — no {@code path(} in sight. Its
     * presence keeps (A) load-bearing; without it, neutering (A) changes nothing, because (B)
     * happens to cover the {@code .extract().path(...)} shape too.
     */
    public static String rawBody(String token) {
        return given().header("Authorization", "Bearer " + token)
            .when().get("/api/sample/raw")
            .then().extract().asString();
    }

    public static void useRandomPort(int port) {
        RestAssured.port = port;
    }
}
