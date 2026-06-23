package com.ax.template.authblueprint.uomconversion;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * dimensional-uom-conversion-l0 compliance — verified against the live uom-conversion reference
 * workload. The invariant: a conversion is a pure ratio only within one dimension; a cross-dimension
 * conversion requires a recorded versioned bridging material property (density / unit-weight) — else
 * 422 INCOMPATIBLE_DIMENSIONS, never a silent wrong number; every conversion records its full
 * reconstructible basis; the arithmetic is deterministic BigDecimal at a recorded scale.
 * Spec: specs/dimensional-uom-conversion-l0.yaml (NIST SP 811 §7.1/§7.14 + CWE-682).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("UOMCONVERSION")
class UomConversionComplianceTest {

    @LocalServerPort int port;
    String member;

    @BeforeEach
    void setup() {
        UomConversionTestSupport.useRandomPort(port);
        member = UomConversionTestSupport.obtainToken(UomConversionTestSupport.freshEmail("uom-member"), "MEMBER");
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────
    private String registerMaterial(String ref) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"materialRef\":\"" + ref + "\"}")
        .when().post("/api/uom-conversion/materials").then().statusCode(201).extract().path("id");
    }

    private ExtractableResponse<Response> recordProperty(String materialId, String fromDim, String toDim, String factor) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"fromDimension\":\"" + fromDim + "\",\"toDimension\":\"" + toDim + "\",\"factor\":" + factor + "}")
        .when().post("/api/uom-conversion/materials/" + materialId + "/properties").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> convert(String materialId, String body) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body(body)
        .when().post("/api/uom-conversion/materials/" + materialId + "/conversions").thenReturn().then().extract();
    }

    // ── UOMCONV-COMPAT-001 — same-dimension ratio; cross-dimension needs a bridge; else 422 ──
    @Test @Tag("UOMCONV-COMPAT-001")
    void sameDimensionRatio_crossDimensionWithoutBridge_is422_namingBothDimensions() {
        String material = registerMaterial("MAT-COMPAT");

        // same dimension kg → g : pure ratio, no material property needed
        ExtractableResponse<Response> kgToG =
            convert(material, "{\"fromQuantity\":2,\"fromUnit\":\"KG\",\"toUnit\":\"G\"}");
        assertThat(kgToG.statusCode()).isEqualTo(200);
        assertThat(kgToG.jsonPath().getString("mode")).isEqualTo("SAME_DIMENSION");
        assertThat(new BigDecimal(kgToG.jsonPath().getString("toQuantity")))
            .isEqualByComparingTo("2000");

        // cross dimension L → kg with NO recorded material property → 422 INCOMPATIBLE_DIMENSIONS
        ExtractableResponse<Response> noBridge =
            convert(material, "{\"fromQuantity\":2,\"fromUnit\":\"L\",\"toUnit\":\"KG\"}");
        assertThat(noBridge.statusCode()).isEqualTo(422);
        assertThat(noBridge.jsonPath().getString("code")).isEqualTo("INCOMPATIBLE_DIMENSIONS");
        assertThat(noBridge.jsonPath().getString("detail")).contains("VOLUME").contains("MASS");

        // an unknown unit → 422 UNKNOWN_UNIT
        ExtractableResponse<Response> unknown =
            convert(material, "{\"fromQuantity\":2,\"fromUnit\":\"FURLONG\",\"toUnit\":\"KG\"}");
        assertThat(unknown.statusCode()).isEqualTo(422);
        assertThat(unknown.jsonPath().getString("code")).isEqualTo("UNKNOWN_UNIT");
    }

    // ── UOMCONV-MATERIAL-001 — a density bridges volume→mass: mass = volume × density ──
    @Test @Tag("UOMCONV-MATERIAL-001")
    void densityBridgesVolumeToMass() {
        String material = registerMaterial("MAT-MILK");
        // density 1.03 kg/L (VOLUME → MASS)
        assertThat(recordProperty(material, "VOLUME", "MASS", "1.03").statusCode()).isEqualTo(201);

        // 2 L → kg = 2 × 1.03 = 2.06 kg
        ExtractableResponse<Response> lToKg =
            convert(material, "{\"fromQuantity\":2,\"fromUnit\":\"L\",\"toUnit\":\"KG\"}");
        assertThat(lToKg.statusCode()).isEqualTo(200);
        assertThat(lToKg.jsonPath().getString("mode")).isEqualTo("CROSS_DIMENSION");
        assertThat(new BigDecimal(lToKg.jsonPath().getString("toQuantity"))).isEqualByComparingTo("2.06");
        assertThat(new BigDecimal(lToKg.jsonPath().getString("factor"))).isEqualByComparingTo("1.03");

        // 500 mL → g = 0.5 L × 1.03 = 0.515 kg = 515 g
        ExtractableResponse<Response> mlToG =
            convert(material, "{\"fromQuantity\":500,\"fromUnit\":\"ML\",\"toUnit\":\"G\"}");
        assertThat(mlToG.statusCode()).isEqualTo(200);
        assertThat(new BigDecimal(mlToG.jsonPath().getString("toQuantity"))).isEqualByComparingTo("515");
    }

    // ── UOMCONV-BASIS-001 — the conversion records its full reconstructible basis ──
    @Test @Tag("UOMCONV-BASIS-001")
    void conversionRecordsFullReconstructibleBasis() {
        String material = registerMaterial("MAT-BASIS");
        recordProperty(material, "VOLUME", "MASS", "0.92");
        String conversionId =
            convert(material, "{\"fromQuantity\":10,\"fromUnit\":\"L\",\"toUnit\":\"KG\"}")
                .jsonPath().getString("id");

        ExtractableResponse<Response> rec = given().header("Authorization", "Bearer " + member)
            .when().get("/api/uom-conversion/conversions/" + conversionId)
            .then().statusCode(200).extract();
        assertThat(new BigDecimal(rec.jsonPath().getString("fromQuantity"))).isEqualByComparingTo("10");
        assertThat(rec.jsonPath().getString("fromUnit")).isEqualTo("L");
        assertThat(rec.jsonPath().getString("toUnit")).isEqualTo("KG");
        assertThat(rec.jsonPath().getString("fromDimension")).isEqualTo("VOLUME");
        assertThat(rec.jsonPath().getString("toDimension")).isEqualTo("MASS");
        assertThat(rec.jsonPath().getString("mode")).isEqualTo("CROSS_DIMENSION");
        assertThat(new BigDecimal(rec.jsonPath().getString("factor"))).isEqualByComparingTo("0.92");
        assertThat(rec.jsonPath().getLong("materialVersion")).isEqualTo(1L);
        assertThat(rec.jsonPath().getInt("resultScale")).isEqualTo(6);
        // re-derivable: 10 L × 0.92 = 9.2 kg
        assertThat(new BigDecimal(rec.jsonPath().getString("toQuantity"))).isEqualByComparingTo("9.2");
    }

    // ── UOMCONV-DETERMINISM-001 — same inputs + version → same result + same record (idempotent) ──
    @Test @Tag("UOMCONV-DETERMINISM-001")
    void conversionIsDeterministicAndIdempotent() {
        String material = registerMaterial("MAT-DET");
        recordProperty(material, "VOLUME", "MASS", "1.03");

        ExtractableResponse<Response> first =
            convert(material, "{\"fromQuantity\":2,\"fromUnit\":\"L\",\"toUnit\":\"KG\"}");
        ExtractableResponse<Response> second =
            convert(material, "{\"fromQuantity\":2,\"fromUnit\":\"L\",\"toUnit\":\"KG\"}");

        assertThat(first.statusCode()).isEqualTo(200);
        assertThat(second.statusCode()).isEqualTo(200);
        // byte-identical result AND the same recorded conversion id (idempotent replay)
        assertThat(new BigDecimal(second.jsonPath().getString("toQuantity")))
            .isEqualByComparingTo(new BigDecimal(first.jsonPath().getString("toQuantity")));
        assertThat(second.jsonPath().getString("id")).isEqualTo(first.jsonPath().getString("id"));
    }

    // ── UOMCONV-VERSION-001 — a corrected density is a new version; an old conversion keeps citing its version ──
    @Test @Tag("UOMCONV-VERSION-001")
    void correctedDensityIsNewVersion_priorConversionKeepsItsVersion() {
        String material = registerMaterial("MAT-VER");
        recordProperty(material, "VOLUME", "MASS", "1.03");   // version 1

        // a conversion pinned to v1
        ExtractableResponse<Response> atV1 =
            convert(material, "{\"fromQuantity\":2,\"fromUnit\":\"L\",\"toUnit\":\"KG\",\"materialVersion\":1}");
        assertThat(atV1.statusCode()).isEqualTo(200);
        assertThat(atV1.jsonPath().getLong("materialVersion")).isEqualTo(1L);
        assertThat(new BigDecimal(atV1.jsonPath().getString("toQuantity"))).isEqualByComparingTo("2.06");

        // append a corrected density (version 2)
        ExtractableResponse<Response> v2 = recordProperty(material, "VOLUME", "MASS", "1.05");
        assertThat(v2.statusCode()).isEqualTo(201);
        assertThat(v2.jsonPath().getLong("version")).isEqualTo(2L);

        // re-deriving the v1 conversion still uses v1's factor (idempotent verbatim)
        ExtractableResponse<Response> reV1 =
            convert(material, "{\"fromQuantity\":2,\"fromUnit\":\"L\",\"toUnit\":\"KG\",\"materialVersion\":1}");
        assertThat(reV1.jsonPath().getString("id")).isEqualTo(atV1.jsonPath().getString("id"));
        assertThat(new BigDecimal(reV1.jsonPath().getString("factor"))).isEqualByComparingTo("1.03");

        // a fresh conversion at the current version uses v2's factor: 2 L × 1.05 = 2.10 kg
        ExtractableResponse<Response> atV2 =
            convert(material, "{\"fromQuantity\":2,\"fromUnit\":\"L\",\"toUnit\":\"KG\"}");
        assertThat(atV2.jsonPath().getLong("materialVersion")).isEqualTo(2L);
        assertThat(new BigDecimal(atV2.jsonPath().getString("toQuantity"))).isEqualByComparingTo("2.10");

        // a non-existent version → 422 UNKNOWN_MATERIAL_VERSION
        ExtractableResponse<Response> badVersion =
            convert(material, "{\"fromQuantity\":2,\"fromUnit\":\"L\",\"toUnit\":\"KG\",\"materialVersion\":99}");
        assertThat(badVersion.statusCode()).isEqualTo(422);
        assertThat(badVersion.jsonPath().getString("code")).isEqualTo("UNKNOWN_MATERIAL_VERSION");

        // the property history is append-only — both versions preserved
        var versions = given().header("Authorization", "Bearer " + member)
            .when().get("/api/uom-conversion/materials/" + material + "/properties")
            .then().statusCode(200).extract().jsonPath().getList("version");
        assertThat(versions).containsExactlyInAnyOrder(1, 2);
    }
}
