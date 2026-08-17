package com.ax.template.authblueprint.divisibility;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * material-divisibility-constraint-l0 compliance — verified against the live divisibility reference
 * workload. The invariant: a per-material divisibility policy (INTEGER_ONLY / FRACTIONAL) that
 * REJECTS — never rounds — a quantity it forbids (422 NON_INTEGRAL_QUANTITY / EXCESS_PRECISION),
 * tests integrality with exact stripTrailingZeros (format-independent), records the policy as a
 * versioned per-material property, and records every check with the policy version in force.
 * Spec: specs/material-divisibility-constraint-l0.yaml (CWE-682 + CWE-1339 + discrete/continuous variable).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("DIVISIBILITY")
class DivisibilityComplianceTest {

    @LocalServerPort int port;
    @Autowired DivisibilityService service;
    String member;

    @BeforeEach
    void setup() {
        member = DivisibilityTestSupport.obtainToken(DivisibilityTestSupport.freshEmail("div-member"), "MEMBER");
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────
    private ExtractableResponse<Response> declare(String materialRef, String kind, int maxScale) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"materialRef\":\"" + materialRef + "\",\"kind\":\"" + kind + "\",\"maxScale\":" + maxScale + "}")
        .when().post("/api/divisibility/policies").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> check(String materialRef, String quantity) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"quantity\":" + quantity + "}")
        .when().post("/api/divisibility/materials/" + materialRef + "/checks").thenReturn().then().extract();
    }

    // ── DIV-INTEGRAL-001 — INTEGER_ONLY rejects a fractional quantity 422; never rounds; whole accepted ──
    @Test @Tag("DIV-INTEGRAL-001")
    void integerOnly_rejectsFractional_422_neverRounds_wholeAccepted() {
        String m = DivisibilityTestSupport.freshMaterial("M-INT");
        assertThat(declare(m, "INTEGER_ONLY", 0).statusCode()).isEqualTo(201);

        // a whole quantity is ACCEPTED
        ExtractableResponse<Response> whole = check(m, "3");
        assertThat(whole.statusCode()).isEqualTo(200);
        assertThat(whole.jsonPath().getString("verdict")).isEqualTo("ACCEPTED");

        // a whole quantity with trailing zeros is STILL integral → ACCEPTED
        ExtractableResponse<Response> wholeTrailingZeros = check(m, "3.00");
        assertThat(wholeTrailingZeros.statusCode()).isEqualTo(200);
        assertThat(wholeTrailingZeros.jsonPath().getString("verdict")).isEqualTo("ACCEPTED");

        // a fractional quantity → 422 NON_INTEGRAL_QUANTITY naming the material, NOT rounded
        ExtractableResponse<Response> frac = check(m, "2.5");
        assertThat(frac.statusCode()).isEqualTo(422);
        assertThat(frac.jsonPath().getString("code")).isEqualTo("NON_INTEGRAL_QUANTITY");
        assertThat(frac.jsonPath().getString("detail")).as("the material is named").contains(m);
        // the rejection MUST NOT echo a silently-rounded 3 — the submitted 2.5 was refused, not changed
        assertThat(frac.jsonPath().getString("detail")).contains("2.5").doesNotContain("rounded to 3");
    }

    // ── DIV-PRECISION-001 — FRACTIONAL enforces max scale; excess precision 422; within-scale accepts ──
    @Test @Tag("DIV-PRECISION-001")
    void fractional_enforcesMaxScale_excessPrecision422_withinScaleAccepts() {
        String m = DivisibilityTestSupport.freshMaterial("M-FRAC");
        assertThat(declare(m, "FRACTIONAL", 3).statusCode()).isEqualTo(201);

        // a quantity within max scale (stripped scale 2 <= 3) → ACCEPTED
        ExtractableResponse<Response> within = check(m, "1.250");
        assertThat(within.statusCode()).isEqualTo(200);
        assertThat(within.jsonPath().getString("verdict")).isEqualTo("ACCEPTED");

        // a whole quantity (scale 0) is within any non-negative max scale → ACCEPTED
        ExtractableResponse<Response> wholeScaleZero = check(m, "5");
        assertThat(wholeScaleZero.statusCode()).isEqualTo(200);
        assertThat(wholeScaleZero.jsonPath().getString("verdict")).isEqualTo("ACCEPTED");

        // a quantity above max scale → 422 EXCESS_PRECISION, NOT silently truncated
        ExtractableResponse<Response> excess = check(m, "1.2345");
        assertThat(excess.statusCode()).isEqualTo(422);
        assertThat(excess.jsonPath().getString("code")).isEqualTo("EXCESS_PRECISION");
        assertThat(excess.jsonPath().getString("detail")).contains(m).contains("1.2345");
        assertThat(excess.jsonPath().getString("detail")).doesNotContain("truncated to 1.234");
    }

    // ── DIV-POLICY-001 — recorded versioned per-material policy; a re-declaration appends a version ──
    @Test @Tag("DIV-POLICY-001")
    void policy_isVersioned_reDeclarationAppends_priorRetained() {
        String m = DivisibilityTestSupport.freshMaterial("M-VER");
        ExtractableResponse<Response> v1 = declare(m, "INTEGER_ONLY", 0);
        assertThat(v1.statusCode()).isEqualTo(201);
        assertThat(v1.jsonPath().getLong("policyVersion")).isEqualTo(1L);
        assertThat(v1.jsonPath().getString("kind")).isEqualTo("INTEGER_ONLY");

        ExtractableResponse<Response> v2 = declare(m, "FRACTIONAL", 2);
        assertThat(v2.statusCode()).isEqualTo(201);
        assertThat(v2.jsonPath().getLong("policyVersion")).isEqualTo(2L);

        // the current policy is now version 2 (FRACTIONAL)
        ExtractableResponse<Response> current = given().header("Authorization", "Bearer " + member)
            .when().get("/api/divisibility/materials/" + m + "/policy").then().statusCode(200).extract();
        assertThat(current.jsonPath().getLong("policyVersion")).isEqualTo(2L);
        assertThat(current.jsonPath().getString("kind")).isEqualTo("FRACTIONAL");
        assertThat(current.jsonPath().getInt("maxScale")).isEqualTo(2);

        // the full history retains version 1 (INTEGER_ONLY), oldest first — never overwritten
        ExtractableResponse<Response> history = given().header("Authorization", "Bearer " + member)
            .when().get("/api/divisibility/materials/" + m + "/policy-history").then().statusCode(200).extract();
        List<Object> versions = history.jsonPath().getList("policyVersion");
        assertThat(versions).hasSize(2);
        assertThat(history.jsonPath().getLong("[0].policyVersion")).isEqualTo(1L);
        assertThat(history.jsonPath().getString("[0].kind")).isEqualTo("INTEGER_ONLY");
        assertThat(history.jsonPath().getLong("[1].policyVersion")).isEqualTo(2L);

        // a check after the re-declaration records version 2
        ExtractableResponse<Response> atPolicyV2 = check(m, "1.50");
        assertThat(atPolicyV2.statusCode()).isEqualTo(200);
        assertThat(atPolicyV2.jsonPath().getLong("policyVersion")).isEqualTo(2L);
    }

    // ── DIV-RECORD-001 — every check recorded immutably with verdict + policy version + verbatim qty ──
    @Test @Tag("DIV-RECORD-001")
    void everyCheck_recorded_withVerdict_policyVersion_andVerbatimQuantity() {
        String m = DivisibilityTestSupport.freshMaterial("M-REC");
        declare(m, "INTEGER_ONLY", 0);

        assertThat(check(m, "4").statusCode()).isEqualTo(200);          // ACCEPTED, recorded
        assertThat(check(m, "2.5").statusCode()).isEqualTo(422);        // NON_INTEGRAL, STILL recorded

        ExtractableResponse<Response> checks = given().header("Authorization", "Bearer " + member)
            .when().get("/api/divisibility/materials/" + m + "/checks").then().statusCode(200).extract();
        List<String> verdicts = checks.jsonPath().getList("verdict");
        assertThat(verdicts).containsExactly("ACCEPTED", "NON_INTEGRAL");   // the rejection survived the 422
        // the rejected quantity is recorded VERBATIM (2.5), not normalized
        assertThat(new BigDecimal(checks.jsonPath().getString("[1].submittedQuantity")))
            .isEqualByComparingTo(new BigDecimal("2.5"));
        // both checks carry the policy version in force (version 1)
        assertThat(checks.jsonPath().getLong("[0].policyVersion")).isEqualTo(1L);
        assertThat(checks.jsonPath().getLong("[1].policyVersion")).isEqualTo(1L);
    }

    // ── DIV-DETERMINISM-001 — integrality via stripTrailingZeros; 5 == 5.0 == 5.00; format-independent ──
    @Test @Tag("DIV-DETERMINISM-001")
    void integrality_isFormatIndependent_viaStripTrailingZeros() {
        String mInt = DivisibilityTestSupport.freshMaterial("M-DET-INT");
        declare(mInt, "INTEGER_ONLY", 0);
        for (String integral : new String[]{"5", "5.0", "5.00", "5.000000"}) {
            ExtractableResponse<Response> integralCheck = check(mInt, integral);
            assertThat(integralCheck.statusCode()).isEqualTo(200);
            assertThat(integralCheck.jsonPath().getString("verdict"))
                .as(integral + " is integral").isEqualTo("ACCEPTED");
        }
        assertThat(check(mInt, "5.5").statusCode()).isEqualTo(422);

        String mFrac = DivisibilityTestSupport.freshMaterial("M-DET-FRAC");
        declare(mFrac, "FRACTIONAL", 2);
        // 1.250 has effective (stripped) scale 2 → ACCEPTED; 1.255 has scale 3 → EXCESS_PRECISION
        ExtractableResponse<Response> withinScale = check(mFrac, "1.250");
        assertThat(withinScale.statusCode()).isEqualTo(200);
        assertThat(withinScale.jsonPath().getString("verdict")).isEqualTo("ACCEPTED");
        assertThat(check(mFrac, "1.255").statusCode()).isEqualTo(422);
        ExtractableResponse<Response> excessScale = check(mFrac, "1.255");
        assertThat(excessScale.statusCode()).isEqualTo(422);
        assertThat(excessScale.jsonPath().getString("code")).isEqualTo("EXCESS_PRECISION");
    }

    // ── AUTHZ / IDOR — unauthenticated rejected 401; a check against an undeclared material is 404 ──
    @Test @Tag("DIV-RECORD-001")
    void unauthenticated401_andUndeclaredMaterial404() {
        String m = DivisibilityTestSupport.freshMaterial("M-AUTHZ");
        // unauthenticated declare → 401
        assertThat(given().header("Content-Type", "application/json")
            .body("{\"materialRef\":\"" + m + "\",\"kind\":\"INTEGER_ONLY\",\"maxScale\":0}")
        .when().post("/api/divisibility/policies").thenReturn().statusCode()).isEqualTo(401);

        // a check against a material with no declared policy → 404 (IDOR-safe; never leaks existence)
        ExtractableResponse<Response> notDeclared = check(DivisibilityTestSupport.freshMaterial("M-NONE"), "1");
        assertThat(notDeclared.statusCode()).isEqualTo(404);
        assertThat(notDeclared.jsonPath().getString("code")).isEqualTo("RESOURCE_NOT_FOUND");
    }

    // ── REJECT-not-round contract — confirmed through the service: ACCEPTED returns the SAME number ──
    @Test @Tag("DIV-INTEGRAL-001")
    void acceptedQuantity_isReturnedUnchanged_notQuantized() {
        String m = DivisibilityTestSupport.freshMaterial("M-UNCHANGED");
        declare(m, "FRACTIONAL", 4);
        ExtractableResponse<Response> ok = check(m, "12.3400");
        assertThat(ok.statusCode()).isEqualTo(200);
        // the accepted quantity is recorded verbatim — never rounded up to a lot multiple (that is
        // order-multiple-quantization's distinct job; this gate never changes the number).
        assertThat(new BigDecimal(ok.jsonPath().getString("submittedQuantity")))
            .isEqualByComparingTo(new BigDecimal("12.34"));
    }
}
