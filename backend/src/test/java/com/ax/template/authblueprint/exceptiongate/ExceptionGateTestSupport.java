package com.ax.template.authblueprint.exceptiongate;

import io.restassured.RestAssured;

import java.util.UUID;

import static io.restassured.RestAssured.given;

/** Shared helpers for orthogonal-exception-gate integration tests (mirrors TrueUpTestSupport). */
public final class ExceptionGateTestSupport {

    private ExceptionGateTestSupport() {}

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
