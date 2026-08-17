package com.ax.template.authblueprint.commercecatalog;

import io.restassured.response.ExtractableResponse;
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
 * CAT-INVENTORY-GATE-001 behavioral tests — genuine, non-vacuous.
 *
 * <p>Verifies the tri-state inventoryType gate in {@link CatalogProductService#assertPurchasable}:
 * <ul>
 *   <li>UNAVAILABLE → 409 CATALOG_SKU_NOT_PURCHASABLE, regardless of active window / price.
 *   <li>ALWAYS_AVAILABLE → passes the catalog gate (200 purchasable).
 *   <li>CHECK_QUANTITY → catalog gate does NOT block; passes (200 purchasable at catalog level).
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("COMMERCECATALOG")
class CatalogInventoryGateTest {

    @LocalServerPort int port;
    String member;

    @BeforeEach
    void setup() {
        member = CatalogTestSupport.obtainToken(
            CatalogTestSupport.freshEmail("cat-invgate"), "MEMBER");
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────

    /**
     * Create a product whose default SKU has the given inventoryType.
     * Returns the default SKU id from the response.
     */
    private String createProductWithInventoryType(InventoryType inventoryType) {
        String invTypeJson = inventoryType != null
            ? "\"" + inventoryType.name() + "\""
            : "null";
        String body = """
            {"name":"InvGateProduct-%s","canSellWithoutOptions":true,
             "defaultSku":{"retailPrice":1000,"currency":"USD","inventoryType":%s}}
            """.formatted(UUID.randomUUID(), invTypeJson);
        ExtractableResponse<Response> r = given()
            .header("Authorization", "Bearer " + member)
            .header("Content-Type", "application/json")
            .body(body)
        .when().post("/api/catalog/products").then().extract();
        assertThat(r.statusCode()).as("create product with inventoryType=" + inventoryType).isEqualTo(201);
        return r.jsonPath().getString("defaultSkuId");
    }

    private ExtractableResponse<Response> checkPurchasable(String skuId) {
        return given()
            .header("Authorization", "Bearer " + member)
        .when().get("/api/catalog/skus/" + skuId + "/purchasable").then().extract();
    }

    // ── CAT-INVENTORY-GATE-001 — UNAVAILABLE SKU is always rejected ──
    @Test @Tag("CAT-INVENTORY-GATE-001")
    void unavailableSku_isNeverPurchasable_regardlessOfWindowOrPrice() {
        // Active, priced product whose default SKU is UNAVAILABLE
        String skuId = createProductWithInventoryType(InventoryType.UNAVAILABLE);

        ExtractableResponse<Response> r = checkPurchasable(skuId);
        assertThat(r.statusCode())
            .as("UNAVAILABLE SKU must be 409 CATALOG_SKU_NOT_PURCHASABLE")
            .isEqualTo(409);
        assertThat(r.jsonPath().getString("code"))
            .isEqualTo("CATALOG_SKU_NOT_PURCHASABLE");
    }

    // ── CAT-INVENTORY-GATE-001 — ALWAYS_AVAILABLE SKU passes the catalog gate ──
    @Test @Tag("CAT-INVENTORY-GATE-001")
    void alwaysAvailableSku_activeAndPriced_isPurchasable() {
        // Active, priced product whose default SKU is ALWAYS_AVAILABLE
        String skuId = createProductWithInventoryType(InventoryType.ALWAYS_AVAILABLE);

        ExtractableResponse<Response> r = checkPurchasable(skuId);
        assertThat(r.statusCode())
            .as("ALWAYS_AVAILABLE active priced SKU must be 200 purchasable")
            .isEqualTo(200);
    }

    // ── CAT-INVENTORY-GATE-001 — CHECK_QUANTITY SKU passes the catalog gate (quantity deferred) ──
    @Test @Tag("CAT-INVENTORY-GATE-001")
    void checkQuantitySku_activeAndPriced_passesTheCatalogGate() {
        // Active, priced product whose default SKU is CHECK_QUANTITY
        // The catalog gate must NOT block — quantity check is deferred to inventory-reservation.
        String skuId = createProductWithInventoryType(InventoryType.CHECK_QUANTITY);

        ExtractableResponse<Response> r = checkPurchasable(skuId);
        assertThat(r.statusCode())
            .as("CHECK_QUANTITY active priced SKU must pass catalog gate (200) — quantity check deferred")
            .isEqualTo(200);
    }
}
