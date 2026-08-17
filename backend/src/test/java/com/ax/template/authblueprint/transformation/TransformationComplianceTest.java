package com.ax.template.authblueprint.transformation;

import io.restassured.RestAssured;
import io.restassured.config.JsonConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.path.json.config.JsonPathConfig;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * transformation-conservation-l0 compliance — verified against the live transformation reference
 * workload. The invariant is conserve-to-CLASSIFIED-residual (the dual of balanced-posting's
 * net-zero): Σinput == Σgood + Σresidual exactly, every residual carries a governed disposition.
 * Spec: specs/transformation-conservation-l0.yaml.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("TRANSFORMATION")
class TransformationComplianceTest {

    @LocalServerPort int port;
    String member;

    @BeforeEach
    void setup() {
        RestAssured.config = RestAssuredConfig.config().jsonConfig(
            JsonConfig.jsonConfig().numberReturnType(JsonPathConfig.NumberReturnType.BIG_DECIMAL));
        member = TransformationTestSupport.obtainToken(TransformationTestSupport.freshEmail("xf-member"), "MEMBER");
    }

    private ExtractableResponse<Response> record(String body) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body(body)
        .when().post("/api/transformations").thenReturn().then().extract();
    }

    // ── XFORM-ACCOUNTED-LOSS-001 / XFORM-RESIDUAL-CLASSIFIED-001 — conserving record with classified loss ──
    @Test @Tag("XFORM-ACCOUNTED-LOSS-001") @Tag("XFORM-RESIDUAL-CLASSIFIED-001")
    void record_conservingWithClassifiedResidual_201() {
        ExtractableResponse<Response> r = record("{"
            + "\"inputs\":[{\"materialCode\":\"flour\",\"qty\":100.00,\"unit\":\"kg\"}],"
            + "\"goodOutputs\":[{\"materialCode\":\"bread\",\"qty\":90.00,\"unit\":\"kg\"}],"
            + "\"residuals\":[{\"materialCode\":\"trim\",\"qty\":7.00,\"unit\":\"kg\",\"disposition\":\"SCRAP\"},"
            + "{\"materialCode\":\"evap\",\"qty\":3.00,\"unit\":\"kg\",\"disposition\":\"YIELD_LOSS\"}]}");
        assertThat(r.statusCode()).isEqualTo(201);
        assertThat((BigDecimal) r.path("totalInput")).isEqualByComparingTo("100.00");
        assertThat((BigDecimal) r.path("totalGood")).isEqualByComparingTo("90.00");
        assertThat((BigDecimal) r.path("totalResidual")).isEqualByComparingTo("10.00");
        // conservation: input == good + residual
        assertThat(((BigDecimal) r.path("totalGood")).add(r.path("totalResidual")))
            .isEqualByComparingTo("100.00");
        // XFORM-ATOMIC-001: all four legs persisted together (1 input + 1 good + 2 residual)
        String id = ((Object) r.path("id")).toString();
        assertThat(r.jsonPath().getInt("legs.size()")).isEqualTo(4);
        BigDecimal persistedResidual = given().header("Authorization", "Bearer " + member)
            .when().get("/api/transformations/" + id).then().statusCode(200)
            .extract().path("totalResidual");
        assertThat(persistedResidual).isEqualByComparingTo("10.00");
    }

    // ── XFORM-ACCOUNTED-LOSS-001 — an imbalance is rejected, nothing persisted ──
    @Test @Tag("XFORM-ACCOUNTED-LOSS-001") @Tag("XFORM-ATOMIC-001")
    void record_imbalance_422_persistsNothing() {
        ExtractableResponse<Response> r = record("{"
            + "\"inputs\":[{\"materialCode\":\"flour\",\"qty\":100.00,\"unit\":\"kg\"}],"
            + "\"goodOutputs\":[{\"materialCode\":\"bread\",\"qty\":90.00,\"unit\":\"kg\"}],"
            + "\"residuals\":[{\"materialCode\":\"trim\",\"qty\":5.00,\"unit\":\"kg\",\"disposition\":\"SCRAP\"}]}");
        assertThat(r.statusCode()).isEqualTo(422);          // 95 != 100
        assertThat(r.path("code").toString()).isEqualTo("XFORM_NOT_CONSERVED");
        assertThat((Object) r.path("id")).as("a rejected transformation returns no persisted run").isNull();
    }

    // ── XFORM-RESIDUAL-CLASSIFIED-001 — a residual with no / unknown disposition is rejected ──
    @Test @Tag("XFORM-RESIDUAL-CLASSIFIED-001")
    void record_unclassifiedOrUnknownResidual_rejected() {
        // null disposition -> 400 at the validation boundary (@NotNull)
        assertThat(record("{"
            + "\"inputs\":[{\"materialCode\":\"a\",\"qty\":10.00,\"unit\":\"kg\"}],"
            + "\"goodOutputs\":[{\"materialCode\":\"b\",\"qty\":8.00,\"unit\":\"kg\"}],"
            + "\"residuals\":[{\"materialCode\":\"c\",\"qty\":2.00,\"unit\":\"kg\"}]}").statusCode())
            .isEqualTo(400);
        // unknown disposition value -> 400 (enum bind fail)
        assertThat(record("{"
            + "\"inputs\":[{\"materialCode\":\"a\",\"qty\":10.00,\"unit\":\"kg\"}],"
            + "\"goodOutputs\":[{\"materialCode\":\"b\",\"qty\":8.00,\"unit\":\"kg\"}],"
            + "\"residuals\":[{\"materialCode\":\"c\",\"qty\":2.00,\"unit\":\"kg\",\"disposition\":\"MISC\"}]}").statusCode())
            .isEqualTo(400);
    }

    // ── XFORM-DIMENSION-001 — mixed units without a pinned conversion are rejected ──
    @Test @Tag("XFORM-DIMENSION-001")
    void record_mixedUnits_422() {
        ExtractableResponse<Response> r = record("{"
            + "\"inputs\":[{\"materialCode\":\"flour\",\"qty\":100.00,\"unit\":\"kg\"}],"
            + "\"goodOutputs\":[{\"materialCode\":\"loaves\",\"qty\":90.00,\"unit\":\"ea\"}],"
            + "\"residuals\":[{\"materialCode\":\"trim\",\"qty\":10.00,\"unit\":\"kg\",\"disposition\":\"SCRAP\"}]}");
        assertThat(r.statusCode()).isEqualTo(422);
        assertThat(r.path("code").toString()).isEqualTo("XFORM_MIXED_UNIT");
    }

    // ── over-precise quantity (scale > 4) is a clean 400 at the boundary, never a setScale 500 ──
    @Test @Tag("XFORM-ACCOUNTED-LOSS-001")
    void record_overPreciseQuantity_400_notServerError() {
        ExtractableResponse<Response> r = record("{"
            + "\"inputs\":[{\"materialCode\":\"a\",\"qty\":1.23456,\"unit\":\"kg\"}],"
            + "\"goodOutputs\":[{\"materialCode\":\"b\",\"qty\":1.00000,\"unit\":\"kg\"}],"
            + "\"residuals\":[{\"materialCode\":\"c\",\"qty\":0.23456,\"unit\":\"kg\",\"disposition\":\"SCRAP\"}]}");
        assertThat(r.statusCode()).as("over-scale qty -> clean 400, not 500").isEqualTo(400);
    }

    // ── all-residual transformation (total loss) conserves ──
    @Test @Tag("XFORM-ACCOUNTED-LOSS-001")
    void record_allResidual_totalLoss_conserves() {
        ExtractableResponse<Response> r = record("{"
            + "\"inputs\":[{\"materialCode\":\"batch\",\"qty\":50.00,\"unit\":\"kg\"}],"
            + "\"goodOutputs\":[],"
            + "\"residuals\":[{\"materialCode\":\"reject\",\"qty\":50.00,\"unit\":\"kg\",\"disposition\":\"REWORK\"}]}");
        assertThat(r.statusCode()).isEqualTo(201);
        assertThat((BigDecimal) r.path("totalGood")).isEqualByComparingTo("0.00");
        assertThat((BigDecimal) r.path("totalResidual")).isEqualByComparingTo("50.00");
    }

    // ── RBAC + IDOR ──
    @Test
    void rbac_unauthIs401_unknownIs404() {
        given().header("Content-Type", "application/json")
            .body("{\"inputs\":[{\"materialCode\":\"a\",\"qty\":1.00,\"unit\":\"kg\"}],\"goodOutputs\":[{\"materialCode\":\"b\",\"qty\":1.00,\"unit\":\"kg\"}]}")
        .when().post("/api/transformations").then().statusCode(401);

        given().header("Authorization", "Bearer " + member)
        .when().get("/api/transformations/" + UUID.randomUUID())
        .then().statusCode(404).body("type", equalTo("urn:problem:not-found"));
    }
}
