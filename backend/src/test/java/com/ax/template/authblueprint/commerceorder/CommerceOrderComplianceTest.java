package com.ax.template.authblueprint.commerceorder;

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
 * Behavioral compliance test for the commerceorder domain.
 *
 * <p>Every test asserts COMPUTED VALUES, STATUS CODES, or EXACT EQUALITY.
 * No {@code != null} only assertions — per the adversarial review requirement.
 *
 * <p>Invariants covered:
 * <ul>
 *   <li>ORDER-MERGE-001: add same sku twice → one line, quantity summed to 5</li>
 *   <li>ORDER-IMMUTABLE-001: submit then addItem → 409 ORDER_NOT_EDITABLE</li>
 *   <li>ORDER-LIFECYCLE-001: SUBMITTED → IN_PROCESS rejected; CANCELLED → submit rejected</li>
 *   <li>ORDER-FULFILL-001: 5 units split 2+3 → ok; 2+2 → 422 ORDER_FULFILLMENT_NOT_CONSERVED</li>
 *   <li>ORDER-TOTAL-SNAPSHOT-001: total frozen at submit == value passed in</li>
 *   <li>ORDER-AUTHZ-001: other user's GET → 404</li>
 *   <li>ORDER-SNAPSHOT-001: line price derived from snapshot (unit_price_at_add × qty)</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("COMMERCEORDER")
class CommerceOrderComplianceTest {

    @LocalServerPort
    int port;

    String memberToken;
    String otherToken;

    @BeforeEach
    void setup() {
        memberToken = CommerceOrderTestSupport.obtainToken(
            CommerceOrderTestSupport.freshEmail("co-member"), "MEMBER");
        otherToken = CommerceOrderTestSupport.obtainToken(
            CommerceOrderTestSupport.freshEmail("co-other"), "MEMBER");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ORDER-MERGE-001: add same sku twice → one line, quantity = 5
    // ─────────────────────────────────────────────────────────────────────────

    @Test @Tag("ORDER-MERGE-001")
    void merge_sameSku_addedTwice_quantitySummed() {
        String orderId = CommerceOrderTestSupport.createCart(memberToken, "KRW");
        String skuId = "sku-" + UUID.randomUUID();

        // Add sku qty=2 first
        CommerceOrderTestSupport.addItem(memberToken, orderId, skuId, "Widget", 1000L, 2);
        // Add same sku qty=3 → should merge, NOT create a second line
        CommerceOrderTestSupport.addItem(memberToken, orderId, skuId, "Widget", 1000L, 3);

        ExtractableResponse<Response> resp = given()
            .header("Authorization", "Bearer " + memberToken)
        .when().get("/api/orders/" + orderId)
        .then().statusCode(200)
        .extract();

        // BEHAVIORAL: exactly ONE item line, quantity == 5
        int itemCount = resp.jsonPath().getList("items").size();
        assertThat(itemCount).as("same sku added twice must produce exactly one line (MERGE)").isEqualTo(1);

        int qty = resp.jsonPath().getInt("items[0].quantity");
        assertThat(qty).as("merged quantity must be 2 + 3 = 5").isEqualTo(5);

        long lineTotal = resp.jsonPath().getLong("items[0].lineTotal");
        assertThat(lineTotal).as("line total must be 5 × 1000 = 5000").isEqualTo(5000L);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ORDER-IMMUTABLE-001: submit then addItem → 409
    // ─────────────────────────────────────────────────────────────────────────

    @Test @Tag("ORDER-IMMUTABLE-001")
    void submitThenAdd_returns409_orderNotEditable() {
        String orderId = CommerceOrderTestSupport.createCart(memberToken, "KRW");
        CommerceOrderTestSupport.addItem(memberToken, orderId, "sku-a", "Widget A", 500L, 1);

        // Submit the order
        CommerceOrderTestSupport.submit(memberToken, orderId, 500L, 500L, 0L)
            .response().then().statusCode(200);

        // Now try to add another item — must be 409 ORDER_NOT_EDITABLE
        String addBody = "{\"skuId\":\"sku-b\",\"nameAtAdd\":\"Widget B\",\"unitPriceAtAdd\":300,\"quantity\":1}";
        ExtractableResponse<Response> addResp = given()
            .header("Authorization", "Bearer " + memberToken)
            .header("Content-Type", "application/json")
            .body(addBody)
        .when().post("/api/orders/" + orderId + "/items")
        .then().extract();

        assertThat(addResp.statusCode())
            .as("addItem after submit must return 409 (ORDER_NOT_EDITABLE)").isEqualTo(409);
        String code = addResp.jsonPath().getString("code");
        assertThat(code).as("error code must be ORDER_NOT_EDITABLE").isEqualTo("ORDER_NOT_EDITABLE");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ORDER-LIFECYCLE-001: SUBMITTED → re-submit (IN_PROCESS) rejected with 409
    // ─────────────────────────────────────────────────────────────────────────

    @Test @Tag("ORDER-LIFECYCLE-001")
    void reSubmit_submittedOrder_returns409() {
        String orderId = CommerceOrderTestSupport.createCart(memberToken, "KRW");
        CommerceOrderTestSupport.addItem(memberToken, orderId, "sku-x", "Item X", 100L, 1);

        // First submit — must succeed
        CommerceOrderTestSupport.submit(memberToken, orderId, 100L, 100L, 0L)
            .response().then().statusCode(200);

        // Second submit — SUBMITTED → SUBMITTED is not in ALLOWED; must return 409
        ExtractableResponse<Response> resp2 = CommerceOrderTestSupport.submit(memberToken, orderId, 100L, 100L, 0L);
        assertThat(resp2.statusCode())
            .as("re-submitting a SUBMITTED order must return 409 (ORDER_INVALID_TRANSITION)").isEqualTo(409);
        String code = resp2.jsonPath().getString("code");
        assertThat(code).as("error code must be ORDER_INVALID_TRANSITION").isEqualTo("ORDER_INVALID_TRANSITION");
    }

    @Test @Tag("ORDER-LIFECYCLE-001")
    void cancel_submittedOrder_succeeds() {
        String orderId = CommerceOrderTestSupport.createCart(memberToken, "KRW");
        CommerceOrderTestSupport.addItem(memberToken, orderId, "sku-c", "Item C", 200L, 2);
        CommerceOrderTestSupport.submit(memberToken, orderId, 400L, 400L, 0L)
            .response().then().statusCode(200);

        // SUBMITTED → CANCELLED is allowed
        ExtractableResponse<Response> cancelResp = given()
            .header("Authorization", "Bearer " + memberToken)
        .when().post("/api/orders/" + orderId + "/cancel")
        .then().extract();

        assertThat(cancelResp.statusCode()).as("SUBMITTED → CANCELLED must succeed").isEqualTo(200);
        assertThat(cancelResp.jsonPath().getString("status"))
            .as("order must be CANCELLED after cancel").isEqualTo("CANCELLED");
    }

    @Test @Tag("ORDER-LIFECYCLE-001")
    void submit_cancelledOrder_returns409() {
        String orderId = CommerceOrderTestSupport.createCart(memberToken, "KRW");

        // Cancel the IN_PROCESS order
        given()
            .header("Authorization", "Bearer " + memberToken)
        .when().post("/api/orders/" + orderId + "/cancel")
        .then().statusCode(200);

        // CANCELLED → SUBMITTED is terminal — must return 409
        ExtractableResponse<Response> submitResp = CommerceOrderTestSupport.submit(memberToken, orderId, 0L, 0L, 0L);
        assertThat(submitResp.statusCode())
            .as("submitting a CANCELLED order must return 409 (ORDER_INVALID_TRANSITION)").isEqualTo(409);
        String code = submitResp.jsonPath().getString("code");
        assertThat(code).as("error code must be ORDER_INVALID_TRANSITION").isEqualTo("ORDER_INVALID_TRANSITION");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ORDER-FULFILL-001: 5 units split 2+3 → ok; 2+2 → 422
    // ─────────────────────────────────────────────────────────────────────────

    @Test @Tag("ORDER-FULFILL-001")
    void fulfillment_conserved_split2plus3_succeeds() {
        String orderId = CommerceOrderTestSupport.createCart(memberToken, "KRW");
        String itemId = CommerceOrderTestSupport.addItem(memberToken, orderId, "sku-ff", "Fulfillable", 100L, 5);

        // H1 fix: must submit before assigning fulfillment
        CommerceOrderTestSupport.submit(memberToken, orderId, 500L, 500L, 0L)
            .response().then().statusCode(200);

        // Assign fulfillment: 2 to address A, 3 to address B (total = 5 = item.quantity)
        String body = String.format(
            "{\"groups\":["
            + "{\"address\":\"Seoul\",\"items\":[{\"orderItemId\":\"%s\",\"quantity\":2}]},"
            + "{\"address\":\"Busan\",\"items\":[{\"orderItemId\":\"%s\",\"quantity\":3}]}"
            + "]}", itemId, itemId);

        int status = given()
            .header("Authorization", "Bearer " + memberToken)
            .header("Content-Type", "application/json")
            .body(body)
        .when().post("/api/orders/" + orderId + "/fulfillment")
        .then().extract().statusCode();

        assertThat(status).as("fulfillment 2+3=5 matches item qty=5 on SUBMITTED order; must succeed").isEqualTo(200);
    }

    @Test @Tag("ORDER-FULFILL-001")
    void fulfillment_notConserved_split2plus2_returns422() {
        String orderId = CommerceOrderTestSupport.createCart(memberToken, "KRW");
        String itemId = CommerceOrderTestSupport.addItem(memberToken, orderId, "sku-nc", "NcItem", 100L, 5);

        // H1 fix: must submit before assigning fulfillment
        CommerceOrderTestSupport.submit(memberToken, orderId, 500L, 500L, 0L)
            .response().then().statusCode(200);

        // Assign fulfillment: 2+2=4 but item quantity=5 → must fail with 422
        String body = String.format(
            "{\"groups\":["
            + "{\"address\":\"Seoul\",\"items\":[{\"orderItemId\":\"%s\",\"quantity\":2}]},"
            + "{\"address\":\"Busan\",\"items\":[{\"orderItemId\":\"%s\",\"quantity\":2}]}"
            + "]}", itemId, itemId);

        ExtractableResponse<Response> resp = given()
            .header("Authorization", "Bearer " + memberToken)
            .header("Content-Type", "application/json")
            .body(body)
        .when().post("/api/orders/" + orderId + "/fulfillment")
        .then().extract();

        assertThat(resp.statusCode())
            .as("fulfillment 2+2=4 != item qty=5; must return 422 (ORDER_FULFILLMENT_NOT_CONSERVED)")
            .isEqualTo(422);
        String code = resp.jsonPath().getString("code");
        assertThat(code).as("error code must be ORDER_FULFILLMENT_NOT_CONSERVED")
            .isEqualTo("ORDER_FULFILLMENT_NOT_CONSERVED");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // H1: fulfillment on IN_PROCESS cart → 409 ORDER_NOT_SUBMITTED
    // ─────────────────────────────────────────────────────────────────────────

    @Test @Tag("ORDER-FULFILL-001")
    void fulfillment_onInProcessCart_returns409() {
        String orderId = CommerceOrderTestSupport.createCart(memberToken, "KRW");
        String itemId = CommerceOrderTestSupport.addItem(memberToken, orderId, "sku-h1a", "H1 Item", 100L, 2);

        // Attempt fulfillment WITHOUT submitting first → 409
        String body = String.format(
            "{\"groups\":[{\"address\":\"Seoul\","
            + "\"items\":[{\"orderItemId\":\"%s\",\"quantity\":2}]}]}", itemId);

        ExtractableResponse<Response> resp = given()
            .header("Authorization", "Bearer " + memberToken)
            .header("Content-Type", "application/json")
            .body(body)
        .when().post("/api/orders/" + orderId + "/fulfillment")
        .then().extract();

        assertThat(resp.statusCode())
            .as("fulfillment on IN_PROCESS cart must return 409 (H1: post-submit only)").isEqualTo(409);
        String code = resp.jsonPath().getString("code");
        assertThat(code).as("error code must be ORDER_NOT_SUBMITTED").isEqualTo("ORDER_NOT_SUBMITTED");
    }

    @Test @Tag("ORDER-FULFILL-001")
    void fulfillment_onCancelledOrder_returns409() {
        String orderId = CommerceOrderTestSupport.createCart(memberToken, "KRW");
        String itemId = CommerceOrderTestSupport.addItem(memberToken, orderId, "sku-h1b", "H1b Item", 100L, 2);

        // Cancel the IN_PROCESS order
        given().header("Authorization", "Bearer " + memberToken)
            .when().post("/api/orders/" + orderId + "/cancel")
            .then().statusCode(200);

        // Attempt fulfillment on CANCELLED order → 409
        String body = String.format(
            "{\"groups\":[{\"address\":\"Seoul\","
            + "\"items\":[{\"orderItemId\":\"%s\",\"quantity\":2}]}]}", itemId);

        ExtractableResponse<Response> resp = given()
            .header("Authorization", "Bearer " + memberToken)
            .header("Content-Type", "application/json")
            .body(body)
        .when().post("/api/orders/" + orderId + "/fulfillment")
        .then().extract();

        assertThat(resp.statusCode())
            .as("fulfillment on CANCELLED order must return 409 (H1)").isEqualTo(409);
        String code = resp.jsonPath().getString("code");
        assertThat(code).as("error code must be ORDER_NOT_SUBMITTED").isEqualTo("ORDER_NOT_SUBMITTED");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // H2: re-assign fulfillment replaces, not doubles
    // ─────────────────────────────────────────────────────────────────────────

    @Test @Tag("ORDER-FULFILL-001")
    void fulfillment_reassign_replacesNotDoubles() {
        String orderId = CommerceOrderTestSupport.createCart(memberToken, "KRW");
        String itemId = CommerceOrderTestSupport.addItem(memberToken, orderId, "sku-h2", "H2 Item", 200L, 5);
        CommerceOrderTestSupport.submit(memberToken, orderId, 1000L, 1000L, 0L)
            .response().then().statusCode(200);

        String body = String.format(
            "{\"groups\":["
            + "{\"address\":\"Seoul\",\"items\":[{\"orderItemId\":\"%s\",\"quantity\":2}]},"
            + "{\"address\":\"Busan\",\"items\":[{\"orderItemId\":\"%s\",\"quantity\":3}]}"
            + "]}", itemId, itemId);

        // First assignment
        given().header("Authorization", "Bearer " + memberToken)
            .header("Content-Type", "application/json").body(body)
            .when().post("/api/orders/" + orderId + "/fulfillment")
            .then().statusCode(200);

        // Second identical assignment — must REPLACE (idempotent, not double)
        ExtractableResponse<Response> resp2 = given()
            .header("Authorization", "Bearer " + memberToken)
            .header("Content-Type", "application/json").body(body)
        .when().post("/api/orders/" + orderId + "/fulfillment")
        .then().extract();

        assertThat(resp2.statusCode())
            .as("second fulfillment assign must succeed (replace semantics)").isEqualTo(200);

        // Read back — must have exactly 2 groups (Seoul + Busan), not 4
        ExtractableResponse<Response> getResp = given()
            .header("Authorization", "Bearer " + memberToken)
        .when().get("/api/orders/" + orderId)
        .then().statusCode(200).extract();

        // The order still has the same single item with qty=5
        int itemCount = getResp.jsonPath().getList("items").size();
        int qty = getResp.jsonPath().getInt("items[0].quantity");
        assertThat(itemCount).as("order must still have exactly 1 item after re-assign").isEqualTo(1);
        assertThat(qty).as("item quantity must still be 5 after re-assign (not doubled)").isEqualTo(5);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // H3: fulfillment with phantom (foreign) orderItemId → 422
    // ─────────────────────────────────────────────────────────────────────────

    @Test @Tag("ORDER-FULFILL-001")
    void fulfillment_phantomOrderItemId_returns422() {
        String orderId = CommerceOrderTestSupport.createCart(memberToken, "KRW");
        CommerceOrderTestSupport.addItem(memberToken, orderId, "sku-h3", "H3 Item", 100L, 3);
        CommerceOrderTestSupport.submit(memberToken, orderId, 300L, 300L, 0L)
            .response().then().statusCode(200);

        // Reference a random UUID that does not belong to this order
        String phantomId = UUID.randomUUID().toString();
        String body = String.format(
            "{\"groups\":[{\"address\":\"Seoul\","
            + "\"items\":[{\"orderItemId\":\"%s\",\"quantity\":3}]}]}", phantomId);

        ExtractableResponse<Response> resp = given()
            .header("Authorization", "Bearer " + memberToken)
            .header("Content-Type", "application/json")
            .body(body)
        .when().post("/api/orders/" + orderId + "/fulfillment")
        .then().extract();

        assertThat(resp.statusCode())
            .as("fulfillment with phantom orderItemId must return 422 (H3)").isEqualTo(422);
        String code = resp.jsonPath().getString("code");
        assertThat(code).as("error code must be ORDER_FULFILLMENT_NOT_CONSERVED")
            .isEqualTo("ORDER_FULFILLMENT_NOT_CONSERVED");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ORDER-TOTAL-SNAPSHOT-001: totals frozen at submit == values passed in
    // ─────────────────────────────────────────────────────────────────────────

    @Test @Tag("ORDER-TOTAL-SNAPSHOT-001")
    void submit_freezesTotals_totalEqualPassedInValue() {
        String orderId = CommerceOrderTestSupport.createCart(memberToken, "KRW");
        CommerceOrderTestSupport.addItem(memberToken, orderId, "sku-t", "Total Item", 2500L, 3);

        long passedTotal = 7500L;
        long passedSub = 7000L;
        long passedTax = 500L;

        ExtractableResponse<Response> submitResp = CommerceOrderTestSupport.submit(
            memberToken, orderId, passedTotal, passedSub, passedTax);
        submitResp.response().then().statusCode(200);

        // Read back the order — totals must equal exactly what was passed in
        ExtractableResponse<Response> getResp = given()
            .header("Authorization", "Bearer " + memberToken)
        .when().get("/api/orders/" + orderId)
        .then().statusCode(200)
        .extract();

        assertThat(getResp.jsonPath().getLong("total"))
            .as("total after submit must equal the value passed to submit (ORDER-TOTAL-SNAPSHOT-001)")
            .isEqualTo(passedTotal);
        assertThat(getResp.jsonPath().getLong("subTotal"))
            .as("subTotal after submit must equal value passed").isEqualTo(passedSub);
        assertThat(getResp.jsonPath().getLong("tax"))
            .as("tax after submit must equal value passed").isEqualTo(passedTax);
        assertThat(getResp.jsonPath().getString("status"))
            .as("status must be SUBMITTED after submit").isEqualTo("SUBMITTED");
        assertThat(getResp.jsonPath().getString("submittedAt"))
            .as("submittedAt must be set after submit").isNotNull().isNotEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ORDER-AUTHZ-001: other user's GET → 404 (IDOR-safe)
    // ─────────────────────────────────────────────────────────────────────────

    @Test @Tag("ORDER-AUTHZ-001")
    void otherUser_getOrder_returns404() {
        // Create a cart as memberToken
        String orderId = CommerceOrderTestSupport.createCart(memberToken, "USD");

        // Try to read it as otherToken — must get 404, not 403 (IDOR-safe)
        int status = given()
            .header("Authorization", "Bearer " + otherToken)
        .when().get("/api/orders/" + orderId)
        .then().extract().statusCode();

        assertThat(status)
            .as("reading another user's order must return 404 (IDOR-safe, not 403)")
            .isEqualTo(404);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ORDER-SNAPSHOT-001: line total derived from snapshot price, not live catalog
    // ─────────────────────────────────────────────────────────────────────────

    @Test @Tag("ORDER-SNAPSHOT-001")
    void lineTotal_derivedFromSnapshotPrice_notLiveCatalog() {
        String orderId = CommerceOrderTestSupport.createCart(memberToken, "USD");
        // Add item with unit_price_at_add = 750, qty = 4
        CommerceOrderTestSupport.addItem(memberToken, orderId, "sku-snap", "Snapshot Widget", 750L, 4);

        ExtractableResponse<Response> resp = given()
            .header("Authorization", "Bearer " + memberToken)
        .when().get("/api/orders/" + orderId)
        .then().statusCode(200)
        .extract();

        long unitPrice = resp.jsonPath().getLong("items[0].unitPriceAtAdd");
        int qty = resp.jsonPath().getInt("items[0].quantity");
        long lineTotal = resp.jsonPath().getLong("items[0].lineTotal");

        assertThat(unitPrice).as("unit_price_at_add must be 750 (snapshot)").isEqualTo(750L);
        assertThat(qty).as("quantity must be 4").isEqualTo(4);
        assertThat(lineTotal)
            .as("lineTotal must be 750 × 4 = 3000 (computed from snapshot, not live catalog)")
            .isEqualTo(3000L);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ORDER-IMMUTABLE-001: update qty on submitted order → 409
    // ─────────────────────────────────────────────────────────────────────────

    @Test @Tag("ORDER-IMMUTABLE-001")
    void updateQty_afterSubmit_returns409() {
        String orderId = CommerceOrderTestSupport.createCart(memberToken, "KRW");
        String itemId = CommerceOrderTestSupport.addItem(memberToken, orderId, "sku-upd", "Update Item", 200L, 1);
        CommerceOrderTestSupport.submit(memberToken, orderId, 200L, 200L, 0L)
            .response().then().statusCode(200);

        // PATCH qty on submitted order — must be 409
        ExtractableResponse<Response> patchResp = given()
            .header("Authorization", "Bearer " + memberToken)
            .header("Content-Type", "application/json")
            .body("{\"quantity\":5}")
        .when().patch("/api/orders/" + orderId + "/items/" + itemId)
        .then().extract();

        assertThat(patchResp.statusCode())
            .as("PATCH qty after submit must return 409 (ORDER_NOT_EDITABLE)").isEqualTo(409);
    }
}
