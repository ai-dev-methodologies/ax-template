package com.ax.template.authblueprint.orderquantization;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * order-multiple-quantization-l0 compliance — verified against the live orderquantization reference
 * workload. The invariant: orderQuantity = max(MOQ, ceil(required / multiple) * multiple), a
 * DELIBERATELY NON-CONSERVING quantization whose surplus overage = orderQuantity − required is
 * recorded (never hidden), the full basis persisted and reconstructible, MOQ / multiple positive,
 * and the quantizer idempotent.
 * Spec: specs/order-multiple-quantization-l0.yaml (FAR 52.207-4 / 7.204 lot-sizing + CWE-682).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("ORDERQUANTIZATION")
class OrderQuantizationComplianceTest {

    @LocalServerPort int port;
    String member;

    @BeforeEach
    void setup() {
        member = OrderQuantizationTestSupport.obtainToken(
            OrderQuantizationTestSupport.freshEmail("oq-member"), "MEMBER");
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────
    private ExtractableResponse<Response> quantize(String itemRef, long required, long moq, long multiple) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"itemRef\":\"" + itemRef + "\",\"required\":" + required + ","
                + "\"moq\":" + moq + ",\"orderMultiple\":" + multiple + "}")
        .when().post("/api/order-quantization/quantizations").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> getRecord(String id) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/order-quantization/quantizations/" + id).then().statusCode(200).extract();
    }

    // ── ORDERQUANT-QUANTIZE-001 — orderQuantity = max(MOQ, ceil(required/multiple)*multiple) ──
    @Test @Tag("ORDERQUANT-QUANTIZE-001")
    void quantize_roundsUpToLotMultiple_flooredAtMoq() {
        // required=10, MOQ=1, multiple=1 → 10 (exact fit)
        assertThat(quantize("SKU-A", 10, 1, 1).statusCode()).isEqualTo(201);
        ExtractableResponse<Response> skuAResp = quantize("SKU-A", 10, 1, 1);
        assertThat(skuAResp.statusCode()).isEqualTo(201);
        assertThat(skuAResp.jsonPath().getLong("orderQuantity")).isEqualTo(10L);

        // required=23, MOQ=1, multiple=10 → ceil(23/10)=3 → 30
        ExtractableResponse<Response> skuBResp = quantize("SKU-B", 23, 1, 10);
        assertThat(skuBResp.statusCode()).isEqualTo(201);
        assertThat(skuBResp.jsonPath().getLong("orderQuantity")).isEqualTo(30L);

        // required=5, MOQ=50, multiple=10 → ceil(5/10)=1*10=10, max(50,10)=50 (MOQ floor dominates)
        ExtractableResponse<Response> skuCResp = quantize("SKU-C", 5, 50, 10);
        assertThat(skuCResp.statusCode()).isEqualTo(201);
        assertThat(skuCResp.jsonPath().getLong("orderQuantity")).isEqualTo(50L);

        // required=0, MOQ=25, multiple=5 → max(25, 0) = 25 (cannot place sub-MOQ order)
        ExtractableResponse<Response> skuDResp = quantize("SKU-D", 0, 25, 5);
        assertThat(skuDResp.statusCode()).isEqualTo(201);
        assertThat(skuDResp.jsonPath().getLong("orderQuantity")).isEqualTo(25L);

        // required=100, MOQ=1, multiple=12 → ceil(100/12)=9*12=108
        ExtractableResponse<Response> skuEResp = quantize("SKU-E", 100, 1, 12);
        assertThat(skuEResp.statusCode()).isEqualTo(201);
        assertThat(skuEResp.jsonPath().getLong("orderQuantity")).isEqualTo(108L);
    }

    // ── ORDERQUANT-OVERAGE-001 — NON-CONSERVING: orderQuantity >= required, overage recorded ──
    @Test @Tag("ORDERQUANT-OVERAGE-001")
    void overage_isRecorded_asOrderQuantityMinusRequired() {
        // required=23, multiple=10 → orderQuantity 30, overage 7
        ExtractableResponse<Response> r1 = quantize("SKU-OV1", 23, 1, 10);
        assertThat(r1.statusCode()).isEqualTo(201);
        assertThat(r1.jsonPath().getLong("orderQuantity")).isEqualTo(30L);
        assertThat(r1.jsonPath().getLong("overage")).isEqualTo(7L);
        assertThat(r1.jsonPath().getLong("overage"))
            .as("overage == orderQuantity - required")
            .isEqualTo(r1.jsonPath().getLong("orderQuantity") - r1.jsonPath().getLong("required"));

        // required=5, MOQ=50, multiple=10 → orderQuantity 50, overage 45
        ExtractableResponse<Response> r2 = quantize("SKU-OV2", 5, 50, 10);
        assertThat(r2.statusCode()).isEqualTo(201);
        assertThat(r2.jsonPath().getLong("orderQuantity")).isEqualTo(50L);
        assertThat(r2.jsonPath().getLong("overage")).isEqualTo(45L);

        // exact fit → overage 0 (still produced by the non-conserving quantizer)
        ExtractableResponse<Response> r3 = quantize("SKU-OV3", 10, 1, 1);
        assertThat(r3.statusCode()).isEqualTo(201);
        assertThat(r3.jsonPath().getLong("overage")).isEqualTo(0L);
        // the order is never SHORT — orderQuantity is always >= required (non-conserving direction)
        assertThat(r3.jsonPath().getLong("orderQuantity"))
            .isGreaterThanOrEqualTo(r3.jsonPath().getLong("required"));
    }

    // ── ORDERQUANT-BASIS-001 — the full basis is recorded and reconstructible ──
    @Test @Tag("ORDERQUANT-BASIS-001")
    void basis_isRecorded_andReconstructible() {
        ExtractableResponse<Response> basisResp = quantize("SKU-BASIS", 23, 5, 10);
        assertThat(basisResp.statusCode()).isEqualTo(201);
        String id = basisResp.jsonPath().getString("id");
        ExtractableResponse<Response> got = getRecord(id);
        // every basis member is present
        assertThat(got.jsonPath().getString("itemRef")).isEqualTo("SKU-BASIS");
        assertThat(got.jsonPath().getLong("required")).isEqualTo(23L);
        assertThat(got.jsonPath().getLong("moq")).isEqualTo(5L);
        assertThat(got.jsonPath().getLong("orderMultiple")).isEqualTo(10L);
        assertThat(got.jsonPath().getLong("orderQuantity")).isEqualTo(30L);
        assertThat(got.jsonPath().getLong("overage")).isEqualTo(7L);
        assertThat(got.jsonPath().getString("createdAt")).as("the recorded basis carries its instant").isNotBlank();
        // re-running the deterministic quantizer on the recorded inputs reproduces the result
        ExtractableResponse<Response> replay = quantize("SKU-BASIS", 23, 5, 10);
        assertThat(replay.statusCode()).isEqualTo(201);
        assertThat(replay.jsonPath().getLong("orderQuantity")).isEqualTo(got.jsonPath().getLong("orderQuantity"));
        assertThat(replay.jsonPath().getLong("overage")).isEqualTo(got.jsonPath().getLong("overage"));
    }

    // ── ORDERQUANT-CONSTRAINT-001 — MOQ / multiple positive, required non-negative ──
    @Test @Tag("ORDERQUANT-CONSTRAINT-001")
    void constraint_rejectsNonPositiveMoqOrMultiple_andNegativeRequired() {
        ExtractableResponse<Response> zeroMoq = quantize("SKU-X", 10, 0, 5);
        assertThat(zeroMoq.statusCode()).isEqualTo(422);
        assertThat(zeroMoq.jsonPath().getString("code")).isEqualTo("ORDERQUANT_INVALID_CONSTRAINT");

        ExtractableResponse<Response> zeroMultiple = quantize("SKU-X", 10, 1, 0);
        assertThat(zeroMultiple.statusCode()).isEqualTo(422);
        assertThat(zeroMultiple.jsonPath().getString("code")).isEqualTo("ORDERQUANT_INVALID_CONSTRAINT");

        // a negative required is rejected by bean validation at the boundary (400) — never reaches the quantizer
        ExtractableResponse<Response> negRequired = quantize("SKU-X", -1, 1, 5);
        assertThat(negRequired.statusCode()).isEqualTo(400);
    }

    // ── ORDERQUANT-IDEMPOTENT-001 — same (required, MOQ, multiple) → same orderQuantity ──
    @Test @Tag("ORDERQUANT-IDEMPOTENT-001")
    void idempotent_sameInputsSameResult() {
        ExtractableResponse<Response> q1Resp = quantize("SKU-IDEM", 23, 1, 10);
        assertThat(q1Resp.statusCode()).isEqualTo(201);
        long q1 = q1Resp.jsonPath().getLong("orderQuantity");
        ExtractableResponse<Response> q2Resp = quantize("SKU-IDEM", 23, 1, 10);
        assertThat(q2Resp.statusCode()).isEqualTo(201);
        long q2 = q2Resp.jsonPath().getLong("orderQuantity");
        ExtractableResponse<Response> q3Resp = quantize("SKU-IDEM", 23, 1, 10);
        assertThat(q3Resp.statusCode()).isEqualTo(201);
        long q3 = q3Resp.jsonPath().getLong("orderQuantity");
        assertThat(q1).isEqualTo(30L);
        assertThat(q2).isEqualTo(q1);
        assertThat(q3).isEqualTo(q1);

        ExtractableResponse<Response> ov1Resp = quantize("SKU-IDEM2", 5, 50, 10);
        assertThat(ov1Resp.statusCode()).isEqualTo(201);
        long ov1 = ov1Resp.jsonPath().getLong("overage");
        ExtractableResponse<Response> ov2Resp = quantize("SKU-IDEM2", 5, 50, 10);
        assertThat(ov2Resp.statusCode()).isEqualTo(201);
        long ov2 = ov2Resp.jsonPath().getLong("overage");
        assertThat(ov1).isEqualTo(45L);
        assertThat(ov2).isEqualTo(ov1);
    }
}
