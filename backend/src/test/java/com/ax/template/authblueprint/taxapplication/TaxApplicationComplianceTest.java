package com.ax.template.authblueprint.taxapplication;

import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * Behavioral compliance tests for tax-application-l0.yaml (3 families) — black-box HTTP via
 * RestAssured, real round-trip (signup → login → create taxable order → recompute → read tax).
 *
 * <p>TAX-IDEMPOTENT-RECOMPUTE-001 (re-price converges to exactly ONE tax row == correct amount),
 * TAX-EXEMPT-SKIP-001 (exempt customer / exempt line ⇒ 0 tax for that scope), TAX-AUTHZ-001
 * (ADMIN defines/declares/recomputes, authenticated reads).
 *
 * <p>BEFORE_CLASS dirties-context: the suite has many @SpringBootTest contexts vs a 32-entry
 * cache; a fresh context avoids LRU-eviction flake.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Tag("TAX_APPLICATION")
class TaxApplicationComplianceTest {

    @LocalServerPort int port;

    @BeforeEach
    void setup() {
        TaxApplicationTestSupport.useRandomPort(port);
    }

    // ─── IDEMPOTENT-RECOMPUTE family ─────────────────────────────────────────

    /**
     * TAX-IDEMPOTENT-RECOMPUTE-001: recomputing a taxable order twice leaves exactly ONE tax record
     * — the SAME assessment id, same amount == round(base × rate). The stable assessment id across
     * recomputes proves the row was UPDATED in place (not duplicated), and UNIQUE(order_id) forbids
     * a second row. base 10000 minor units × 1000 bp (10%) ⇒ tax 1000.
     */
    @Test
    @Tag("TAX-IDEMPOTENT-RECOMPUTE-001")
    void recomputeTwice_convergesToOneRecord_sameIdSameAmount() {
        String admin = adminToken("idem-admin");
        String reader = memberToken("idem-read");

        String orderId = createOrder(admin, """
            {"customerExempt":false,"lines":[{"taxableBaseMinor":10000,"exempt":false}]}
            """);

        // First recompute → creates exactly one row.
        String firstId = recompute(admin, orderId, 1000)
            .body("present", Matchers.equalTo(true))
            .body("taxAmountMinor", Matchers.equalTo(1000))
            .body("orderId", Matchers.equalTo(orderId))
            .extract().path("assessmentId");

        // Second recompute (idempotent) → SAME row id, SAME amount — not duplicated.
        recompute(admin, orderId, 1000)
            .body("present", Matchers.equalTo(true))
            .body("taxAmountMinor", Matchers.equalTo(1000))
            .body("assessmentId", Matchers.equalTo(firstId));

        // The read surface confirms the single converged amount.
        readTax(reader, orderId)
            .body("present", Matchers.equalTo(true))
            .body("taxAmountMinor", Matchers.equalTo(1000))
            .body("assessmentId", Matchers.equalTo(firstId));
    }

    /**
     * TAX-IDEMPOTENT-RECOMPUTE-001: a now-exempt re-price REMOVES the prior tax row (never strands
     * it). After removal a further recompute yields no row — no resurrection, no duplicate.
     */
    @Test
    @Tag("TAX-IDEMPOTENT-RECOMPUTE-001")
    void nowExemptReprice_removesPriorRow_noStrandedTax() {
        String admin = adminToken("rm-admin");
        String reader = memberToken("rm-read");

        String orderId = createOrder(admin, """
            {"customerExempt":false,"lines":[{"taxableBaseMinor":10000,"exempt":false}]}
            """);

        // Taxable → one row with amount 1000.
        recompute(admin, orderId, 1000)
            .body("present", Matchers.equalTo(true))
            .body("taxAmountMinor", Matchers.equalTo(1000));

        // Declare the customer exempt, then re-price → prior row removed.
        given().header("Authorization", "Bearer " + admin).contentType(ContentType.JSON)
            .body("{\"customerExempt\":true}")
        .when().post("/api/admin/tax-orders/" + orderId + "/exempt-customer")
        .then().statusCode(200)
            .body("customerExempt", Matchers.equalTo(true));

        recompute(admin, orderId, 1000)
            .body("present", Matchers.equalTo(false))
            .body("taxAmountMinor", Matchers.equalTo(0));

        // The combined tax now reads absent/zero — the old row is not stranded.
        readTax(reader, orderId)
            .body("present", Matchers.equalTo(false))
            .body("taxAmountMinor", Matchers.equalTo(0));

        // Re-running again still yields no row (no resurrection, no duplicate).
        recompute(admin, orderId, 1000)
            .body("present", Matchers.equalTo(false))
            .body("taxAmountMinor", Matchers.equalTo(0));
    }

    // ─── EXEMPT-SKIP family ──────────────────────────────────────────────────

    /**
     * TAX-EXEMPT-SKIP-001: an order whose customer is tax-exempt recomputes to ZERO tax / no tax
     * row — the whole taxable base is skipped regardless of line amounts.
     */
    @Test
    @Tag("TAX-EXEMPT-SKIP-001")
    void exemptCustomer_yieldsZeroTaxNoRow() {
        String admin = adminToken("ex-admin");
        String reader = memberToken("ex-read");

        String orderId = createOrder(admin, """
            {"customerExempt":true,"lines":[{"taxableBaseMinor":50000,"exempt":false}]}
            """);

        recompute(admin, orderId, 1000)
            .body("present", Matchers.equalTo(false))
            .body("taxAmountMinor", Matchers.equalTo(0));

        readTax(reader, orderId)
            .body("present", Matchers.equalTo(false))
            .body("taxAmountMinor", Matchers.equalTo(0));
    }

    /**
     * TAX-EXEMPT-SKIP-001: a tax-exempt LINE contributes 0 — an order with one exempt and one
     * non-exempt line is taxed on the non-exempt base only. non-exempt base 10000 × 1000 bp ⇒ 1000
     * (the exempt line's 99999 base is skipped, NOT taxed).
     */
    @Test
    @Tag("TAX-EXEMPT-SKIP-001")
    void exemptLine_contributesZero_onlyNonExemptBaseTaxed() {
        String admin = adminToken("el-admin");

        String orderId = createOrder(admin, """
            {"customerExempt":false,"lines":[
              {"taxableBaseMinor":10000,"exempt":false},
              {"taxableBaseMinor":99999,"exempt":true}]}
            """);

        recompute(admin, orderId, 1000)
            .body("present", Matchers.equalTo(true))
            .body("taxAmountMinor", Matchers.equalTo(1000));   // exempt line skipped — NOT 10999
    }

    // ─── AUTHZ family ────────────────────────────────────────────────────────

    /**
     * TAX-AUTHZ-001: defining a taxable order requires ROLE_ADMIN (MEMBER → 403, ADMIN → 201);
     * reading the combined tax requires a valid JWT (unauthenticated → 401/403); an unknown order
     * id → 404 problem+json.
     */
    @Test
    @Tag("TAX-AUTHZ-001")
    void authz_adminDefinesAuthenticatedReadsUnknownIs404() {
        String admin = adminToken("az-admin");
        String member = memberToken("az-member");
        String body = "{\"customerExempt\":false,\"lines\":[{\"taxableBaseMinor\":10000,\"exempt\":false}]}";

        // MEMBER cannot define a taxable order.
        given().header("Authorization", "Bearer " + member).contentType(ContentType.JSON).body(body)
        .when().post("/api/admin/tax-orders")
        .then().statusCode(403);

        // ADMIN can define.
        String orderId = createOrder(admin, body);

        // Unauthenticated read is rejected.
        given()
        .when().get("/api/tax-orders/" + orderId + "/tax")
        .then().statusCode(Matchers.anyOf(Matchers.is(401), Matchers.is(403)));

        // Authenticated read of an unknown id → 404 problem+json.
        given().header("Authorization", "Bearer " + member)
        .when().get("/api/tax-orders/" + UUID.randomUUID() + "/tax")
        .then().statusCode(404)
            .body("code", Matchers.equalTo("TAX_ORDER_NOT_FOUND"));
    }

    /** TAX-AUTHZ-001: a non-admin attempting to recompute is denied (403). */
    @Test
    @Tag("TAX-AUTHZ-001")
    void authz_memberCannotRecompute() {
        String admin = adminToken("rc-admin");
        String member = memberToken("rc-member");
        String orderId = createOrder(admin,
            "{\"customerExempt\":false,\"lines\":[{\"taxableBaseMinor\":10000,\"exempt\":false}]}");

        given().header("Authorization", "Bearer " + member).contentType(ContentType.JSON)
            .body("{\"rateBasisPoints\":1000}")
        .when().post("/api/admin/tax-orders/" + orderId + "/recompute")
        .then().statusCode(403);
    }

    /** TAX-AUTHZ-001: a negative injected rate is rejected at recompute time with 422 problem+json. */
    @Test
    @Tag("TAX-AUTHZ-001")
    void authz_negativeRateRejected() {
        String admin = adminToken("nr-admin");
        String orderId = createOrder(admin,
            "{\"customerExempt\":false,\"lines\":[{\"taxableBaseMinor\":10000,\"exempt\":false}]}");

        given().header("Authorization", "Bearer " + admin).contentType(ContentType.JSON)
            .body("{\"rateBasisPoints\":-1}")
        .when().post("/api/admin/tax-orders/" + orderId + "/recompute")
        .then().statusCode(422)
            .body("code", Matchers.equalTo("TAX_RATE_INVALID"));
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private String adminToken(String prefix) {
        return TaxApplicationTestSupport.obtainToken(TaxApplicationTestSupport.freshEmail(prefix), "ADMIN");
    }

    private String memberToken(String prefix) {
        return TaxApplicationTestSupport.obtainToken(TaxApplicationTestSupport.freshEmail(prefix), "MEMBER");
    }

    private String createOrder(String token, String body) {
        return given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON).body(body)
        .when().post("/api/admin/tax-orders")
        .then().statusCode(201)
            .extract().path("id");
    }

    private ValidatableResponse recompute(String token, String orderId, long rateBasisPoints) {
        return given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON)
            .body("{\"rateBasisPoints\":" + rateBasisPoints + "}")
        .when().post("/api/admin/tax-orders/" + orderId + "/recompute")
        .then().statusCode(200);
    }

    private ValidatableResponse readTax(String token, String orderId) {
        return given().header("Authorization", "Bearer " + token)
        .when().get("/api/tax-orders/" + orderId + "/tax")
        .then().statusCode(200);
    }
}
