package com.ax.template.authblueprint.tokenizedsecurities;

import static io.restassured.RestAssured.given;

import java.util.UUID;

import io.restassured.RestAssured;

public final class TokenizedSecuritiesTestSupport {

    private TokenizedSecuritiesTestSupport() {}

    public static String freshEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }

    public static String obtainToken(String email, String role) {
        given().header("Content-Type", "application/json")
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\",\"role\":\"" + role + "\"}")
        .when().post("/api/auth/email/signup");
        return given().header("Content-Type", "application/json")
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\"}")
        .when().post("/api/auth/email/login")
        .then().extract().path("accessToken");
    }

    public static void useRandomPort(int port) { RestAssured.port = port; }
}
