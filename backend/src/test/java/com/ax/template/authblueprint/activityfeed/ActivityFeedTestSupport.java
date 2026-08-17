package com.ax.template.authblueprint.activityfeed;

import com.ax.template.authblueprint.common.HttpExtract;

import io.restassured.RestAssured;
import io.restassured.response.Response;

import java.util.UUID;

import static io.restassured.RestAssured.given;

public final class ActivityFeedTestSupport {

    private ActivityFeedTestSupport() {}

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

    public static String resolveUserId(String token) {
        Response me = given()
            .header("Authorization", "Bearer " + token)
        .when().get("/api/auth/me");
        return HttpExtract.path(me, "userId", "GET /api/auth/me (userId)");
    }

    public static void useRandomPort(int port) {
        RestAssured.port = port;
    }
}
