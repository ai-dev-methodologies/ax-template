package com.ax.template.authblueprint.reservation;

import com.ax.template.authblueprint.common.HttpExtract;

import io.restassured.RestAssured;
import io.restassured.response.Response;

import java.util.UUID;

import static io.restassured.RestAssured.given;

/** Shared helpers for reservation integration tests (mirrors GovernedRecordTestSupport). */
public final class ReservationTestSupport {

    private ReservationTestSupport() {}

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
