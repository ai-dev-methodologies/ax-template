package com.ax.template.authblueprint.filestorage;

import io.restassured.RestAssured;

import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * Shared helpers for file-storage integration tests. Mirrors the
 * R15 NotificationTestSupport pattern — uses real JWT tokens via
 * /api/auth/email/signup + /login so the SecurityContext sees a real
 * authenticated user.
 */
public final class FileStorageTestSupport {

    private FileStorageTestSupport() {}

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

    /** JWT subject claim — equals what {@code Authentication#getName()} returns server-side. */
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
