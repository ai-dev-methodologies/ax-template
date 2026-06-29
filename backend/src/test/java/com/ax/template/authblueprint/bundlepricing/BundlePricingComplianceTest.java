package com.ax.template.authblueprint.bundlepricing;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.hamcrest.Matchers;
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
 * Behavioral compliance tests for bundle-pricing-l0.yaml (4 items / 4 families) — black-box
 * HTTP via RestAssured. Absorbed the external reference {@code BundleOrderItemImpl} conserving roll-up.
 *
 * <p>BUNDLE-ITEMSUM-001 (conserving roll-up), BUNDLE-FIXED-001 (fixed base, not summed),
 * BUNDLE-DERIVED-001 (taxability derived from children), BUNDLE-AUTHZ-001 (ADMIN create).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("BUNDLEPRICING")
class BundlePricingComplianceTest {

    @LocalServerPort int port;

    @BeforeEach
    void setup() {
        BundlePricingTestSupport.useRandomPort(port);
    }

    // ─── ITEMSUM family ──────────────────────────────────────────────────────

    /**
     * BUNDLE-ITEMSUM-001: ITEM_SUM composite [(1000×2 taxable),(500×3 non-taxable)] + fee 200 taxable.
     * retail == 1000×2 + 500×3 + 200 == 3700; sale (child1 sale 900, child2 retail fallback)
     * == 900×2 + 500×3 + 200 == 3500; taxablePrice == 1000×2 + 200 == 2200 (excludes non-taxable
     * child); taxable == true. retail is INDEPENDENTLY reconstructed from the disclosed breakdown.
     */
    @Test
    @Tag("BUNDLE-ITEMSUM-001")
    void itemsum_001_conservingRollUp() {
        String admin = adminToken("is1");
        String body = """
            {
              "pricingModel":"ITEM_SUM",
              "currency":"USD",
              "components":[
                {"name":"A","quantity":2,"unitRetailPrice":1000,"unitSalePrice":900,"taxable":true},
                {"name":"B","quantity":3,"unitRetailPrice":500,"taxable":false}
              ],
              "fees":[{"label":"assembly","amount":200,"taxable":true}]
            }
            """;
        String id = createComposite(admin, body);

        Response price = priceOf(admin, id);
        price.then()
            .body("pricingModel", Matchers.equalTo("ITEM_SUM"))
            .body("retailPrice", Matchers.equalTo(3700))
            .body("salePrice", Matchers.equalTo(3500))
            .body("taxablePrice", Matchers.equalTo(2200))
            .body("taxable", Matchers.equalTo(true));

        // Conservation cross-check (INDEPENDENT re-derivation): Σ disclosed per-child
        // retailSubtotal + Σ disclosed fee amounts MUST reconstruct the authoritative total.
        List<Integer> subtotals = price.jsonPath().getList("components.retailSubtotal");
        List<Integer> feeAmounts = price.jsonPath().getList("fees.amount");
        long reconstructed = subtotals.stream().mapToLong(Integer::longValue).sum()
                           + feeAmounts.stream().mapToLong(Integer::longValue).sum();
        assertThat(reconstructed).isEqualTo(3700L);

        // Each child counted exactly once at unitRetailPrice × quantity.
        assertThat(subtotals).containsExactly(2000, 1500);
    }

    // ─── FIXED family ────────────────────────────────────────────────────────

    /**
     * BUNDLE-FIXED-001: BUNDLE composite baseRetailPrice 5000 with children whose Σ would be 3700.
     * retail == 5000 (the fixed base, NOT summed); sale falls back to base retail when no base sale.
     */
    @Test
    @Tag("BUNDLE-FIXED-001")
    void fixed_001_fixedBaseNotSummed() {
        String admin = adminToken("fx1");
        String body = """
            {
              "pricingModel":"BUNDLE",
              "currency":"USD",
              "baseRetailPrice":5000,
              "components":[
                {"name":"A","quantity":2,"unitRetailPrice":1000,"taxable":true},
                {"name":"B","quantity":3,"unitRetailPrice":500,"taxable":true}
              ]
            }
            """;
        String id = createComposite(admin, body);

        Response price = priceOf(admin, id);
        price.then()
            .body("pricingModel", Matchers.equalTo("BUNDLE"))
            .body("retailPrice", Matchers.equalTo(5000))   // fixed base
            .body("salePrice", Matchers.equalTo(5000))     // sale falls back to base retail
            .body("taxable", Matchers.equalTo(true))        // derived from taxable children
            .body("taxablePrice", Matchers.equalTo(5000));  // BUNDLE taxable ⇒ fixed base, NOT summed children

        // The fixed price is NOT the sum of the children (which would be 1000×2 + 500×3 = 3500).
        long childSum = price.jsonPath().getList("components.retailSubtotal").stream()
            .mapToLong(o -> ((Number) o).longValue()).sum();
        assertThat(childSum).isEqualTo(3500L);
        assertThat(price.jsonPath().getLong("retailPrice")).isNotEqualTo(childSum);
    }

    /** BUNDLE-FIXED-001: an explicit base sale price is used verbatim (not summed, not the retail). */
    @Test
    @Tag("BUNDLE-FIXED-001")
    void fixed_002_baseSalePriceUsed() {
        String admin = adminToken("fx2");
        String body = """
            {"pricingModel":"BUNDLE","currency":"USD","baseRetailPrice":5000,"baseSalePrice":4200,
             "components":[{"name":"A","quantity":1,"unitRetailPrice":1000,"taxable":true}]}
            """;
        String id = createComposite(admin, body);
        priceOf(admin, id).then()
            .body("retailPrice", Matchers.equalTo(5000))
            .body("salePrice", Matchers.equalTo(4200));
    }

    /** BUNDLE-FIXED-001: the mode/base-price shape is mutually exclusive — wrong shapes are rejected (400). */
    @Test
    @Tag("BUNDLE-FIXED-001")
    void fixed_003_modeBasePriceExclusive() {
        String admin = adminToken("fx3");

        // ITEM_SUM carrying a base price → 400.
        given().header("Authorization", "Bearer " + admin).contentType(ContentType.JSON)
            .body("""
                {"pricingModel":"ITEM_SUM","currency":"USD","baseRetailPrice":5000,
                 "components":[{"name":"A","quantity":1,"unitRetailPrice":1000,"taxable":true}]}
                """)
        .when().post("/api/bundle-pricing/composites")
        .then().statusCode(400)
            .body("code", Matchers.equalTo("INVALID_COMPOSITE_ITEM"));

        // BUNDLE without a base price → 400.
        given().header("Authorization", "Bearer " + admin).contentType(ContentType.JSON)
            .body("""
                {"pricingModel":"BUNDLE","currency":"USD",
                 "components":[{"name":"A","quantity":1,"unitRetailPrice":1000,"taxable":true}]}
                """)
        .when().post("/api/bundle-pricing/composites")
        .then().statusCode(400)
            .body("code", Matchers.equalTo("INVALID_COMPOSITE_ITEM"));

        // ITEM_SUM with no children → 400.
        given().header("Authorization", "Bearer " + admin).contentType(ContentType.JSON)
            .body("{\"pricingModel\":\"ITEM_SUM\",\"currency\":\"USD\",\"components\":[]}")
        .when().post("/api/bundle-pricing/composites")
        .then().statusCode(400)
            .body("code", Matchers.equalTo("INVALID_COMPOSITE_ITEM"));
    }

    // ─── DERIVED family ──────────────────────────────────────────────────────

    /**
     * BUNDLE-DERIVED-001: a composite whose children are ALL non-taxable derives taxable=false
     * and taxablePrice=0; a composite with a taxable child derives taxable=true.
     */
    @Test
    @Tag("BUNDLE-DERIVED-001")
    void derived_001_taxabilityDerivedFromChildren() {
        String admin = adminToken("dv1");

        // All children non-taxable, no taxable fee → derived taxable=false, taxablePrice=0.
        String nonTaxable = createComposite(admin, """
            {"pricingModel":"ITEM_SUM","currency":"USD",
             "components":[
               {"name":"A","quantity":2,"unitRetailPrice":1000,"taxable":false},
               {"name":"B","quantity":1,"unitRetailPrice":500,"taxable":false}
             ],
             "fees":[{"label":"handling","amount":100,"taxable":false}]}
            """);
        priceOf(admin, nonTaxable).then()
            .body("taxable", Matchers.equalTo(false))
            .body("taxablePrice", Matchers.equalTo(0))
            .body("retailPrice", Matchers.equalTo(2600))    // 1000×2 + 500 + 100
            .body("salePrice", Matchers.equalTo(2600));     // no child sale price ⇒ per-child retail fallback ⇒ == retail

        // One taxable child → derived taxable=true; taxablePrice sums only the taxable child.
        String mixed = createComposite(admin, """
            {"pricingModel":"ITEM_SUM","currency":"USD",
             "components":[
               {"name":"A","quantity":2,"unitRetailPrice":1000,"taxable":true},
               {"name":"B","quantity":1,"unitRetailPrice":500,"taxable":false}
             ]}
            """);
        priceOf(admin, mixed).then()
            .body("taxable", Matchers.equalTo(true))
            .body("taxablePrice", Matchers.equalTo(2000));  // only the taxable child 1000×2
    }

    // ─── AUTHZ family ────────────────────────────────────────────────────────

    /**
     * BUNDLE-AUTHZ-001: composite creation requires ROLE_ADMIN. A MEMBER is forbidden (403);
     * an unauthenticated pricing read is rejected (401/403); an authenticated read succeeds.
     */
    @Test
    @Tag("BUNDLE-AUTHZ-001")
    void authz_001_adminOnlyCreate() {
        String member = BundlePricingTestSupport.obtainToken(
            BundlePricingTestSupport.freshEmail("az-member"), "MEMBER");
        String admin = adminToken("az-admin");

        String body = """
            {"pricingModel":"BUNDLE","currency":"USD","baseRetailPrice":1000,
             "components":[{"name":"A","quantity":1,"unitRetailPrice":1000,"taxable":true}]}
            """;

        // MEMBER cannot create.
        given().header("Authorization", "Bearer " + member).contentType(ContentType.JSON).body(body)
        .when().post("/api/bundle-pricing/composites")
        .then().statusCode(403);

        // ADMIN can create.
        String id = createComposite(admin, body);

        // Unauthenticated pricing read is rejected.
        given()
        .when().get("/api/bundle-pricing/composites/" + id + "/price")
        .then().statusCode(Matchers.anyOf(Matchers.is(401), Matchers.is(403)));

        // Authenticated read succeeds.
        given().header("Authorization", "Bearer " + member)
        .when().get("/api/bundle-pricing/composites/" + id + "/price")
        .then().statusCode(200)
            .body("retailPrice", Matchers.equalTo(1000));
    }

    /** Missing composite → 404 problem+json. */
    @Test
    @Tag("BUNDLE-AUTHZ-001")
    void authz_002_missingCompositeIsNotFound() {
        String member = BundlePricingTestSupport.obtainToken(
            BundlePricingTestSupport.freshEmail("nf"), "MEMBER");
        given().header("Authorization", "Bearer " + member)
        .when().get("/api/bundle-pricing/composites/" + java.util.UUID.randomUUID() + "/price")
        .then().statusCode(404)
            .body("code", Matchers.equalTo("COMPOSITE_ITEM_NOT_FOUND"));
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private String adminToken(String prefix) {
        return BundlePricingTestSupport.obtainToken(
            BundlePricingTestSupport.freshEmail(prefix), "ADMIN");
    }

    private String createComposite(String token, String body) {
        return given().header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON).body(body)
        .when().post("/api/bundle-pricing/composites")
        .then().statusCode(201)
            .extract().path("id");
    }

    private Response priceOf(String token, String id) {
        return given().header("Authorization", "Bearer " + token)
            .when().get("/api/bundle-pricing/composites/" + id + "/price")
            .then().statusCode(200)
            .extract().response();
    }
}
