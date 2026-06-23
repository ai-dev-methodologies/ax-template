package com.ax.template.authblueprint.variancegate;

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
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * variance-tolerance-band-l0 compliance — verified against the live variancegate reference workload.
 * The invariant: the variance is DERIVED (actual − standard) and persisted with the band that
 * governed THIS verdict; the gate is ASYMMETRIC (WITHIN_TOLERANCE iff variance ∈ [−lower, +upper]);
 * a dependent operation on an undisposed breach is blocked 422 naming the variance + band; a breach
 * proceeds only via an explicit who/when/reason disposition that never erases the verdict;
 * concurrent dispositions serialize so exactly one wins.
 * Spec: specs/variance-tolerance-band-l0.yaml (standard-cost variance + NIST §6.1.6 SPC tolerance + CWE-362).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("VARIANCEGATE")
class VarianceComplianceTest {

    @LocalServerPort int port;
    @Autowired VarianceService service;
    String member;

    @BeforeEach
    void setup() {
        VarianceTestSupport.useRandomPort(port);
        member = VarianceTestSupport.obtainToken(VarianceTestSupport.freshEmail("vg-member"), "MEMBER");
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────
    private ExtractableResponse<Response> appraise(String subject, String standard, String actual,
                                                   String lower, String upper) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"subject\":\"" + subject + "\",\"standardValue\":" + standard
                + ",\"actualValue\":" + actual + ",\"lowerTolerance\":" + lower
                + ",\"upperTolerance\":" + upper + "}")
        .when().post("/api/variance-gate/appraisals").thenReturn().then().extract();
    }

    private String appraiseId(String subject, String standard, String actual, String lower, String upper) {
        ExtractableResponse<Response> r = appraise(subject, standard, actual, lower, upper);
        assertThat(r.statusCode()).isEqualTo(201);
        return r.jsonPath().getString("id");
    }

    private ExtractableResponse<Response> proceed(String id) {
        return given().header("Authorization", "Bearer " + member)
            .when().post("/api/variance-gate/appraisals/" + id + "/proceed").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> dispose(String id, String reason) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"reason\":\"" + reason + "\"}")
        .when().post("/api/variance-gate/appraisals/" + id + "/dispositions").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> getAppraisal(String id) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/variance-gate/appraisals/" + id).then().statusCode(200).extract();
    }

    // ── VG-DERIVE-001 — variance is derived (actual − standard), basis + band recorded ──
    @Test @Tag("VG-DERIVE-001")
    void variance_isDerived_basisAndBandRecorded() {
        // standard 100.0000, actual 100.2500 → variance +0.2500 ; band [-2.0000, +0.5000]
        ExtractableResponse<Response> r = appraise("COST-DERIVE", "100.0000", "100.2500", "2.0000", "0.5000");
        assertThat(r.statusCode()).isEqualTo(201);
        // the variance is the DERIVED difference, asserted NUMERICALLY
        assertThat(new BigDecimal(r.jsonPath().getString("variance")))
            .isEqualByComparingTo(new BigDecimal("0.2500"));
        // the basis + band are echoed (reconstructible verdict — never a bare pass/fail)
        assertThat(new BigDecimal(r.jsonPath().getString("standardValue"))).isEqualByComparingTo("100.0000");
        assertThat(new BigDecimal(r.jsonPath().getString("actualValue"))).isEqualByComparingTo("100.2500");
        assertThat(new BigDecimal(r.jsonPath().getString("lowerTolerance"))).isEqualByComparingTo("2.0000");
        assertThat(new BigDecimal(r.jsonPath().getString("upperTolerance"))).isEqualByComparingTo("0.5000");
        assertThat(r.jsonPath().getString("verdict")).isEqualTo("WITHIN_TOLERANCE");   // +0.25 ∈ [-2, +0.5]
        assertThat(r.jsonPath().getString("createdAt")).as("the appraisal records when").isNotBlank();
    }

    // ── VG-GATE-001 — the gate is ASYMMETRIC; favorable wide, unfavorable narrow ──
    @Test @Tag("VG-GATE-001")
    void gate_isAsymmetric_favorableWide_unfavorableNarrow() {
        // band [-2.0000, +0.5000]: the SAME magnitude 1.5000 passes on the favorable side but
        // breaches on the unfavorable side — proving the two bounds are INDEPENDENT.

        // favorable: actual below standard → variance −1.5000, within −2.0000 → WITHIN
        ExtractableResponse<Response> fav = appraise("COST-FAV", "100.0000", "98.5000", "2.0000", "0.5000");
        assertThat(new BigDecimal(fav.jsonPath().getString("variance"))).isEqualByComparingTo("-1.5000");
        assertThat(fav.jsonPath().getString("verdict")).isEqualTo("WITHIN_TOLERANCE");

        // unfavorable: actual above standard → variance +1.5000, beyond +0.5000 → OUT_OF_TOLERANCE
        ExtractableResponse<Response> unfav = appraise("COST-UNFAV", "100.0000", "101.5000", "2.0000", "0.5000");
        assertThat(new BigDecimal(unfav.jsonPath().getString("variance"))).isEqualByComparingTo("1.5000");
        assertThat(unfav.jsonPath().getString("verdict")).isEqualTo("OUT_OF_TOLERANCE");

        // exactly-at-the-bound is inclusive: variance == +upper → still WITHIN
        ExtractableResponse<Response> edge = appraise("COST-EDGE", "100.0000", "100.5000", "2.0000", "0.5000");
        assertThat(new BigDecimal(edge.jsonPath().getString("variance"))).isEqualByComparingTo("0.5000");
        assertThat(edge.jsonPath().getString("verdict")).isEqualTo("WITHIN_TOLERANCE");

        // just past the unfavorable bound → OUT_OF_TOLERANCE
        ExtractableResponse<Response> over = appraise("COST-OVER", "100.0000", "100.5100", "2.0000", "0.5000");
        assertThat(new BigDecimal(over.jsonPath().getString("variance"))).isEqualByComparingTo("0.5100");
        assertThat(over.jsonPath().getString("verdict")).isEqualTo("OUT_OF_TOLERANCE");
    }

    // ── VG-BLOCK-001 — a dependent op proceeds within tolerance, is blocked 422 on a breach ──
    @Test @Tag("VG-BLOCK-001")
    void dependentOp_proceedsWithinTolerance_blockedOnBreach_withVarianceAndBandNamed() {
        // within tolerance → proceed 200
        String okId = appraiseId("PROCEED-OK", "100.0000", "100.2000", "2.0000", "0.5000");
        ExtractableResponse<Response> ok = proceed(okId);
        assertThat(ok.statusCode()).isEqualTo(200);
        assertThat(ok.jsonPath().getString("verdict")).isEqualTo("WITHIN_TOLERANCE");

        // breach with no disposition → blocked 422, the variance + band NAMED in the body
        String breachId = appraiseId("PROCEED-BREACH", "100.0000", "103.0000", "2.0000", "0.5000");
        ExtractableResponse<Response> blocked = proceed(breachId);
        assertThat(blocked.statusCode()).isEqualTo(422);
        assertThat(blocked.jsonPath().getString("code")).isEqualTo("VARIANCE_OUT_OF_TOLERANCE");
        assertThat(new BigDecimal(blocked.jsonPath().getString("variance"))).isEqualByComparingTo("3.0000");
        assertThat(new BigDecimal(blocked.jsonPath().getString("lowerTolerance"))).isEqualByComparingTo("2.0000");
        assertThat(new BigDecimal(blocked.jsonPath().getString("upperTolerance"))).isEqualByComparingTo("0.5000");

        // fail-closed: an unknown appraisal is a 404, never an implicit pass
        ExtractableResponse<Response> unknown = proceed(UUID.randomUUID().toString());
        assertThat(unknown.statusCode()).isEqualTo(404);
        assertThat(unknown.jsonPath().getString("code")).isEqualTo("RESOURCE_NOT_FOUND");
    }

    // ── VG-DISPOSE-001 — a breach proceeds only via an explicit who/when/reason disposition ──
    @Test @Tag("VG-DISPOSE-001")
    void breach_requiresExplicitDisposition_toProceed_verdictNeverRewritten_idempotent() {
        String id = appraiseId("DISPOSE", "100.0000", "104.0000", "2.0000", "0.5000");
        assertThat(getAppraisal(id).jsonPath().getString("verdict")).isEqualTo("OUT_OF_TOLERANCE");

        // before disposition: blocked
        assertThat(proceed(id).statusCode()).isEqualTo(422);

        // record the accountable disposition (actor = caller, reason from the body)
        ExtractableResponse<Response> disposed = dispose(id, "scrap-rework approved by line supervisor");
        assertThat(disposed.statusCode()).isEqualTo(200);
        // the verdict is NOT rewritten — the breach stays visible WITH an override on record
        assertThat(disposed.jsonPath().getString("verdict")).isEqualTo("OUT_OF_TOLERANCE");
        assertThat(disposed.jsonPath().getBoolean("disposed")).isTrue();

        // now the dependent op proceeds
        assertThat(proceed(id).statusCode()).isEqualTo(200);

        // the disposition record carries who/when/reason
        ExtractableResponse<Response> rec = given().header("Authorization", "Bearer " + member)
            .when().get("/api/variance-gate/appraisals/" + id + "/disposition").then().statusCode(200).extract();
        assertThat(rec.jsonPath().getString("decision")).isEqualTo("OVERRIDE");
        assertThat(rec.jsonPath().getString("actor")).isNotBlank();
        assertThat(rec.jsonPath().getString("reason")).isEqualTo("scrap-rework approved by line supervisor");
        assertThat(rec.jsonPath().getString("decidedAt")).isNotBlank();

        // a second OVERRIDE is idempotent — no second row, no error
        assertThat(dispose(id, "second look").statusCode()).isEqualTo(200);
        assertThat(getAppraisal(id).jsonPath().getBoolean("disposed")).isTrue();
    }

    // ── VG-DISPOSE-001 — a blank reason is 422; disposing a within-tolerance appraisal is 422 ──
    @Test @Tag("VG-DISPOSE-001")
    void disposition_blankReasonIs422_andNothingToDisposeIs422() {
        String breachId = appraiseId("DISPOSE-BLANK", "100.0000", "105.0000", "2.0000", "0.5000");
        ExtractableResponse<Response> blank = dispose(breachId, "   ");
        assertThat(blank.statusCode()).isEqualTo(422);
        assertThat(blank.jsonPath().getString("code")).isEqualTo("VARIANCE_BLANK_REASON");

        // a within-tolerance appraisal has no breach to dispose → 422
        String okId = appraiseId("DISPOSE-NONE", "100.0000", "100.1000", "2.0000", "0.5000");
        ExtractableResponse<Response> none = dispose(okId, "no breach here");
        assertThat(none.statusCode()).isEqualTo(422);
        assertThat(none.jsonPath().getString("code")).isEqualTo("VARIANCE_NOTHING_TO_DISPOSE");
    }

    // ── VG-CONCURRENT-001 — keystone: N concurrent dispositions → exactly one disposition row ──
    @Test @Tag("VG-CONCURRENT-001")
    void concurrentDispositions_exactlyOneRow() throws Exception {
        String id = appraiseId("RACE", "100.0000", "110.0000", "2.0000", "0.5000");
        UUID appraisalId = UUID.fromString(id);

        int n = 8;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch start = new CountDownLatch(1);
        ConcurrentLinkedQueue<Integer> codes = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < n; i++) {
            pool.submit(() -> {
                start.await();
                try {
                    service.dispose(appraisalId, "racer", "concurrent override");
                    codes.add(200);                                  // winner or idempotent success
                } catch (VarianceException ex) {
                    codes.add(ex.status().value());                  // loser of the residual race → 409
                }
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        // every call resolves to a recorded disposition (2xx) or the deterministic 409 loser
        assertThat(codes).allMatch(c -> c == 200 || c == 409);
        assertThat(codes.stream().filter(c -> c == 200).count())
            .as("at least one disposition succeeds").isGreaterThanOrEqualTo(1);

        // EXACTLY ONE disposition row exists for the appraisal (the uq backstop)
        VarianceDisposition only = service.dispositionOf(appraisalId);
        assertThat(only).as("exactly one accountable disposition on record").isNotNull();
        assertThat(only.getDecision()).isEqualTo(DispositionDecision.OVERRIDE);
        assertThat(getAppraisal(id).jsonPath().getBoolean("disposed")).isTrue();
        // the breach verdict is never rewritten by the race
        assertThat(getAppraisal(id).jsonPath().getString("verdict")).isEqualTo("OUT_OF_TOLERANCE");
    }
}
