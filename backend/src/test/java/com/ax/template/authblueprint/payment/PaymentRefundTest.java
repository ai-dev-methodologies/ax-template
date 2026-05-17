package com.ax.template.authblueprint.payment;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Black-box compliance tests for PAYMENT-REFUND-001..003.
 *
 * RED phase: all tests fail because PaymentController / RefundService /
 * refund window enforcement do not exist yet. Failures manifest as HTTP 404
 * (endpoint missing) or assertion failures — both are valid RED per /tdd-workflow.
 *
 * Spec: specs/payment-l0.yaml REFUND family.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("PAYMENT")
class PaymentRefundTest {

    @LocalServerPort
    int port;

    @BeforeEach
    void setup() {
        RestAssured.port = port;
    }

    // ─── PAYMENT-REFUND-001 (positive: within 30-day window) ─────────────────

    /**
     * PAYMENT-REFUND-001 (positive): Refund within the 30-day window is accepted.
     *
     * Creates a payment, captures it, then refunds it within the allowed
     * window (simulated by a fresh capture) → 201.
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-REFUND-001")
    void refundWindow_within30days_allowed() {
        String token = obtainToken("refund001a@test.test", "MEMBER");

        // Create and capture a payment (capturedAt = now)
        String paymentId = createCapturedPayment(token, "order-refund001a", 10000);

        // Refund immediately — within the 30-day window
        Response refundResponse =
            given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("{\"amount\":10000}")
            .when().post("/api/payments/" + paymentId + "/refund");

        assertThat(refundResponse.statusCode())
            .as("Refund within 30-day window must return 201")
            .isEqualTo(201);
    }

    // ─── PAYMENT-REFUND-001 (negative: after 30-day window) ──────────────────

    /**
     * PAYMENT-REFUND-001 (negative): Refund outside the 30-day window is rejected.
     *
     * Creates a payment with a back-dated capturedAt (31 days ago) and attempts
     * to refund it → 409 with RFC 7807 ProblemDetail containing "refund window".
     *
     * Implementation will need to accept an override capturedAt in the test
     * context (e.g., via a test-only endpoint or time-injection mechanism).
     * In RED state this test fails with 404 (endpoint missing).
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-REFUND-001")
    void refundWindow_after30days_returns409() {
        String token = obtainToken("refund001b@test.test", "MEMBER");

        // Create a payment with capturedAt = now - 31 days via test-override header
        // or a dedicated test fixture endpoint that back-dates the capturedAt.
        // In RED phase, POST /api/payments returns 404 — valid RED failure.
        Response createResponse =
            given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                // X-Test-CapturedAt: ISO-8601 back-date used by test fixtures only
                .header("X-Test-CapturedAt", java.time.Instant.now()
                    .minus(31, java.time.temporal.ChronoUnit.DAYS)
                    .toString())
                .body("{\"amount\":10000,\"currency\":\"KRW\",\"orderId\":\"order-refund001b\"}")
            .when().post("/api/payments");

        assertThat(createResponse.statusCode())
            .as("Create payment with back-dated capturedAt must return 201")
            .isEqualTo(201);

        String paymentId = createResponse.then().extract().path("id");

        // Attempt refund outside the 30-day window → 409
        Response refundResponse =
            given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("{\"amount\":10000}")
            .when().post("/api/payments/" + paymentId + "/refund");

        assertThat(refundResponse.statusCode())
            .as("Refund outside 30-day window must return 409")
            .isEqualTo(409);

        // RFC 7807: detail must mention refund window expiry
        String body = refundResponse.body().asString();
        assertThat(body)
            .as("409 body must reference refund window in RFC 7807 detail")
            .satisfiesAnyOf(
                b -> assertThat(b).containsIgnoringCase("window"),
                b -> assertThat(b).containsIgnoringCase("expired"),
                b -> assertThat(b).containsIgnoringCase("refund")
            );
    }

    // ─── PAYMENT-REFUND-002 (positive: partial refund within captured amount) ─

    /**
     * PAYMENT-REFUND-002 (positive): Partial refund where sum(refunds) ≤ capturedAmount.
     *
     * Payment for 10000 KRW. Partial refund 3000 → 201.
     * Remaining balance must be 7000.
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-REFUND-002")
    void refundPartial_sumWithinPaymentAmount_allowed() {
        String token = obtainToken("refund002a@test.test", "MEMBER");

        // Create and capture a 10000 KRW payment
        String paymentId = createCapturedPayment(token, "order-refund002a", 10000);

        // Partial refund: 3000 of 10000 — sum ≤ capturedAmount
        Response refundResponse =
            given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("{\"amount\":3000}")
            .when().post("/api/payments/" + paymentId + "/refund");

        assertThat(refundResponse.statusCode())
            .as("Partial refund (3000 of 10000) must return 201")
            .isEqualTo(201);

        // Verify remaining balance = 7000
        Response paymentState =
            given()
                .header("Authorization", "Bearer " + token)
            .when().get("/api/payments/" + paymentId);

        // balance or remainingAmount should be 7000
        String balanceStr = paymentState.then().extract().path("balance") != null
            ? paymentState.then().extract().path("balance").toString()
            : paymentState.then().extract().path("remainingAmount") != null
                ? paymentState.then().extract().path("remainingAmount").toString()
                : null;

        assertThat(balanceStr)
            .as("Remaining balance after 3000 partial refund on 10000 payment must be 7000")
            .isNotNull()
            .isEqualTo("7000");
    }

    // ─── PAYMENT-REFUND-002 (negative: sum exceeds captured amount) ───────────

    /**
     * PAYMENT-REFUND-002 (negative): Partial refund where sum(refunds) > capturedAmount
     * returns 400 with RFC 7807 ProblemDetail.
     *
     * Payment 10000 KRW. Refund 3000 → 201.
     * Second refund 8000 (total 11000 > 10000) → 400.
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-REFUND-002")
    void refundPartial_sumExceedsPaymentAmount_returns400() {
        String token = obtainToken("refund002b@test.test", "MEMBER");

        // Create and capture a 10000 KRW payment
        String paymentId = createCapturedPayment(token, "order-refund002b", 10000);

        // First partial refund: 3000 → accepted
        int firstRefundStatus =
            given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("{\"amount\":3000}")
            .when().post("/api/payments/" + paymentId + "/refund")
            .then().extract().statusCode();

        assertThat(firstRefundStatus)
            .as("First partial refund (3000) must return 201")
            .isEqualTo(201);

        // Second partial refund: 8000 (total 11000 > 10000) → must be rejected
        Response overRefundResponse =
            given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("{\"amount\":8000}")
            .when().post("/api/payments/" + paymentId + "/refund");

        assertThat(overRefundResponse.statusCode())
            .as("Over-refund (sum 11000 > captured 10000) must return 400")
            .isEqualTo(400);

        // RFC 7807: detail must reference amount violation
        String body = overRefundResponse.body().asString();
        assertThat(body)
            .as("400 body must contain RFC 7807 detail about sum exceeding captured amount")
            .satisfiesAnyOf(
                b -> assertThat(b).containsIgnoringCase("sum"),
                b -> assertThat(b).containsIgnoringCase("exceeds"),
                b -> assertThat(b).containsIgnoringCase("amount"),
                b -> assertThat(b).containsIgnoringCase("captured")
            );
    }

    // ─── PAYMENT-REFUND-003: refund-of-refund denied with 409 ────────────────

    /**
     * PAYMENT-REFUND-003: A refund-of-refund (refunding an already-REFUNDED
     * payment) is denied with 409 and RFC 7807 ProblemDetail.
     *
     * Flow: create → capture → full refund (state REFUNDED) →
     *       attempt second refund → 409.
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-REFUND-003")
    void refundOfRefund_denied_returns409() {
        String token = obtainToken("refund003@test.test", "MEMBER");

        // Create and capture a 10000 KRW payment
        String paymentId = createCapturedPayment(token, "order-refund003", 10000);

        // Full refund → payment state becomes REFUNDED
        int fullRefundStatus =
            given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("{\"amount\":10000}")
            .when().post("/api/payments/" + paymentId + "/refund")
            .then().extract().statusCode();

        assertThat(fullRefundStatus)
            .as("Full refund must return 201")
            .isEqualTo(201);

        // Attempt a second refund on the REFUNDED payment → must be 409
        Response secondRefundResponse =
            given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("{\"amount\":10000}")
            .when().post("/api/payments/" + paymentId + "/refund");

        assertThat(secondRefundResponse.statusCode())
            .as("Refund-of-refund on REFUNDED payment must return 409")
            .isEqualTo(409);

        // RFC 7807: body must reference the refund-of-refund problem type
        String body = secondRefundResponse.body().asString();
        assertThat(body)
            .as("409 body must contain RFC 7807 ProblemDetail for refund-of-refund")
            .satisfiesAnyOf(
                b -> assertThat(b).containsIgnoringCase("refund"),
                b -> assertThat(b).containsIgnoringCase("REFUNDED"),
                b -> assertThat(b).containsIgnoringCase("already"),
                b -> assertThat(b).containsIgnoringCase("transition")
            );
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    /**
     * Creates a payment and walks it to CAPTURED state.
     * Returns the payment ID. In RED state returns null (endpoints missing).
     */
    private String createCapturedPayment(String token, String orderId, int amount) {
        String idempotencyKey = UUID.randomUUID().toString();

        Response createResponse =
            given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", idempotencyKey)
                .body("{\"amount\":" + amount + ",\"currency\":\"KRW\",\"orderId\":\"" + orderId + "\"}")
            .when().post("/api/payments");

        String paymentId = createResponse.then().extract().path("id");

        given()
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .body("{}")
        .when().post("/api/payments/" + paymentId + "/authorize");

        given()
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .body("{}")
        .when().post("/api/payments/" + paymentId + "/capture");

        return paymentId;
    }

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
