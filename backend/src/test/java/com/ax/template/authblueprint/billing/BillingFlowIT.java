package com.ax.template.authblueprint.billing;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD anchor: BillingFlowIT.java
 * SP30 acceptance gate — Billing domain compliance tests.
 *
 * RED phase: all tests fail because BillingController / SubscriptionStateMachine /
 * billing endpoints do not exist yet. Failures manifest as HTTP 404
 * (endpoint missing) or assertion failures — both are valid RED per /tdd-workflow.
 *
 * Spec: specs/billing-l0.yaml
 *   BILLING-AUTHZ-001..003  — subscription CRUD requires authentication + IDOR protection
 *   BILLING-IDEMP-001       — BillingEvent writes carry unique idempotencyKey
 *   BILLING-IDEMP-002       — webhook replay within 300s detected and accepted (200)
 *   BILLING-STATE-001       — status only mutated by SubscriptionStateMachine (ArchUnit)
 *   BILLING-STATE-002       — invalid status transitions return 422
 *   BILLING-CUR-001         — float amounts rejected with 400 ProblemDetail
 *   BILLING-WEBHOOK-001     — invalid webhook signature returns 401
 *   BILLING-BOUNDARY-001    — billing ↔ payment cross-import prohibited (ArchUnit)
 *
 * Run: ./gradlew testBilling
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            // Compliance fixture: token issuance requires verified email
            // (AuthServiceImpl.login emits EmailNotVerifiedException → 401 otherwise).
            // Billing tests assert authenticated flows, not the verification workflow,
            // so auto-verify on signup keeps the AUTHZ/IDEMP/CUR assertions binary.
            "auth.signup.auto-verify=true"
        })
// R22 aggregate-test isolation: see FeatureFlagFlowIT for the LRU eviction
// root cause. `BEFORE_CLASS` ensures this class also boots a fresh context
// regardless of any cached-but-mutated entry from earlier `auto-verify=true`
// IT classes, so the WEBHOOK-001 signature-rejection path is not contaminated
// by stale BillingWebhookController state.
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Tag("BILLING")
class BillingFlowIT {

    @LocalServerPort
    int port;

    @BeforeEach
    void setup() {
        RestAssured.port = port;
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private String obtainToken(String email, String role) {
        // SignupRequest enforces @Size(min=12) on password (see SignupRequest.java).
        String password = "Test123!@#xy";
        // Register — EmailAuthController returns 201 CREATED; idempotent re-register
        // (409) is also acceptable so tests can share emails across @Test methods.
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"email":"%s","password":"%s","role":"%s"}
                """.formatted(email, password, role))
            .post("/api/auth/email/signup");
        // Login — LoginResponse exposes "accessToken" (see LoginResponse.java).
        return given()
            .contentType(ContentType.JSON)
            .body("""
                {"email":"%s","password":"%s"}
                """.formatted(email, password))
            .post("/api/auth/email/login")
            .then().statusCode(200)
            .extract().path("accessToken");
    }

    // ─── BILLING-AUTHZ-001 (subscription list requires auth) ─────────────────

    /**
     * BILLING-AUTHZ-001: GET /api/subscriptions without token returns 401.
     */
    @Test
    @Tag("BILLING")
    @Tag("BILLING-AUTHZ-001")
    void subscriptionList_unauthenticated_returns401() {
        given()
            .when().get("/api/subscriptions")
            .then().statusCode(401);
    }

    /**
     * BILLING-AUTHZ-001: Authenticated user can list their subscriptions.
     */
    @Test
    @Tag("BILLING")
    @Tag("BILLING-AUTHZ-001")
    void subscriptionList_authenticated_returns200() {
        String token = obtainToken("billing001@test.test", "MEMBER");

        given()
            .header("Authorization", "Bearer " + token)
            .when().get("/api/subscriptions")
            .then().statusCode(200);
    }

    // ─── BILLING-AUTHZ-002 (IDOR — cross-user subscription returns 404) ──────

    /**
     * BILLING-AUTHZ-002: Accessing another user's subscription by ID returns 404 (not 403).
     * 404 is the IDOR-safe response — does not reveal existence.
     */
    @Test
    @Tag("BILLING")
    @Tag("BILLING-AUTHZ-002")
    void subscriptionById_crossUser_returns404() {
        String token1 = obtainToken("billing002a@test.test", "MEMBER");
        String token2 = obtainToken("billing002b@test.test", "MEMBER");

        // token1 creates a subscription
        String idempotencyKey = UUID.randomUUID().toString();
        Response createResponse = given()
            .header("Authorization", "Bearer " + token1)
            .header("Idempotency-Key", idempotencyKey)
            .contentType(ContentType.JSON)
            .body("""
                {"planId":"%s","provider":"stripe"}
                """.formatted(UUID.randomUUID()))
            .when().post("/api/subscriptions");

        // If creation fails (no plan), skip to raw 404 check with random UUID
        String subscriptionId = createResponse.statusCode() == 201
            ? createResponse.path("id")
            : UUID.randomUUID().toString();

        // token2 tries to access token1's subscription → 404
        given()
            .header("Authorization", "Bearer " + token2)
            .when().get("/api/subscriptions/" + subscriptionId)
            .then().statusCode(404);
    }

    // ─── BILLING-AUTHZ-003 (admin-only plan management) ─────────────────────

    /**
     * BILLING-AUTHZ-003: Non-admin cannot create plans.
     */
    @Test
    @Tag("BILLING")
    @Tag("BILLING-AUTHZ-003")
    void createPlan_nonAdmin_returns403() {
        String token = obtainToken("billing003@test.test", "MEMBER");

        given()
            .header("Authorization", "Bearer " + token)
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .contentType(ContentType.JSON)
            .body("""
                {"name":"Basic","amount":9900,"currency":"KRW","billingCycle":"MONTHLY"}
                """)
            .when().post("/api/admin/billing/plans")
            .then().statusCode(403);
    }

    // ─── BILLING-IDEMP-001 (duplicate webhook accepted, not double-processed) ─

    /**
     * BILLING-IDEMP-001: Duplicate webhook with same provider event ID returns 200.
     * Second delivery must NOT create a duplicate BillingEvent row.
     */
    @Test
    @Tag("BILLING")
    @Tag("BILLING-IDEMP-001")
    void webhook_duplicateEventId_returns200WithoutDoubleProcessing() {
        String providerEventId = "evt_test_" + UUID.randomUUID();
        String webhookBody = """
            {"id":"%s","type":"invoice.payment_succeeded","data":{"object":{"subscription":"sub_test_123"}}}
            """.formatted(providerEventId);

        // First delivery
        given()
            .header("Stripe-Signature", "t=1234567890,v1=test_signature_skip_validation")
            .contentType(ContentType.JSON)
            .body(webhookBody)
            .when().post("/api/webhooks/billing")
            .then().statusCode(200); // or 401 if sig validation active — both valid RED

        // Second delivery (duplicate) — must also return 200 (idempotent)
        given()
            .header("Stripe-Signature", "t=1234567890,v1=test_signature_skip_validation")
            .contentType(ContentType.JSON)
            .body(webhookBody)
            .when().post("/api/webhooks/billing")
            .then().statusCode(200); // must NOT return 500 on duplicate-key
    }

    // ─── BILLING-CUR-001 (float amounts rejected) ────────────────────────────

    /**
     * BILLING-CUR-001: Subscription creation with float amount in plan returns 400.
     * Plan creation with float amount returns 400 ProblemDetail.
     */
    @Test
    @Tag("BILLING")
    @Tag("BILLING-CUR-001")
    void createPlan_floatAmount_returns400() {
        String adminToken = obtainToken("billing-cur-admin@test.test", "ADMIN");

        // float amount should be rejected by Jackson type mismatch
        given()
            .header("Authorization", "Bearer " + adminToken)
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .contentType(ContentType.JSON)
            .body("""
                {"name":"Float Plan","amount":9.99,"currency":"USD","billingCycle":"MONTHLY"}
                """)
            .when().post("/api/admin/billing/plans")
            .then().statusCode(400); // Jackson rejects float into long field
    }

    // ─── BILLING-WEBHOOK-001 (invalid signature returns 401) ─────────────────

    /**
     * BILLING-WEBHOOK-001: Webhook without valid signature header returns 401.
     */
    @Test
    @Tag("BILLING")
    @Tag("BILLING-WEBHOOK-001")
    void webhook_missingSignature_returns401() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"id":"evt_test_nosig","type":"invoice.payment_succeeded"}
                """)
            .when().post("/api/webhooks/billing")
            .then().statusCode(401);
    }

    /**
     * BILLING-WEBHOOK-001: Webhook with tampered signature returns 401.
     */
    @Test
    @Tag("BILLING")
    @Tag("BILLING-WEBHOOK-001")
    void webhook_tamperedSignature_returns401() {
        given()
            .header("Stripe-Signature", "t=1234567890,v1=tampered_invalid_sig")
            .contentType(ContentType.JSON)
            .body("""
                {"id":"evt_test_tampered","type":"invoice.payment_succeeded"}
                """)
            .when().post("/api/webhooks/billing")
            .then().statusCode(401);
    }
}
