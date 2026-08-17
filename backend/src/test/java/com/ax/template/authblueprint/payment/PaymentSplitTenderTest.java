package com.ax.template.authblueprint.payment;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.math.BigDecimal;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Black-box compliance tests for PAYMENT-SPLIT-001 (split-tender coverage).
 *
 * Invariant: an order MUST NOT be confirmable until the SUM of active,
 * successfully-authorized tender amounts (AUTHORIZED + CAPTURED) for that
 * orderId and currency covers the order total. Under-covered → HTTP 422,
 * type "urn:ax:payment:tenders-underfunded", with shortfall.
 *
 * Spec: specs/payment-l0.yaml#PAYMENT-SPLIT-001
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("PAYMENT")
class PaymentSplitTenderTest {

    @LocalServerPort
    int port;

    /**
     * PAYMENT-SPLIT-001 (underfunded): one tender of 6000 against a 10000 order
     * returns 422 with problem type tenders-underfunded and shortfall == 4000.
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-SPLIT-001")
    void splitTender_singleTenderUnderfunds_returns422WithShortfall() {
        String token = obtainToken("split001a@test.test", "MEMBER");
        String orderId = "order-split-" + UUID.randomUUID();

        // Create tender A = 6000 KRW for orderId, walk to AUTHORIZED state
        authorizePayment(token, orderId, 6000, "KRW");

        // Confirm coverage against orderTotal = 10000 KRW → must be 422 (shortfall = 4000)
        Response coverage =
            given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .body("{\"orderId\":\"" + orderId + "\",\"currency\":\"KRW\",\"orderTotal\":10000}")
            .when().post("/api/payments/coverage/confirm");

        assertThat(coverage.statusCode())
            .as("Coverage check with one 6000 KRW tender against 10000 order must return 422")
            .isEqualTo(422);

        String body = coverage.body().asString();
        assertThat(body)
            .as("422 response must contain RFC 7807 type urn:ax:payment:tenders-underfunded")
            .contains("urn:ax:payment:tenders-underfunded");

        // Shortfall must be 4000 (exact integer minor-unit check)
        Object shortfall = coverage.then().extract().path("shortfall");
        assertThat(shortfall).isNotNull();
        assertThat(new BigDecimal(shortfall.toString()).intValue())
            .as("Shortfall must be exactly 4000")
            .isEqualTo(4000);
    }

    /**
     * PAYMENT-SPLIT-001 (fully covered): two tenders of 6000 + 4000 = 10000 KRW
     * against a 10000 order returns 200 with covered == 10000.
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-SPLIT-001")
    void splitTender_twoTendersCoverFull_returns200() {
        String token = obtainToken("split001b@test.test", "MEMBER");
        String orderId = "order-split-" + UUID.randomUUID();

        // Tender A = 6000 KRW (AUTHORIZED)
        authorizePayment(token, orderId, 6000, "KRW");
        // Tender B = 4000 KRW (AUTHORIZED) — same orderId
        authorizePayment(token, orderId, 4000, "KRW");

        // Confirm coverage against orderTotal = 10000 KRW → must be 200
        Response coverage =
            given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .body("{\"orderId\":\"" + orderId + "\",\"currency\":\"KRW\",\"orderTotal\":10000}")
            .when().post("/api/payments/coverage/confirm");

        assertThat(coverage.statusCode())
            .as("Coverage check with 6000+4000 KRW tenders against 10000 order must return 200")
            .isEqualTo(200);

        Object covered = coverage.then().extract().path("covered");
        assertThat(covered).isNotNull();
        assertThat(new BigDecimal(covered.toString()).intValue())
            .as("Covered must be exactly 10000")
            .isEqualTo(10000);
    }

    /**
     * PAYMENT-SPLIT-001 (negative guard — voided excluded): a VOIDED tender does NOT
     * count toward coverage. Adding a voided 4000 KRW tender does not satisfy the shortfall.
     *
     * Flow: tender A = 6000 AUTHORIZED + tender B = 4000 AUTHORIZED then VOIDED →
     * coverage confirm against 10000 still returns 422 (only 6000 counts).
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-SPLIT-001")
    void splitTender_voidedTenderExcluded_stillUnderfunded() {
        String token = obtainToken("split001c@test.test", "MEMBER");
        String orderId = "order-split-" + UUID.randomUUID();

        // Tender A = 6000 KRW → AUTHORIZED (counts)
        authorizePayment(token, orderId, 6000, "KRW");

        // Tender B = 4000 KRW → create, authorize, then void (must not count)
        String tenderBId = createPayment(token, orderId, 4000, "KRW");
        authorizeById(token, tenderBId);
        voidById(token, tenderBId);

        // Confirm coverage against orderTotal = 10000 KRW → still 422 (voided tender excluded)
        Response coverage =
            given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .body("{\"orderId\":\"" + orderId + "\",\"currency\":\"KRW\",\"orderTotal\":10000}")
            .when().post("/api/payments/coverage/confirm");

        assertThat(coverage.statusCode())
            .as("VOIDED tender must not count toward coverage; 422 expected with shortfall=4000")
            .isEqualTo(422);

        Object shortfall = coverage.then().extract().path("shortfall");
        assertThat(shortfall).isNotNull();
        assertThat(new BigDecimal(shortfall.toString()).intValue())
            .as("Shortfall must still be 4000 because the voided tender is excluded")
            .isEqualTo(4000);
    }

    /**
     * PAYMENT-SPLIT-001 (CAPTURED state counts): a CAPTURED tender counts toward
     * coverage just like AUTHORIZED.
     *
     * Tender A = 6000 KRW AUTHORIZED → CAPTURED; tender B = 4000 KRW AUTHORIZED.
     * Confirm coverage {orderId, KRW, 10000} → 200, covered == 10000.
     * Proves the CAPTURED branch of the state IN-clause.
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-SPLIT-001")
    void splitTender_capturedTenderCountsTowardCoverage() {
        String token = obtainToken("split001d@test.test", "MEMBER");
        String orderId = "order-split-" + UUID.randomUUID();

        // Tender A = 6000 KRW → AUTHORIZED then CAPTURED
        String tenderAId = createPayment(token, orderId, 6000, "KRW");
        authorizeById(token, tenderAId);
        captureById(token, tenderAId);

        // Tender B = 4000 KRW → AUTHORIZED
        authorizePayment(token, orderId, 4000, "KRW");

        // Confirm coverage against 10000 KRW → must be 200, covered == 10000
        Response coverage =
            given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .body("{\"orderId\":\"" + orderId + "\",\"currency\":\"KRW\",\"orderTotal\":10000}")
            .when().post("/api/payments/coverage/confirm");

        assertThat(coverage.statusCode())
            .as("CAPTURED tender must count toward coverage; 200 expected")
            .isEqualTo(200);

        Object covered = coverage.then().extract().path("covered");
        assertThat(covered).isNotNull();
        assertThat(new BigDecimal(covered.toString()).intValue())
            .as("Covered must be exactly 10000 (6000 CAPTURED + 4000 AUTHORIZED)")
            .isEqualTo(10000);
    }

    /**
     * PAYMENT-SPLIT-001 (currency isolation): a tender in a different currency must NOT
     * count toward coverage denominated in the order's currency.
     *
     * Tender A = 6000 KRW AUTHORIZED + tender B = 5000 USD AUTHORIZED (same orderId).
     * Confirm coverage {orderId, KRW, 10000} → still 422, shortfall == 4000 (USD excluded).
     */
    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-SPLIT-001")
    void splitTender_differentCurrencyTenderExcluded_stillUnderfunded() {
        String token = obtainToken("split001e@test.test", "MEMBER");
        String orderId = "order-split-" + UUID.randomUUID();

        // Tender A = 6000 KRW → AUTHORIZED (counts toward KRW coverage)
        authorizePayment(token, orderId, 6000, "KRW");

        // Tender B = 5000 USD → AUTHORIZED (must NOT count toward KRW coverage)
        authorizePayment(token, orderId, 5000, "USD");

        // Confirm KRW coverage against 10000 KRW → must be 422 (USD tender excluded)
        Response coverage =
            given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .body("{\"orderId\":\"" + orderId + "\",\"currency\":\"KRW\",\"orderTotal\":10000}")
            .when().post("/api/payments/coverage/confirm");

        assertThat(coverage.statusCode())
            .as("USD tender must not count toward KRW coverage; 422 expected with shortfall=4000")
            .isEqualTo(422);

        Object shortfall = coverage.then().extract().path("shortfall");
        assertThat(shortfall).isNotNull();
        assertThat(new BigDecimal(shortfall.toString()).intValue())
            .as("Shortfall must be 4000 — only the 6000 KRW tender counts, USD is excluded")
            .isEqualTo(4000);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    /** Creates a payment for the given orderId, amount, currency and walks it to AUTHORIZED. */
    private void authorizePayment(String token, String orderId, int amount, String currency) {
        String id = createPayment(token, orderId, amount, currency);
        authorizeById(token, id);
    }

    /** Creates a payment and returns its id. */
    private String createPayment(String token, String orderId, int amount, String currency) {
        Response resp =
            given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("{\"amount\":" + amount + ",\"currency\":\"" + currency
                    + "\",\"orderId\":\"" + orderId + "\"}")
            .when().post("/api/payments");

        assertThat(resp.statusCode())
            .as("Payment creation for orderId=" + orderId + " amount=" + amount
                + " currency=" + currency + " must succeed")
            .isIn(200, 201);
        return resp.then().extract().path("id");
    }

    /** Walks an existing payment (by id) to AUTHORIZED state. */
    private void authorizeById(String token, String paymentId) {
        int status =
            given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
            .when().post("/api/payments/" + paymentId + "/authorize")
            .then().extract().statusCode();

        assertThat(status)
            .as("Authorize payment id=" + paymentId + " must succeed")
            .isIn(200, 201);
    }

    /** Walks an existing AUTHORIZED payment (by id) to CAPTURED state. */
    private void captureById(String token, String paymentId) {
        int status =
            given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
            .when().post("/api/payments/" + paymentId + "/capture")
            .then().extract().statusCode();

        assertThat(status)
            .as("Capture payment id=" + paymentId + " must succeed")
            .isIn(200, 201);
    }

    /** Voids an existing payment (by id). */
    private void voidById(String token, String paymentId) {
        int status =
            given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
            .when().post("/api/payments/" + paymentId + "/void")
            .then().extract().statusCode();

        assertThat(status)
            .as("Void payment id=" + paymentId + " must succeed")
            .isIn(200, 201);
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
