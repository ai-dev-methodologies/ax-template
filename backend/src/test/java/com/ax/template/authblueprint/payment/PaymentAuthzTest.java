package com.ax.template.authblueprint.payment;

import io.restassured.RestAssured;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Black-box compliance tests for PAYMENT-AUTHZ-001..004.
 *
 * RED phase: all tests fail because PaymentController / payment endpoints
 * do not exist yet. Failures manifest as HTTP 404 (endpoint missing) or
 * connection-refused assertions — both are valid RED per /tdd-workflow.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("PAYMENT")
class PaymentAuthzTest {

    @LocalServerPort
    int port;

    // ─── PAYMENT-AUTHZ-001 ────────────────────────────────────────────────────

    /**
     * PAYMENT-AUTHZ-001: All payment endpoints require authentication;
     * unauthenticated requests are rejected with HTTP 401.
     *
     * Tests all 5 payment endpoints: POST /api/payments,
     * GET /api/payments, GET /api/payments/{id},
     * POST /api/payments/{id}/refund, POST /api/payments/{id}/void.
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-AUTHZ-001")
    void authRequired_authenticationMissingReturns401() {
        String fakeId = UUID.randomUUID().toString();

        // POST /api/payments — no auth → 401
        assertThat(
            given()
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("{\"amount\":10000,\"currency\":\"KRW\",\"orderId\":\"order-1\"}")
            .when().post("/api/payments")
            .then().extract().statusCode()
        ).as("POST /api/payments without auth must return 401").isEqualTo(401);

        // GET /api/payments — no auth → 401
        assertThat(
            given().when().get("/api/payments")
            .then().extract().statusCode()
        ).as("GET /api/payments without auth must return 401").isEqualTo(401);

        // GET /api/payments/{id} — no auth → 401
        assertThat(
            given().when().get("/api/payments/" + fakeId)
            .then().extract().statusCode()
        ).as("GET /api/payments/{id} without auth must return 401").isEqualTo(401);

        // POST /api/payments/{id}/refund — no auth → 401
        assertThat(
            given()
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("{\"amount\":10000}")
            .when().post("/api/payments/" + fakeId + "/refund")
            .then().extract().statusCode()
        ).as("POST /api/payments/{id}/refund without auth must return 401").isEqualTo(401);

        // POST /api/payments/{id}/void — no auth → 401
        assertThat(
            given()
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("{}")
            .when().post("/api/payments/" + fakeId + "/void")
            .then().extract().statusCode()
        ).as("POST /api/payments/{id}/void without auth must return 401").isEqualTo(401);
    }

    // ─── PAYMENT-AUTHZ-002 ────────────────────────────────────────────────────

    /**
     * PAYMENT-AUTHZ-002: Refund authority — owner can refund their own payment.
     *
     * Positive case: the authenticated user who created the payment
     * can successfully POST /api/payments/{id}/refund.
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-AUTHZ-002")
    void refundAuthority_ownerCanRefundOwnPayment() {
        // Step 1: Create a payment as userA
        String ownerToken = obtainToken("owner@authz002.test", "MEMBER");
        String idempotencyKey = UUID.randomUUID().toString();

        int createStatus =
            given()
                .header("Authorization", "Bearer " + ownerToken)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", idempotencyKey)
                .body("{\"amount\":10000,\"currency\":\"KRW\",\"orderId\":\"order-authz002\"}")
            .when().post("/api/payments")
            .then().extract().statusCode();

        assertThat(createStatus)
            .as("Owner creating a payment must return 201")
            .isEqualTo(201);

        String paymentId =
            given()
                .header("Authorization", "Bearer " + ownerToken)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", idempotencyKey)
                .body("{\"amount\":10000,\"currency\":\"KRW\",\"orderId\":\"order-authz002\"}")
            .when().post("/api/payments")
            .then().extract().path("id");

        // Step 2: Owner refunds their own payment — must succeed (not 403/404)
        int refundStatus =
            given()
                .header("Authorization", "Bearer " + ownerToken)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("{\"amount\":10000}")
            .when().post("/api/payments/" + paymentId + "/refund")
            .then().extract().statusCode();

        assertThat(refundStatus)
            .as("Owner refunding own payment must not return 403 or 404")
            .isNotIn(403, 404);
    }

    // ─── PAYMENT-AUTHZ-002 (negative) / PAYMENT-AUTHZ-003 ────────────────────

    /**
     * PAYMENT-AUTHZ-002 (negative) + PAYMENT-AUTHZ-003:
     * Non-owner refund attempt returns 404 (IDOR-safe, not 403).
     *
     * Returning 404 instead of 403 prevents enumeration of existing payment IDs.
     * Reference: OWASP ASVS V4.2.1.
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-AUTHZ-002")
    @Tag("PAYMENT-AUTHZ-003")
    void refundAuthority_nonOwnerCannotRefund_returns404NotIdor() {
        String ownerToken = obtainToken("owner@authz003.test", "MEMBER");
        String otherToken = obtainToken("other@authz003.test", "MEMBER");
        String idempotencyKey = UUID.randomUUID().toString();

        // Owner creates a payment
        String paymentId =
            given()
                .header("Authorization", "Bearer " + ownerToken)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", idempotencyKey)
                .body("{\"amount\":10000,\"currency\":\"KRW\",\"orderId\":\"order-authz003\"}")
            .when().post("/api/payments")
            .then().extract().path("id");

        // Different user attempts refund → must be 404 (not 403, IDOR-safe)
        int refundStatus =
            given()
                .header("Authorization", "Bearer " + otherToken)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("{\"amount\":10000}")
            .when().post("/api/payments/" + paymentId + "/refund")
            .then().extract().statusCode();

        assertThat(refundStatus)
            .as("Non-owner refund must return 404 (IDOR-safe), not 403")
            .isEqualTo(404);
    }

    /**
     * PAYMENT-AUTHZ-003: Cross-user GET payment returns 404, not 403.
     *
     * UserB querying UserA's payment must receive 404 to prevent
     * payment ID enumeration.
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-AUTHZ-003")
    void crossUserPaymentQueryDenied_returns404() {
        String ownerToken = obtainToken("owner@authz003b.test", "MEMBER");
        String otherToken = obtainToken("other@authz003b.test", "MEMBER");
        String idempotencyKey = UUID.randomUUID().toString();

        // Owner creates a payment
        String paymentId =
            given()
                .header("Authorization", "Bearer " + ownerToken)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", idempotencyKey)
                .body("{\"amount\":10000,\"currency\":\"KRW\",\"orderId\":\"order-authz003b\"}")
            .when().post("/api/payments")
            .then().extract().path("id");

        // Other user queries owner's payment → 404 (not 403)
        int getStatus =
            given()
                .header("Authorization", "Bearer " + otherToken)
            .when().get("/api/payments/" + paymentId)
            .then().extract().statusCode();

        assertThat(getStatus)
            .as("Cross-user GET must return 404 (IDOR-safe), not 403")
            .isEqualTo(404);
    }

    // ─── PAYMENT-AUTHZ-004 ────────────────────────────────────────────────────

    /**
     * PAYMENT-AUTHZ-004: Admin override is audited.
     *
     * Positive: admin can refund any payment regardless of ownership AND
     * a payment_events ledger row with type=ADMIN_OVERRIDE is created.
     * Negative: regular user cannot access the admin endpoint.
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-AUTHZ-004")
    void adminOverrideAudited() {
        String ownerToken = obtainToken("owner@authz004.test", "MEMBER");
        String adminToken = obtainToken("admin@authz004.test", "ADMIN");
        String memberToken = obtainToken("member@authz004.test", "MEMBER");
        String idempotencyKey = UUID.randomUUID().toString();

        // Owner creates a payment
        String paymentId =
            given()
                .header("Authorization", "Bearer " + ownerToken)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", idempotencyKey)
                .body("{\"amount\":10000,\"currency\":\"KRW\",\"orderId\":\"order-authz004\"}")
            .when().post("/api/payments")
            .then().extract().path("id");

        // Regular member cannot use admin-force-void endpoint → 403
        int memberStatus =
            given()
                .header("Authorization", "Bearer " + memberToken)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("{\"justification\":\"fraud\"}")
            .when().post("/api/admin/payments/" + paymentId + "/force-void")
            .then().extract().statusCode();

        assertThat(memberStatus)
            .as("Regular member must not access admin force-void endpoint")
            .isEqualTo(403);

        // Admin can perform force-void and an audit ledger row is created
        int adminStatus =
            given()
                .header("Authorization", "Bearer " + adminToken)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("{\"justification\":\"fraud confirmed by support ticket #123\"}")
            .when().post("/api/admin/payments/" + paymentId + "/force-void")
            .then().extract().statusCode();

        assertThat(adminStatus)
            .as("Admin force-void must succeed (2xx)")
            .isBetween(200, 299);

        // Audit ledger: a payment_events row with type=ADMIN_OVERRIDE must exist
        // Verified via GET /api/admin/payments/{id}/events — ADMIN_OVERRIDE event present
        String eventType =
            given()
                .header("Authorization", "Bearer " + adminToken)
            .when().get("/api/admin/payments/" + paymentId + "/events")
            .then().extract().path("find { it.type == 'ADMIN_OVERRIDE' }.type");

        assertThat(eventType)
            .as("Admin override must create an ADMIN_OVERRIDE ledger event")
            .isEqualTo("ADMIN_OVERRIDE");
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    /**
     * Obtain a JWT for a test user. In RED state this will return a token
     * stub or fail — both are acceptable RED outcomes. In GREEN state (P3.0)
     * this will create the user and return a real JWT via the auth endpoint.
     */
    private String obtainToken(String email, String role) {
        // Register user (idempotent — ignore 409)
        given()
            .header("Content-Type", "application/json")
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\",\"role\":\"" + role + "\"}")
        .when().post("/api/auth/email/signup");

        // Login and extract token
        return given()
            .header("Content-Type", "application/json")
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\"}")
        .when().post("/api/auth/email/login")
        .then().extract().path("accessToken");
    }
}
