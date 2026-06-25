package com.ax.template.authblueprint.commercepricing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Commerce pricing compliance test — behavioral assertions per invariant.
 * Every test asserts COMPUTED AMOUNTS — never != null checks or hollow assertions.
 *
 * Test inventory:
 *   PRICING-ORDER-001: tax is on taxableBase (NET, post-discount), not on gross
 *   PRICING-ORDER-002: keystone — pipeline's net tax is STRICTLY LESS THAN gross tax
 *   PRICING-TOTAL-001: conservation fixture — Σ(proratedDiscount)==discount, total==components sum
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("COMMERCEPRICING")
class CommercePricingComplianceTest {

    @LocalServerPort int port;

    String memberToken;

    @BeforeEach
    void setup() {
        PricingTestSupport.useRandomPort(port);
        memberToken = PricingTestSupport.obtainToken(
            PricingTestSupport.freshEmail("pricing-member"), "MEMBER");
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // PRICING-ORDER-001 — tax is applied to taxableBase (NET), not gross
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * PRICING-ORDER-001: one line at 1000, orderDiscount=200, taxBasisPoints=1000 (10%).
     * taxableBase = 1000 − 200 = 800. tax = 10% of 800 = 80. NOT 10% of gross 1000 = 100.
     * Asserts the JSON tax field == 80.
     */
    @Test @Tag("PRICING-ORDER-001")
    void taxOnNetBase_singleLine_taxIs80NotGross100() {
        String body = """
            {
              "lines": [{"sku": "SKU-A", "amount": 1000}],
              "orderDiscount": 200,
              "shipping": 0,
              "taxBasisPoints": 1000
            }
            """;

        // taxableBase = 1000 - 200 = 800; tax = 800 * 1000 / 10000 = 80
        int taxFromResponse = given()
            .header("Authorization", "Bearer " + memberToken)
            .header("Content-Type", "application/json")
            .body(body)
        .when().post("/api/pricing/quote")
        .then().statusCode(200)
        .extract().path("lines[0].tax");

        assertThat(taxFromResponse)
            .as("PRICING-ORDER-001: tax must be 10%% of taxableBase (800) = 80, NOT 10%% of gross (1000) = 100")
            .isEqualTo(80);

        // Also assert taxableBase field
        int taxableBaseFromResponse = given()
            .header("Authorization", "Bearer " + memberToken)
            .header("Content-Type", "application/json")
            .body(body)
        .when().post("/api/pricing/quote")
        .then().statusCode(200)
        .extract().path("lines[0].taxableBase");

        assertThat(taxableBaseFromResponse)
            .as("PRICING-ORDER-001: taxableBase must be 800 (1000 - 200 prorated discount)")
            .isEqualTo(800);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // PRICING-ORDER-002 — keystone: net tax STRICTLY LESS THAN gross tax
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * PRICING-ORDER-002 (keystone): same inputs as PRICING-ORDER-001.
     * Computes gross-base tax (10% of 1000 = 100) IN THE TEST and asserts:
     *   pipeline tax (80) is STRICTLY LESS THAN gross tax (100).
     * This proves the phase ordering is observable and correct.
     */
    @Test @Tag("PRICING-ORDER-002")
    void keystone_netTax_strictlyLessThan_grossTax() {
        String body = """
            {
              "lines": [{"sku": "SKU-A", "amount": 1000}],
              "orderDiscount": 200,
              "shipping": 0,
              "taxBasisPoints": 1000
            }
            """;

        // Gross inputs for the test's own computation
        long gross = 1000L;
        int taxBasisPoints = 1000;
        // Gross-base tax: what you'd get if tax were applied BEFORE discount (wrong phase order)
        long grossBaseTax = gross * taxBasisPoints / 10_000L; // = 100

        // Pipeline response
        int pipelineTotalTax = given()
            .header("Authorization", "Bearer " + memberToken)
            .header("Content-Type", "application/json")
            .body(body)
        .when().post("/api/pricing/quote")
        .then().statusCode(200)
        .extract().path("totalTax");

        // Keystone assertion: net tax (80) < gross tax (100) — phase order is observable
        assertThat((long) pipelineTotalTax)
            .as("PRICING-ORDER-002 keystone: pipeline tax (%d) must be STRICTLY LESS THAN gross-base tax (%d)",
                pipelineTotalTax, grossBaseTax)
            .isLessThan(grossBaseTax);

        // Also verify the exact value — 80, not just < 100
        assertThat(pipelineTotalTax)
            .as("PRICING-ORDER-002: net tax must be exactly 80 (10%% of 800 taxable base)")
            .isEqualTo(80);

        // And verify total uses net tax not gross tax
        int total = given()
            .header("Authorization", "Bearer " + memberToken)
            .header("Content-Type", "application/json")
            .body(body)
        .when().post("/api/pricing/quote")
        .then().statusCode(200)
        .extract().path("total");

        // total = 1000 - 200 + 0 + 80 = 880 (with net tax)
        // If gross tax were used: total = 1000 - 200 + 0 + 100 = 900 (wrong)
        assertThat(total)
            .as("PRICING-ORDER-002: total must use net tax (880), not gross-base tax (900)")
            .isEqualTo(880);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // PRICING-TOTAL-001 — conservation: Σ(proratedDiscount)==discount, total==components
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * PRICING-TOTAL-001: fixture engineered to lose a penny under naive proration.
     * Lines: [333, 333, 334], orderDiscount=100, shipping=50, taxBasisPoints=1000 (10%).
     *
     * Floor proration: floor(100*333/1000)=33, floor(100*333/1000)=33, floor(100*334/1000)=33
     * Σ=99, leftover=1. Largest remainder: 333*100 % 1000=300 (lines 0,1), 334*100 % 1000=400 (line 2).
     * Line 2 gets leftover → shares: 33, 33, 34. Σ=100 EXACTLY.
     *
     * taxableBase: 333-33=300, 333-33=300, 334-34=300. tax: 30, 30, 30. Σtax=90.
     * total = 1000 - 100 + 50 + 90 = 1040.
     *
     * Asserts:
     * 1. Σ(proratedDiscount) == 100 exactly (conservation)
     * 2. total == 1000 - 100 + 50 + 90 == 1040 (closure)
     * 3. total == reconstruct-from-components in test (exact equality)
     */
    @Test @Tag("PRICING-TOTAL-001")
    void conservation_pennyFixture_sumsExactly_totalEqualsComponents() {
        String body = """
            {
              "lines": [
                {"sku": "SKU-A", "amount": 333},
                {"sku": "SKU-B", "amount": 333},
                {"sku": "SKU-C", "amount": 334}
              ],
              "orderDiscount": 100,
              "shipping": 50,
              "taxBasisPoints": 1000
            }
            """;

        io.restassured.response.ExtractableResponse<io.restassured.response.Response> resp = given()
            .header("Authorization", "Bearer " + memberToken)
            .header("Content-Type", "application/json")
            .body(body)
        .when().post("/api/pricing/quote")
        .then().statusCode(200)
        .extract();

        int subTotal     = resp.path("subTotal");       // 1000
        int orderDiscount = resp.path("orderDiscount"); // 100
        int shipping     = resp.path("shipping");       // 50
        int totalTax     = resp.path("totalTax");       // 90
        int total        = resp.path("total");          // 1040

        // 1. Σ(proratedDiscount) == orderDiscount EXACTLY
        int d0 = resp.path("lines[0].proratedDiscount"); // 33
        int d1 = resp.path("lines[1].proratedDiscount"); // 33
        int d2 = resp.path("lines[2].proratedDiscount"); // 34
        int sumProrated = d0 + d1 + d2;
        assertThat(sumProrated)
            .as("PRICING-TOTAL-001: Σ(proratedDiscount) must equal orderDiscount (%d) EXACTLY, got %d",
                orderDiscount, sumProrated)
            .isEqualTo(orderDiscount);
        // Verify the penny went to the right line (largest fractional remainder)
        assertThat(d2)
            .as("PRICING-TOTAL-001: line[2] (amount=334) has largest fractional remainder and must get the leftover penny")
            .isEqualTo(34);

        // 2. total == closure formula (reconstructed in the test)
        long reconstructed = (long) subTotal - orderDiscount + shipping + totalTax;
        assertThat((long) total)
            .as("PRICING-TOTAL-001: total (%d) must equal subTotal(%d) - discount(%d) + shipping(%d) + tax(%d) = %d",
                total, subTotal, orderDiscount, shipping, totalTax, reconstructed)
            .isEqualTo(reconstructed);

        // 3. Exact values for full traceability
        assertThat(subTotal).as("subTotal must be 333+333+334=1000").isEqualTo(1000);
        assertThat(totalTax).as("Σtax: each taxableBase=300, tax=30 each → 90 total").isEqualTo(90);
        assertThat(total).as("total must be 1000-100+50+90=1040").isEqualTo(1040);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Invalid input: taxBasisPoints out of range → 422
    // ─────────────────────────────────────────────────────────────────────────────

    @Test @Tag("PRICING-ORDER-001")
    void invalidTaxRate_above10000_returns422() {
        String body = """
            {
              "lines": [{"sku": "SKU-A", "amount": 1000}],
              "orderDiscount": 0,
              "shipping": 0,
              "taxBasisPoints": 10001
            }
            """;

        String code = given()
            .header("Authorization", "Bearer " + memberToken)
            .header("Content-Type", "application/json")
            .body(body)
        .when().post("/api/pricing/quote")
        .then().statusCode(422)
        .extract().path("code");

        assertThat(code)
            .as("taxBasisPoints > 10000 must return PRICING_INVALID_TAX_RATE")
            .isEqualTo("PRICING_INVALID_TAX_RATE");
    }

    @Test @Tag("PRICING-ORDER-001")
    void invalidTaxRate_negative_returns422() {
        String body = """
            {
              "lines": [{"sku": "SKU-A", "amount": 1000}],
              "orderDiscount": 0,
              "shipping": 0,
              "taxBasisPoints": -1
            }
            """;

        String code = given()
            .header("Authorization", "Bearer " + memberToken)
            .header("Content-Type", "application/json")
            .body(body)
        .when().post("/api/pricing/quote")
        .then().statusCode(422)
        .extract().path("code");

        assertThat(code)
            .as("taxBasisPoints < 0 must return PRICING_INVALID_TAX_RATE")
            .isEqualTo("PRICING_INVALID_TAX_RATE");
    }

    @Test @Tag("PRICING-ORDER-001")
    void unauthenticated_returns401() {
        String body = """
            {
              "lines": [{"sku": "SKU-A", "amount": 1000}],
              "orderDiscount": 0,
              "shipping": 0,
              "taxBasisPoints": 1000
            }
            """;

        given()
            .header("Content-Type", "application/json")
            .body(body)
        .when().post("/api/pricing/quote")
        .then().statusCode(401);
    }
}
