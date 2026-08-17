package com.ax.template.authblueprint.register;

import com.ax.template.authblueprint.common.HttpExtract;

import io.restassured.RestAssured;
import io.restassured.response.Response;

import java.util.UUID;

import static io.restassured.RestAssured.given;

/** Shared helpers for monotone-register integration tests (mirrors ReservationTestSupport). */
public final class RegisterTestSupport {

    private RegisterTestSupport() {}

    public static String freshEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }

    public static String obtainToken(String email, String role) {
        given()
            .header("Content-Type", "application/json")
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\",\"role\":\"" + role + "\"}")
        .when().post("/api/auth/email/signup");

        Response login = given()
            .header("Content-Type", "application/json")
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\"}")
        .when().post("/api/auth/email/login");
        return HttpExtract.path(login, "accessToken",
            "POST /api/auth/email/login (accessToken for " + role + ")");
    }

    public static void useRandomPort(int port) {
        RestAssured.port = port;
    }
}
