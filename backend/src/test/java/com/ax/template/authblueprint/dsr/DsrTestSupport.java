package com.ax.template.authblueprint.dsr;

import io.restassured.RestAssured;

import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * Shared helpers for DSR integration tests. Mirrors {@code ReportExportTestSupport}
 * — real JWTs via {@code /api/auth/email/signup} + {@code /login} so the security
 * pipeline sees a genuine authenticated user (no MockMvc, no @WithMockUser).
 */
public final class DsrTestSupport {

    private DsrTestSupport() {}

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
