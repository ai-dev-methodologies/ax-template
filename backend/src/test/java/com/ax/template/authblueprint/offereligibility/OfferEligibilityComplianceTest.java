package com.ax.template.authblueprint.offereligibility;

import io.restassured.http.ContentType;
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
 * Behavioral compliance tests for offer-eligibility-l0.yaml (4 families) — black-box HTTP via
 * RestAssured, real round-trip (signup → login → create offer → evaluate).
 *
 * <p>OFFER-QUALIFIER-MINQTY-001 (BOGO qualifier→target min-qty), OFFER-SEGMENT-ELIGIBILITY-001
 * (customer-xref allow-list OR matched segment), OFFER-FAIL-CLOSED-001 (deny by default on
 * missing/unknown criteria), OFFER-AUTHZ-001 (ADMIN defines, authenticated evaluates).
 *
 * <p>BEFORE_CLASS dirties-context: the suite has many @SpringBootTest contexts vs a 32-entry
 * cache; a fresh context avoids LRU-eviction flake.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Tag("OFFER_ELIGIBILITY")
class OfferEligibilityComplianceTest {

    @LocalServerPort int port;

    @BeforeEach
    void setup() {
        OfferEligibilityTestSupport.useRandomPort(port);
    }

    // ─── QUALIFIER-MINQTY family ─────────────────────────────────────────────

    /**
     * OFFER-QUALIFIER-MINQTY-001: an offer requiring qualifier qty >= 2 is NOT applied when the
     * qualifying line quantity is 1 (target NOT discounted — recorded NOT-APPLIED, HTTP 200), and
     * IS applied when the qualifying quantity reaches 2 with an eligible customer and a target line.
     */
    @Test
    @Tag("OFFER-QUALIFIER-MINQTY-001")
    void qualifierMinQty_belowThresholdNotApplied_atThresholdApplied() {
        String admin = adminToken("qm-admin");
        String evaluator = memberToken("qm-eval");
        UUID customer = UUID.randomUUID();

        String offerId = createOffer(admin, """
            {"name":"%s","qualifierSku":"Q","minQualifierQty":2,"targetSku":"T",
             "discountBasisPoints":1000,"eligibleCustomerIds":["%s"]}
            """.formatted(freshName("qm"), customer));

        // Qualifier qty = 1 (< min 2) → NOT applied, target not discounted, HTTP 200 (not an error).
        evaluate(evaluator, offerId, """
            {"customerId":"%s","lines":[{"sku":"Q","quantity":1},{"sku":"T","quantity":1}]}
            """.formatted(customer))
            .body("applied", Matchers.equalTo(false))
            .body("reason", Matchers.equalTo("QUALIFIER_MIN_QTY_NOT_MET"))
            .body("offerId", Matchers.equalTo(offerId));

        // Qualifier qty = 2 (>= min) + eligible customer + target line present → APPLIED.
        evaluate(evaluator, offerId, """
            {"customerId":"%s","lines":[{"sku":"Q","quantity":2},{"sku":"T","quantity":1}]}
            """.formatted(customer))
            .body("applied", Matchers.equalTo(true))
            .body("reason", Matchers.equalTo("ELIGIBLE"));
    }

    /** OFFER-QUALIFIER-MINQTY-001: an eligible customer with the qualifier met but NO target line → NO_TARGET_LINE. */
    @Test
    @Tag("OFFER-QUALIFIER-MINQTY-001")
    void qualifierMinQty_noTargetLineNotApplied() {
        String admin = adminToken("nt-admin");
        String evaluator = memberToken("nt-eval");
        UUID customer = UUID.randomUUID();

        String offerId = createOffer(admin, """
            {"name":"%s","qualifierSku":"Q","minQualifierQty":1,"targetSku":"T",
             "discountBasisPoints":500,"eligibleCustomerIds":["%s"]}
            """.formatted(freshName("nt"), customer));

        evaluate(evaluator, offerId, """
            {"customerId":"%s","lines":[{"sku":"Q","quantity":3}]}
            """.formatted(customer))
            .body("applied", Matchers.equalTo(false))
            .body("reason", Matchers.equalTo("NO_TARGET_LINE"));
    }

    // ─── SEGMENT-ELIGIBILITY family ──────────────────────────────────────────

    /**
     * OFFER-SEGMENT-ELIGIBILITY-001: an offer eligible to customerA (allow-list) AND to segment
     * "gold" applies to customerA, applies to any customer in segment "gold", and is NOT applied to
     * an unrelated customer in no matching segment (CUSTOMER_NOT_ELIGIBLE).
     */
    @Test
    @Tag("OFFER-SEGMENT-ELIGIBILITY-001")
    void segmentEligibility_allowListOrSegment_elseNotEligible() {
        String admin = adminToken("se-admin");
        String evaluator = memberToken("se-eval");
        UUID allowed = UUID.randomUUID();
        UUID segmented = UUID.randomUUID();
        UUID stranger = UUID.randomUUID();

        String offerId = createOffer(admin, """
            {"name":"%s","qualifierSku":"Q","minQualifierQty":1,"targetSku":"T",
             "discountBasisPoints":1500,"eligibleSegment":"gold","eligibleCustomerIds":["%s"]}
            """.formatted(freshName("se"), allowed));

        String qualifyingLines = "\"lines\":[{\"sku\":\"Q\",\"quantity\":2},{\"sku\":\"T\",\"quantity\":1}]";

        // Allow-listed customer → applied.
        evaluate(evaluator, offerId, "{\"customerId\":\"" + allowed + "\"," + qualifyingLines + "}")
            .body("applied", Matchers.equalTo(true))
            .body("reason", Matchers.equalTo("ELIGIBLE"));

        // Customer in segment "gold" (not on the allow-list) → applied via the segment path.
        evaluate(evaluator, offerId,
            "{\"customerId\":\"" + segmented + "\",\"customerSegments\":[\"gold\"]," + qualifyingLines + "}")
            .body("applied", Matchers.equalTo(true))
            .body("reason", Matchers.equalTo("ELIGIBLE"));

        // Unrelated customer, no matching segment → NOT applied.
        evaluate(evaluator, offerId,
            "{\"customerId\":\"" + stranger + "\",\"customerSegments\":[\"silver\"]," + qualifyingLines + "}")
            .body("applied", Matchers.equalTo(false))
            .body("reason", Matchers.equalTo("CUSTOMER_NOT_ELIGIBLE"));
    }

    // ─── FAIL-CLOSED family ──────────────────────────────────────────────────

    /**
     * OFFER-FAIL-CLOSED-001: an offer declared WITHOUT target criteria is NOT applied even to a fully
     * qualifying, eligible context (deny by default, reason MISSING_TARGET_CRITERIA) — a mis-declared
     * offer can never reach the discount path.
     */
    @Test
    @Tag("OFFER-FAIL-CLOSED-001")
    void failClosed_missingTargetCriteriaNeverApplies() {
        String admin = adminToken("fc-admin");
        String evaluator = memberToken("fc-eval");
        UUID customer = UUID.randomUUID();

        // No targetSku/targetTag declared — the offer is structurally incomplete.
        String offerId = createOffer(admin, """
            {"name":"%s","qualifierSku":"Q","minQualifierQty":1,
             "discountBasisPoints":2000,"eligibleCustomerIds":["%s"]}
            """.formatted(freshName("fc"), customer));

        evaluate(evaluator, offerId, """
            {"customerId":"%s","lines":[{"sku":"Q","quantity":5},{"sku":"T","quantity":1}]}
            """.formatted(customer))
            .body("applied", Matchers.equalTo(false))
            .body("reason", Matchers.equalTo("MISSING_TARGET_CRITERIA"));
    }

    /** OFFER-FAIL-CLOSED-001: an offer with NO eligibility criteria (empty allow-list, no segment) denies by default. */
    @Test
    @Tag("OFFER-FAIL-CLOSED-001")
    void failClosed_missingEligibilityCriteriaNeverApplies() {
        String admin = adminToken("fe-admin");
        String evaluator = memberToken("fe-eval");
        UUID customer = UUID.randomUUID();

        String offerId = createOffer(admin, """
            {"name":"%s","qualifierSku":"Q","minQualifierQty":1,"targetSku":"T","discountBasisPoints":1000}
            """.formatted(freshName("fe")));

        evaluate(evaluator, offerId, """
            {"customerId":"%s","lines":[{"sku":"Q","quantity":2},{"sku":"T","quantity":1}]}
            """.formatted(customer))
            .body("applied", Matchers.equalTo(false))
            .body("reason", Matchers.equalTo("MISSING_ELIGIBILITY_CRITERIA"));
    }

    // ─── AUTHZ family ────────────────────────────────────────────────────────

    /**
     * OFFER-AUTHZ-001: defining an offer requires ROLE_ADMIN (MEMBER → 403, ADMIN → 201);
     * evaluating requires a valid JWT (unauthenticated → 401/403); an unknown offer id → 404.
     */
    @Test
    @Tag("OFFER-AUTHZ-001")
    void authz_adminDefinesAuthenticatedEvaluatesUnknownIs404() {
        String admin = adminToken("az-admin");
        String member = memberToken("az-member");
        UUID customer = UUID.randomUUID();
        String body = """
            {"name":"%s","qualifierSku":"Q","minQualifierQty":1,"targetSku":"T",
             "discountBasisPoints":1000,"eligibleCustomerIds":["%s"]}
            """.formatted(freshName("az"), customer);

        // MEMBER cannot define an offer.
        given().header("Authorization", "Bearer " + member).contentType(ContentType.JSON).body(body)
        .when().post("/api/admin/offers")
        .then().statusCode(403);

        // ADMIN can define.
        String offerId = createOffer(admin, body);

        // Unauthenticated evaluate is rejected.
        given().contentType(ContentType.JSON).body("{\"customerId\":\"" + customer + "\"}")
        .when().post("/api/offers/" + offerId + "/evaluate")
        .then().statusCode(Matchers.anyOf(Matchers.is(401), Matchers.is(403)));

        // Authenticated evaluate of an unknown id → 404 problem+json.
        given().header("Authorization", "Bearer " + member).contentType(ContentType.JSON)
            .body("{\"customerId\":\"" + customer + "\"}")
        .when().post("/api/offers/" + UUID.randomUUID() + "/evaluate")
        .then().statusCode(404)
            .body("code", Matchers.equalTo("OFFER_NOT_FOUND"));
    }

    /** OFFER-AUTHZ-001: a min_qualifier_qty < 1 is rejected at definition time with 422 problem+json. */
    @Test
    @Tag("OFFER-AUTHZ-001")
    void authz_invalidOfferRejected() {
        String admin = adminToken("iv-admin");
        given().header("Authorization", "Bearer " + admin).contentType(ContentType.JSON)
            .body("""
                {"name":"%s","qualifierSku":"Q","minQualifierQty":0,"targetSku":"T","discountBasisPoints":1000}
                """.formatted(freshName("iv")))
        .when().post("/api/admin/offers")
        .then().statusCode(Matchers.anyOf(Matchers.is(400), Matchers.is(422)));
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private String adminToken(String prefix) {
        return OfferEligibilityTestSupport.obtainToken(OfferEligibilityTestSupport.freshEmail(prefix), "ADMIN");
    }

    private String memberToken(String prefix) {
        return OfferEligibilityTestSupport.obtainToken(OfferEligibilityTestSupport.freshEmail(prefix), "MEMBER");
    }

    private static String freshName(String prefix) {
        return prefix + "-offer-" + UUID.randomUUID();
    }

    private String createOffer(String token, String body) {
        return given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON).body(body)
        .when().post("/api/admin/offers")
        .then().statusCode(201)
            .extract().path("id");
    }

    private io.restassured.response.ValidatableResponse evaluate(String token, String offerId, String body) {
        return given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON).body(body)
        .when().post("/api/offers/" + offerId + "/evaluate")
        .then().statusCode(200);
    }
}
