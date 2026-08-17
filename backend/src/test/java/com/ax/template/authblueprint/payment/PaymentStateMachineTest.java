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
 * Black-box compliance tests for PAYMENT-STATE-001..003.
 *
 * RED phase: all tests fail because PaymentController / PaymentStateMachine /
 * state transition endpoints do not exist yet. Failures manifest as HTTP 404
 * (endpoint missing) or assertion failures — both are valid RED per /tdd-workflow.
 *
 * Spec: specs/payment-l0.yaml STATE family.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("PAYMENT")
class PaymentStateMachineTest {

    @LocalServerPort
    int port;

    // ─── PAYMENT-STATE-001 (legal transition: AUTHORIZED → CAPTURED) ─────────

    /**
     * PAYMENT-STATE-001: Legal transition CREATED → AUTHORIZED → CAPTURED succeeds.
     *
     * Creates a payment, authorizes it, then captures it.
     * Final state must be CAPTURED.
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-STATE-001")
    void stateMachine_legalTransition_authorizedToCaptured() {
        String token = obtainToken("state001a@test.test", "MEMBER");
        String idempotencyKey = UUID.randomUUID().toString();

        // Step 1: Create payment → state CREATED
        Response createResponse =
            given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", idempotencyKey)
                .body("{\"amount\":10000,\"currency\":\"KRW\",\"orderId\":\"order-state001a\"}")
            .when().post("/api/payments");

        assertThat(createResponse.statusCode())
            .as("Create payment must return 201")
            .isEqualTo(201);

        String paymentId = createResponse.then().extract().path("id");
        assertThat(paymentId).isNotNull();

        // Step 2: Authorize payment → state AUTHORIZED
        int authorizeStatus =
            given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("{}")
            .when().post("/api/payments/" + paymentId + "/authorize")
            .then().extract().statusCode();

        assertThat(authorizeStatus)
            .as("Authorize payment must return 200 or 201")
            .isIn(200, 201);

        // Step 3: Capture payment → state CAPTURED
        Response captureResponse =
            given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("{}")
            .when().post("/api/payments/" + paymentId + "/capture");

        assertThat(captureResponse.statusCode())
            .as("Capture payment must return 200 or 201")
            .isIn(200, 201);

        String finalState = captureResponse.then().extract().path("state");
        assertThat(finalState)
            .as("Final state after capture must be CAPTURED")
            .isEqualTo("CAPTURED");
    }

    // ─── PAYMENT-STATE-001 (legal transition: AUTHORIZED → VOIDED) ───────────

    /**
     * PAYMENT-STATE-001: Legal transition CREATED → AUTHORIZED → VOIDED succeeds.
     *
     * Creates a payment, authorizes it, then voids it.
     * Final state must be VOIDED.
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-STATE-001")
    void stateMachine_legalTransition_authorizedToVoided() {
        String token = obtainToken("state001b@test.test", "MEMBER");
        String idempotencyKey = UUID.randomUUID().toString();

        // Step 1: Create payment
        Response createResponse =
            given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", idempotencyKey)
                .body("{\"amount\":10000,\"currency\":\"KRW\",\"orderId\":\"order-state001b\"}")
            .when().post("/api/payments");

        assertThat(createResponse.statusCode())
            .as("Create payment must return 201")
            .isEqualTo(201);

        String paymentId = createResponse.then().extract().path("id");

        // Step 2: Authorize payment
        given()
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .body("{}")
        .when().post("/api/payments/" + paymentId + "/authorize");

        // Step 3: Void payment → state VOIDED
        Response voidResponse =
            given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("{}")
            .when().post("/api/payments/" + paymentId + "/void");

        assertThat(voidResponse.statusCode())
            .as("Void payment must return 200 or 201")
            .isIn(200, 201);

        String finalState = voidResponse.then().extract().path("state");
        assertThat(finalState)
            .as("Final state after void must be VOIDED")
            .isEqualTo("VOIDED");
    }

    // ─── PAYMENT-STATE-003: illegal transition rejected with 409 ─────────────

    /**
     * PAYMENT-STATE-003: Illegal transition REFUNDED → CAPTURED returns 409.
     *
     * After a payment is REFUNDED, attempting to re-capture it must be
     * rejected with 409 and an RFC 7807 ProblemDetail body containing
     * currentState and attemptedEvent extension fields.
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-STATE-003")
    void stateMachine_illegalTransition_refundedToCaptured_returns409() {
        String token = obtainToken("state003a@test.test", "MEMBER");

        // Walk payment to REFUNDED state: create → authorize → capture → refund
        String paymentId = createCapturedPayment(token, "order-state003a", 10000);

        // Refund the captured payment
        given()
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .body("{\"amount\":10000}")
        .when().post("/api/payments/" + paymentId + "/refund");

        // Attempt to re-capture the REFUNDED payment → must return 409
        Response illegalCapture =
            given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("{}")
            .when().post("/api/payments/" + paymentId + "/capture");

        assertThat(illegalCapture.statusCode())
            .as("Illegal REFUNDED → CAPTURED transition must return 409")
            .isEqualTo(409);

        // RFC 7807: response body must contain state machine context
        String body = illegalCapture.body().asString();
        assertThat(body)
            .as("409 body must contain RFC 7807 ProblemDetail with state context")
            .satisfiesAnyOf(
                b -> assertThat(b).containsIgnoringCase("REFUNDED"),
                b -> assertThat(b).containsIgnoringCase("illegal"),
                b -> assertThat(b).containsIgnoringCase("transition")
            );
    }

    // ─── PAYMENT-STATE-002: optimistic locking prevents lost update ───────────

    /**
     * PAYMENT-STATE-002: Two concurrent state transitions on the same AUTHORIZED
     * payment — the second one with a stale @Version must fail.
     *
     * Tests that JPA optimistic locking (@Version) prevents concurrent state
     * transitions from both succeeding (i.e., prevents lost update on payment state).
     *
     * In RED state this fails because Payment entity / endpoints do not exist.
     * The concurrent case (two separate HTTP requests with stale version) is
     * approximated here via sequential calls; full race coverage is in P3.6
     * (US-011 concurrency tests).
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-STATE-002")
    void stateAtomic_optimisticLockingPreventsLostUpdate() {
        String token = obtainToken("state002@test.test", "MEMBER");
        String idempotencyKey = UUID.randomUUID().toString();

        // Create and authorize a payment
        Response createResponse =
            given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", idempotencyKey)
                .body("{\"amount\":10000,\"currency\":\"KRW\",\"orderId\":\"order-state002\"}")
            .when().post("/api/payments");

        assertThat(createResponse.statusCode())
            .as("Create payment must return 201")
            .isEqualTo(201);

        String paymentId = createResponse.then().extract().path("id");

        given()
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .body("{}")
        .when().post("/api/payments/" + paymentId + "/authorize");

        // First capture with current (correct) version → should succeed
        Response firstCapture =
            given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("{}")
            .when().post("/api/payments/" + paymentId + "/capture");

        assertThat(firstCapture.statusCode())
            .as("First capture must succeed (200 or 201)")
            .isIn(200, 201);

        // Second capture attempt on already-CAPTURED payment → 409 (stale version / wrong state)
        // This simulates the optimistic lock loser: once the state changed, a concurrent
        // in-flight transaction with an old @Version would get a 409 on retry.
        Response secondCapture =
            given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("{}")
            .when().post("/api/payments/" + paymentId + "/capture");

        assertThat(secondCapture.statusCode())
            .as("Second capture on already-CAPTURED payment must return 409 (optimistic lock / illegal transition)")
            .isEqualTo(409);
    }

    // ─── PAYMENT-STATE-003: CAPTURED → AUTHORIZED is illegal ─────────────────

    /**
     * PAYMENT-STATE-003: Once CAPTURED, the payment cannot revert to AUTHORIZED.
     *
     * Illegal backward transition must return 409 with RFC 7807 body.
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-STATE-003")
    void stateAtomic_illegalTransitionRejected_capturedToAuthorized_returns409() {
        String token = obtainToken("state003b@test.test", "MEMBER");

        // Walk payment to CAPTURED state
        String paymentId = createCapturedPayment(token, "order-state003b", 10000);

        // Attempt to re-authorize a CAPTURED payment → must be 409
        Response illegalAuthorize =
            given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("{}")
            .when().post("/api/payments/" + paymentId + "/authorize");

        assertThat(illegalAuthorize.statusCode())
            .as("Illegal CAPTURED → AUTHORIZED transition must return 409")
            .isEqualTo(409);

        String body = illegalAuthorize.body().asString();
        assertThat(body)
            .as("409 body must reference the illegal state transition per RFC 7807")
            .satisfiesAnyOf(
                b -> assertThat(b).containsIgnoringCase("CAPTURED"),
                b -> assertThat(b).containsIgnoringCase("illegal"),
                b -> assertThat(b).containsIgnoringCase("transition"),
                b -> assertThat(b).containsIgnoringCase("state")
            );
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    /**
     * Creates a payment and walks it to CAPTURED state:
     * create → authorize → capture.
     *
     * Returns the payment ID. In RED state this returns null (endpoint missing).
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
