package com.ax.template.authblueprint.inventoryreservation;

import com.ax.template.authblueprint.common.HttpExtract;
import io.restassured.RestAssured;
import io.restassured.response.Response;

import java.util.UUID;

import static io.restassured.RestAssured.given;

/** Shared helpers for two-axis-inventory-reservation integration tests (mirrors DunningTestSupport). */
public final class InventoryReservationTestSupport {

    private InventoryReservationTestSupport() {}

    public static String freshEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }

    public static String obtainToken(String email, String role) {
        given()
            .header("Content-Type", "application/json")
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\",\"role\":\"" + role + "\"}")
        .when().post("/api/auth/email/signup");

        Response httpExtractResponse = given()
            .header("Content-Type", "application/json")
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\"}")
        .when().post("/api/auth/email/login")
        .then().extract().response();
        return HttpExtract.path(httpExtractResponse, "accessToken", "POST /api/auth/email/login (obtainToken)");
    }

}
