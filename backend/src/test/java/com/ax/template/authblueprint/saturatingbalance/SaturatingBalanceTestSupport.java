package com.ax.template.authblueprint.saturatingbalance;

import com.ax.template.authblueprint.common.HttpExtract;

import io.restassured.response.Response;

import java.util.UUID;

import static io.restassured.RestAssured.given;

/** Shared helpers for saturating-balance integration tests (mirrors QueryGuardTestSupport). */
public final class SaturatingBalanceTestSupport {

    private SaturatingBalanceTestSupport() {}

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
}
