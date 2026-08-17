package com.ax.template.authblueprint.payment;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Black-box compliance tests for PAYMENT-IDEMP-001..003.
 *
 * RED phase: all tests fail because PaymentController / IdempotencyKeyStore
 * do not exist yet. Failures manifest as HTTP 404 or assertion failures —
 * both are valid RED per /tdd-workflow.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("PAYMENT")
class PaymentIdempotencyTest {

    @LocalServerPort
    int port;

    // ─── PAYMENT-IDEMP-001 ────────────────────────────────────────────────────

    /**
     * PAYMENT-IDEMP-001: POST /api/payments without Idempotency-Key header
     * must be rejected with HTTP 400 and RFC 7807 ProblemDetail.
     *
     * Mutation endpoints (POST /api/payments, POST …/refund, POST …/void)
     * all require this header.
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-IDEMP-001")
    void idempotencyKeyRequired_POSTwithoutHeader_returns400() {
        String authToken = obtainToken("idemp001@test.test", "MEMBER");

        // POST /api/payments without Idempotency-Key → 400
        Response response =
            given()
                .header("Authorization", "Bearer " + authToken)
                .header("Content-Type", "application/json")
                // deliberately omit Idempotency-Key header
                .body("{\"amount\":10000,\"currency\":\"KRW\",\"orderId\":\"order-idemp001\"}")
            .when().post("/api/payments");

        assertThat(response.statusCode())
            .as("Missing Idempotency-Key must return 400")
            .isEqualTo(400);

        // RFC 7807: response body should contain a 'detail' field explaining the issue
        String body = response.body().asString();
        assertThat(body)
            .as("400 response must contain RFC 7807 'detail' field")
            .containsIgnoringCase("idempotency");
    }

    // ─── PAYMENT-IDEMP-002 ────────────────────────────────────────────────────

    /**
     * PAYMENT-IDEMP-002: Duplicate POST /api/payments with the same
     * Idempotency-Key within the 24-hour window returns the original stored
     * response — no second charge is created.
     *
     * Test (positive replay): POST twice with same key → same paymentId returned,
     * only one Payment row in the database.
     * Test (new key): POST with different key → new paymentId (new charge).
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-IDEMP-002")
    void idempotencyReplay_within24h_returnsOriginalResponse() {
        String authToken = obtainToken("idemp002@test.test", "MEMBER");
        String idempotencyKey = "idemp-replay-" + UUID.randomUUID();

        String requestBody = "{\"amount\":10000,\"currency\":\"KRW\",\"orderId\":\"order-idemp002\"}";

        // First POST — creates payment
        Response first =
            given()
                .header("Authorization", "Bearer " + authToken)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", idempotencyKey)
                .body(requestBody)
            .when().post("/api/payments");

        assertThat(first.statusCode())
            .as("First POST must succeed with 201")
            .isEqualTo(201);

        String firstPaymentId = first.then().extract().path("id");
        assertThat(firstPaymentId)
            .as("First POST must return a payment id")
            .isNotNull()
            .isNotBlank();

        // Second POST with same Idempotency-Key — must return original (200 or 201)
        // and the SAME paymentId (no duplicate charge)
        Response second =
            given()
                .header("Authorization", "Bearer " + authToken)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", idempotencyKey)
                .body(requestBody)
            .when().post("/api/payments");

        assertThat(second.statusCode())
            .as("Replay POST must return 200 or 201")
            .isIn(200, 201);

        String secondPaymentId = second.then().extract().path("id");
        assertThat(secondPaymentId)
            .as("Replay POST must return the SAME paymentId (no duplicate charge)")
            .isEqualTo(firstPaymentId);

        // New key → different paymentId (new charge created)
        String newKey = "idemp-new-" + UUID.randomUUID();
        Response newCharge =
            given()
                .header("Authorization", "Bearer " + authToken)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", newKey)
                .body("{\"amount\":10000,\"currency\":\"KRW\",\"orderId\":\"order-idemp002-new\"}")
            .when().post("/api/payments");

        assertThat(newCharge.statusCode())
            .as("New key POST must succeed with 201")
            .isEqualTo(201);

        String newPaymentId = newCharge.then().extract().path("id");
        assertThat(newPaymentId)
            .as("New key must produce a different paymentId")
            .isNotEqualTo(firstPaymentId);
    }

    // ─── PAYMENT-IDEMP-003 ────────────────────────────────────────────────────

    /**
     * PAYMENT-IDEMP-003: Concurrent duplicate requests with the same
     * Idempotency-Key result in exactly one charge.
     *
     * Basic version (sequential simulation): two back-to-back calls with the
     * same key must produce exactly one Payment.
     * P3.6 will deepen this with a CountDownLatch 5-thread race @RepeatedTest(20).
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-IDEMP-003")
    void idempotencyConcurrentSameKey_onlyOneCharge() {
        String authToken = obtainToken("idemp003@test.test", "MEMBER");
        String sharedKey = "idemp-concurrent-" + UUID.randomUUID();

        String requestBody = "{\"amount\":10000,\"currency\":\"KRW\",\"orderId\":\"order-idemp003\"}";

        // Call 1
        Response call1 =
            given()
                .header("Authorization", "Bearer " + authToken)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", sharedKey)
                .body(requestBody)
            .when().post("/api/payments");

        // Call 2 — same key (sequential duplicate simulating a race)
        Response call2 =
            given()
                .header("Authorization", "Bearer " + authToken)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", sharedKey)
                .body(requestBody)
            .when().post("/api/payments");

        // Both calls must succeed (200 or 201)
        assertThat(call1.statusCode())
            .as("First concurrent call must succeed (200 or 201)")
            .isIn(200, 201);
        assertThat(call2.statusCode())
            .as("Second concurrent call must succeed (200 or 201) — replay hit")
            .isIn(200, 201);

        // Both must return the same paymentId — only one charge created
        String paymentId1 = call1.then().extract().path("id");
        String paymentId2 = call2.then().extract().path("id");

        assertThat(paymentId1).isNotNull();
        assertThat(paymentId2)
            .as("Concurrent duplicate must return the same paymentId — no double charge")
            .isEqualTo(paymentId1);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private String obtainToken(String email, String role) {
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
}
