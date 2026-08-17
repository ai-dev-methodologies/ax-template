package com.ax.template.authblueprint.piecewisedeadband;

import com.ax.template.authblueprint.common.HttpExtract;
import io.restassured.RestAssured;
import io.restassured.response.Response;

import java.util.UUID;

import static io.restassured.RestAssured.given;

/** Shared helpers for piecewise-deadband integration tests (mirrors ThresholdTestSupport). */
public final class DeadbandTestSupport {

    private DeadbandTestSupport() {}

    public static String freshEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }

    public static String obtainToken(String email, String role) {
        given()
            .header("Content-Type", "application/json")
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\",\"role\":\"" + role + "\"}")
        .when().post("/api/auth/email/signup");

        Response httpExtractResponse = given()
            .header("Content-Type", "application/json")
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\"}")
        .when().post("/api/auth/email/login")
        .then().extract().response();
        return HttpExtract.path(httpExtractResponse, "accessToken", "POST /api/auth/email/login (obtainToken)");
    }

    public static void useRandomPort(int port) {
        RestAssured.port = port;
    }
}
