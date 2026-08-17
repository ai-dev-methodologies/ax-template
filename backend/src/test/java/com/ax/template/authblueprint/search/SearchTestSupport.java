package com.ax.template.authblueprint.search;

import com.ax.template.authblueprint.common.HttpExtract;

import io.restassured.response.Response;

import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * Shared helpers for search domain integration tests. Mirrors the
 * R15/R16 FileStorageTestSupport pattern — issues real JWT tokens via
 * /api/auth/email/signup + /login so the SecurityContext sees a real
 * authenticated user.
 */
public final class SearchTestSupport {

    private SearchTestSupport() {}

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
