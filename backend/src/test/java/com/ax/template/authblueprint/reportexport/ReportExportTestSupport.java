package com.ax.template.authblueprint.reportexport;

import com.ax.template.authblueprint.common.HttpExtract;

import io.restassured.RestAssured;
import io.restassured.response.Response;

import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * Shared helpers for report-export integration tests. Mirrors the
 * {@code FileStorageTestSupport} / {@code NotificationTestSupport} pattern — uses real JWT
 * tokens via {@code /api/auth/email/signup} + {@code /login} so the SecurityContext sees a
 * real authenticated user.
 */
public final class ReportExportTestSupport {

    private ReportExportTestSupport() {}

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
