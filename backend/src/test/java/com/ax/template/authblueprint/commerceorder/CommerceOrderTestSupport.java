package com.ax.template.authblueprint.commerceorder;

import io.restassured.RestAssured;

import java.util.UUID;

import static io.restassured.RestAssured.given;

public final class CommerceOrderTestSupport {

    private CommerceOrderTestSupport() {}

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

    public static void useRandomPort(int port) {
        RestAssured.port = port;
    }

    /** Create a cart and return the order id. */
    public static String createCart(String token, String currency) {
        return given()
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .body("{\"currency\":\"" + currency + "\"}")
        .when().post("/api/orders")
        .then().statusCode(201)
        .extract().path("id");
    }

    /** Add an item to the cart and return the item id. */
    public static String addItem(String token, String orderId,
                                  String skuId, String name, long unitPrice, int quantity) {
        String body = String.format(
            "{\"skuId\":\"%s\",\"nameAtAdd\":\"%s\",\"unitPriceAtAdd\":%d,\"quantity\":%d}",
            skuId, name, unitPrice, quantity);
        return given()
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .body(body)
        .when().post("/api/orders/" + orderId + "/items")
        .then().statusCode(200)
        .extract().path("id");
    }

    /** Submit the order. Returns the response body map. */
    public static io.restassured.response.ExtractableResponse<io.restassured.response.Response>
        submit(String token, String orderId, long total, long subTotal, long tax) {
        String body = String.format(
            "{\"total\":%d,\"subTotal\":%d,\"tax\":%d}", total, subTotal, tax);
        return given()
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .body(body)
        .when().post("/api/orders/" + orderId + "/submit")
        .then().extract();
    }
}
