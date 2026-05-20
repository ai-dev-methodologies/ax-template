package com.ax.template.authblueprint.notification;

import io.restassured.RestAssured;

import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * Shared helpers for notification integration tests.
 *
 * <p>All tests use real auth tokens via /api/auth/email/signup + /login so the
 * SecurityContext sees a real authenticated user — matching NOTIF-AUTHZ-002
 * which derives the caller's userId from the SecurityContext, never a URL.
 */
public final class NotificationTestSupport {

    private NotificationTestSupport() {}

    public static String freshEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }

    /** Signs up the user then logs in and returns the access token. */
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

    /**
     * Resolves the authenticated user's id (the JWT {@code sub} claim) — needed
     * for direct DB seeding. The notification service stores
     * {@code recipientUserId = auth.getName()}, which equals the JWT subject.
     */
    public static String resolveCallerUserId(String token) {
        return given()
            .header("Authorization", "Bearer " + token)
        .when().get("/api/auth/me")
        .then().extract().path("userId");
    }

    public static void useRandomPort(int port) {
        RestAssured.port = port;
    }
}
