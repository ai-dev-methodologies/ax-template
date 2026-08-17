package com.ax.template.authblueprint.piecewisedeadband;

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

/**
 * piecewise-deadband-l0 compliance — verified against the live piecewisedeadband reference workload. The
 * invariant: segments tile the domain exactly (no gap/overlap); an evaluation resolves the covering
 * segment and compares against ITS OWN target/deadband, recording a signed deviation; evaluations are
 * append-only and idempotent by input hash. Spec: specs/piecewise-deadband-l0.yaml.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("PIECEWISE_DEADBAND")
class DeadbandComplianceTest {

    @LocalServerPort int port;
    String member;

    @BeforeEach
    void setup() {
        member = DeadbandTestSupport.obtainToken(DeadbandTestSupport.freshEmail("pwdb-member"), "MEMBER");
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────
    private String segmentJson(String start, String end, String target, String width) {
        return "{\"start\":" + start + ",\"end\":" + end + ",\"obligationTarget\":" + target
            + ",\"deadbandWidth\":" + width + "}";
    }

    private ExtractableResponse<Response> createConfigRaw(String key, String domainStart, String domainEnd,
                                                          String segmentsJson) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"configKey\":\"" + key + "\",\"domainStart\":" + domainStart + ",\"domainEnd\":" + domainEnd
                + ",\"segments\":" + segmentsJson + "}")
        .when().post("/api/piecewise-deadband/configs").thenReturn().then().extract();
    }

    /** [0,50) target=10 width=2 ; [50,100) target=20 width=5 — two DIFFERENT deadband widths. */
    private String twoTierSegments() {
        return "[" + segmentJson("0", "50", "10", "2") + "," + segmentJson("50", "100", "20", "5") + "]";
    }

    private String createTwoTierConfig(String key) {
        ExtractableResponse<Response> res = createConfigRaw(key, "0", "100", twoTierSegments());
        assertThat(res.statusCode()).isEqualTo(201);
        return key;
    }

    private ExtractableResponse<Response> evaluate(String key, String pointX, String actual) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"pointX\":" + pointX + ",\"actualValue\":" + actual + "}")
        .when().post("/api/piecewise-deadband/configs/" + key + "/evaluations").thenReturn().then().extract();
    }

    // ── PWDB-SEGMENT-001 — exact tiling → 201; gap / overlap / short span → 422 ──
    @Test @Tag("PWDB-SEGMENT-001")
    void segmentsMustTileExactly_gapOverlapAndShortSpanAreRejected() {
        String key = "pwdb-" + UUID.randomUUID();
        assertThat(createConfigRaw(key, "0", "100", twoTierSegments()).statusCode()).isEqualTo(201);

        // gap: [0,40) + [50,100) leaves (40,50) undefined
        ExtractableResponse<Response> gap = createConfigRaw("pwdb-" + UUID.randomUUID(), "0", "100",
            "[" + segmentJson("0", "40", "10", "2") + "," + segmentJson("50", "100", "20", "5") + "]");
        assertThat(gap.statusCode()).isEqualTo(422);
        assertThat(gap.path("code").toString()).isEqualTo("PWDB_INVALID_SEGMENTS");

        // overlap: [0,60) + [50,100) double-defines [50,60)
        ExtractableResponse<Response> overlap = createConfigRaw("pwdb-" + UUID.randomUUID(), "0", "100",
            "[" + segmentJson("0", "60", "10", "2") + "," + segmentJson("50", "100", "20", "5") + "]");
        assertThat(overlap.statusCode()).isEqualTo(422);
        assertThat(overlap.path("code").toString()).isEqualTo("PWDB_INVALID_SEGMENTS");

        // short span: [0,50) + [50,90) never reaches domainEnd=100
        ExtractableResponse<Response> shortSpan = createConfigRaw("pwdb-" + UUID.randomUUID(), "0", "100",
            "[" + segmentJson("0", "50", "10", "2") + "," + segmentJson("50", "90", "20", "5") + "]");
        assertThat(shortSpan.statusCode()).isEqualTo(422);
        assertThat(shortSpan.path("code").toString()).isEqualTo("PWDB_INVALID_SEGMENTS");
    }

    // ── PWDB-EVAL-001 — compare vs the COVERING segment's own target/deadband; signed deviation ──
    @Test @Tag("PWDB-EVAL-001")
    void evaluationComparesAgainstCoveringSegment_perSegmentDeadband_signedDeviation() {
        String key = createTwoTierConfig("pwdb-" + UUID.randomUUID());

        // point 10 is in segment 1 (target=10, width=2): actual=11 → deviation=1, within width=2 → compliant
        ExtractableResponse<Response> ok1 = evaluate(key, "10", "11");
        assertThat(ok1.statusCode()).isEqualTo(201);
        assertThat(new BigDecimal(ok1.jsonPath().getString("deviation"))).isEqualByComparingTo("1");
        assertThat(ok1.jsonPath().getBoolean("compliant")).isTrue();

        // point 10, actual=15 → deviation=5, OUTSIDE width=2 → deviation recorded, signed magnitude exact
        ExtractableResponse<Response> miss1 = evaluate(key, "10", "15");
        assertThat(miss1.statusCode()).isEqualTo(201);
        assertThat(new BigDecimal(miss1.jsonPath().getString("deviation"))).isEqualByComparingTo("5");
        assertThat(miss1.jsonPath().getBoolean("compliant")).isFalse();
        String segment1Id = miss1.jsonPath().getString("segmentId");

        // point 60 is in segment 2 (target=20, width=5): actual=24 → deviation=4, would FAIL segment 1's
        // width=2 but PASSES segment 2's wider width=5 — proves the per-segment tolerance is honored
        ExtractableResponse<Response> ok2 = evaluate(key, "60", "24");
        assertThat(ok2.statusCode()).isEqualTo(201);
        assertThat(new BigDecimal(ok2.jsonPath().getString("deviation"))).isEqualByComparingTo("4");
        assertThat(ok2.jsonPath().getBoolean("compliant")).isTrue();
        assertThat(ok2.jsonPath().getString("segmentId")).isNotEqualTo(segment1Id);

        // a NEGATIVE deviation is recorded signed (actual below target)
        ExtractableResponse<Response> negative = evaluate(key, "60", "12");   // 12 − 20 = −8, |−8| > 5
        assertThat(negative.statusCode()).isEqualTo(201);
        assertThat(new BigDecimal(negative.jsonPath().getString("deviation"))).isEqualByComparingTo("-8");
        assertThat(negative.jsonPath().getBoolean("compliant")).isFalse();

        // a point outside [0,100) is rejected 422
        ExtractableResponse<Response> outOfDomain = evaluate(key, "100", "20");
        assertThat(outOfDomain.statusCode()).isEqualTo(422);
        assertThat(outOfDomain.path("code").toString()).isEqualTo("PWDB_POINT_OUT_OF_DOMAIN");
    }

    // ── PWDB-IMMUTABLE-001 — re-evaluating identical inputs is idempotent; a different actual is a NEW row ──
    @Test @Tag("PWDB-IMMUTABLE-001")
    void reEvaluatingIdenticalInputs_isIdempotent_differentActualIsNewRow() {
        String key = createTwoTierConfig("pwdb-" + UUID.randomUUID());

        ExtractableResponse<Response> first = evaluate(key, "10", "11");
        assertThat(first.statusCode()).isEqualTo(201);
        String firstId = first.jsonPath().getString("id");

        int countAfterFirst = given().header("Authorization", "Bearer " + member)
            .when().get("/api/piecewise-deadband/configs/" + key + "/evaluations")
            .then().statusCode(200).extract().jsonPath().getList("data").size();

        // identical (pointX, actualValue) replayed → 200, SAME id, no new row
        ExtractableResponse<Response> replay = evaluate(key, "10", "11");
        assertThat(replay.statusCode()).isEqualTo(200);
        assertThat(replay.jsonPath().getString("id")).isEqualTo(firstId);

        int countAfterReplay = given().header("Authorization", "Bearer " + member)
            .when().get("/api/piecewise-deadband/configs/" + key + "/evaluations")
            .then().statusCode(200).extract().jsonPath().getList("data").size();
        assertThat(countAfterReplay).as("a replay must NOT create a duplicate row").isEqualTo(countAfterFirst);

        // a DIFFERENT actual at the SAME point is a genuinely new evaluation
        ExtractableResponse<Response> different = evaluate(key, "10", "12");
        assertThat(different.statusCode()).isEqualTo(201);
        assertThat(different.jsonPath().getString("id")).isNotEqualTo(firstId);
    }

    // ── RBAC — unauthenticated create is rejected; unknown config is an IDOR-safe 404 ──
    @Test
    void rbac_unauthenticatedCreateIsRejected_unknownConfigIs404() {
        given().header("Content-Type", "application/json")
            .body("{\"configKey\":\"x\",\"domainStart\":0,\"domainEnd\":1,\"segments\":["
                + segmentJson("0", "1", "0", "0") + "]}")
        .when().post("/api/piecewise-deadband/configs").then().statusCode(401);

        given().header("Authorization", "Bearer " + member)
            .when().get("/api/piecewise-deadband/configs/" + UUID.randomUUID())
            .then().statusCode(404);
    }
}
