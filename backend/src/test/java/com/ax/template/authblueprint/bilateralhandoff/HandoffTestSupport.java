package com.ax.template.authblueprint.bilateralhandoff;

import com.ax.template.authblueprint.common.HttpExtract;
import io.restassured.response.Response;

import java.util.UUID;

import static io.restassured.RestAssured.given;

/** Shared helpers for bilateral-handoff integration tests (mirrors TimedOfferTestSupport). */
public final class HandoffTestSupport {

    private HandoffTestSupport() {}

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
        .when().post("/api/auth/email/login")
        .then().extract().response();
        return HttpExtract.path(login, "accessToken", "POST /api/auth/email/login (obtainToken)");
    }


    /** The caller identity {@code Authentication.getName()} resolves to is the userId (JWT
     *  subject), NOT the email — a party must be named by that same identity for BHO-BIND-001's
     *  equality check to match the real authenticated caller. */
    public static String resolveUserId(String token) {
        Response me = given().header("Authorization", "Bearer " + token)
            .when().get("/api/auth/me").then().statusCode(200).extract().response();
        return HttpExtract.path(me, "userId", "GET /api/auth/me (resolveUserId)");
    }
}
