package com.ax.template.authblueprint.dispatch;

import io.restassured.RestAssured;

import java.util.UUID;

import static io.restassured.RestAssured.given;

/** Shared helpers for dispatch integration tests (mirrors AnnouncementTestSupport): real JWTs via
 *  /api/auth/email/signup + /login so the security pipeline sees a genuine principal. */
public final class DispatchTestSupport {

    private DispatchTestSupport() {}

    public static String freshEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }

    public static String obtainToken(String email, String role) {
        given()
            .header("Content-Type", "application/json")
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\",\"role\":\"" + role + "\"}")
        .when().post("/api/auth/email/signup");

        return given()
            .header("Content-Type", "application/json")
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\"}")
        .when().post("/api/auth/email/login")
        .then().extract().path("accessToken");
    }

    public static void useRandomPort(int port) {
        RestAssured.port = port;
    }
}
