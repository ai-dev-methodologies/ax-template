package com.ax.template.authblueprint.withholdingsplit;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * withholding-split-l0 compliance — verified against the live withholdingsplit reference workload.
 * The invariant: a gross payment splits into EXACTLY withholding + net legs summing to gross to the
 * cent, the rate that produced the split is recorded immutably, a per-period remittance run is
 * idempotent, and a correction is a NEW reversing posting, never an edit.
 * Spec: specs/withholding-split-l0.yaml.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("WITHHOLDING_SPLIT")
class WithholdingSplitComplianceTest {

    // Distinct fabricated period per call — remittance collection sums EVERY posting for a period,
    // so sibling test methods sharing "the current month" would pollute each other's totals.
    private static final AtomicInteger PERIOD_SEQ = new AtomicInteger();
    private static String freshPeriod() {
        int n = PERIOD_SEQ.getAndIncrement();
        return String.format("29%02d-%02d", n / 12, (n % 12) + 1);
    }

    @LocalServerPort int port;
    String member;

    @BeforeEach
    void setup() {
        member = WithholdingSplitTestSupport.obtainToken(
            WithholdingSplitTestSupport.freshEmail("wht-member"), "MEMBER");
    }

    private ExtractableResponse<Response> post(String gross, String rate, String period) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"grossAmount\":" + gross + ",\"rate\":" + rate + ",\"period\":\"" + period + "\"}")
        .when().post("/api/withholding-split/postings").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> reverse(String id) {
        return given().header("Authorization", "Bearer " + member)
        .when().post("/api/withholding-split/postings/" + id + "/reverse").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> getPosting(String id) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/withholding-split/postings/" + id).then().statusCode(200).extract();
    }

    private ExtractableResponse<Response> collect(String period) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"period\":\"" + period + "\"}")
        .when().post("/api/withholding-split/remittances").thenReturn().then().extract();
    }

    private BigDecimal legAmount(ExtractableResponse<Response> r, String legType) {
        List<java.util.Map<String, Object>> legs = r.jsonPath().getList("legs");
        return legs.stream().filter(l -> legType.equals(l.get("legType")))
            .map(l -> new BigDecimal(l.get("amount").toString())).findFirst()
            .orElseThrow(() -> new AssertionError("no " + legType + " leg"));
    }

    // ── WHT-SPLIT-001 — exactly 2 legs, sum == gross exactly ──
    @Test @Tag("WHT-SPLIT-001")
    void split_producesExactlyTwoLegs_summingToGrossExactly() {
        ExtractableResponse<Response> posted = post("100", "0.2", freshPeriod());
        assertThat(posted.statusCode()).isEqualTo(201);
        List<?> legs = posted.jsonPath().getList("legs");
        assertThat(legs).hasSize(2);
        assertThat(legAmount(posted, "WITHHOLDING")).isEqualByComparingTo("20.00");
        assertThat(legAmount(posted, "NET")).isEqualByComparingTo("80.00");
        assertThat(legAmount(posted, "WITHHOLDING").add(legAmount(posted, "NET")))
            .as("WHT-SPLIT-001 — legs sum to gross exactly")
            .isEqualByComparingTo("100");
    }

    @Test @Tag("WHT-SPLIT-001")
    void invalidGross_rejected422_andRateOutOfRange_rejected400() {
        String p = freshPeriod();
        assertThat(post("0", "0.2", p).statusCode()).isEqualTo(422);
        assertThat(post("100", "1.0", p).statusCode()).isEqualTo(400);    // @DecimalMax exclusive
        assertThat(post("100", "-0.1", p).statusCode()).isEqualTo(400);   // @DecimalMin
    }

    @Test @Tag("WHT-SPLIT-001")
    void postingNotFound_is404() {
        assertThat(given().header("Authorization", "Bearer " + member)
            .when().get("/api/withholding-split/postings/" + UUID.randomUUID())
            .thenReturn().statusCode()).isEqualTo(404);
    }

    // ── WHT-RATE-002 — rate recorded immutably; odd-cent remainder assigned to net ──
    @Test @Tag("WHT-RATE-002")
    void oddCentRemainder_assignedToNetLeg_rateRecordedOnPosting() {
        ExtractableResponse<Response> posted = post("100.01", "0.333333", freshPeriod());
        assertThat(posted.statusCode()).isEqualTo(201);
        assertThat(new BigDecimal(posted.jsonPath().getString("rate")))
            .as("WHT-RATE-002 — the rate snapshot that produced this split")
            .isEqualByComparingTo("0.333333");
        assertThat(legAmount(posted, "WITHHOLDING")).isEqualByComparingTo("33.34");
        assertThat(legAmount(posted, "NET")).isEqualByComparingTo("66.67");
        assertThat(legAmount(posted, "WITHHOLDING").add(legAmount(posted, "NET")))
            .as("odd-cent case still sums to gross exactly")
            .isEqualByComparingTo("100.01");
    }

    // ── WHT-REMIT-003 — idempotent per-period collection, never double-counted ──
    @Test @Tag("WHT-REMIT-003")
    void remittance_idempotentPerPeriod_rerunNeverDoubleCounts() {
        String period = freshPeriod();
        post("100", "0.1", period);                                    // withholding 10.00
        post("200", "0.1", period);                                    // withholding 20.00

        ExtractableResponse<Response> first = collect(period);
        assertThat(first.statusCode()).isEqualTo(201);
        assertThat(new BigDecimal(first.jsonPath().getString("totalWithheld"))).isEqualByComparingTo("30.00");
        int firstCount = first.jsonPath().getInt("postingCount");
        assertThat(firstCount).isEqualTo(2);

        post("300", "0.1", period);                                    // a THIRD posting after collection

        ExtractableResponse<Response> second = collect(period);
        assertThat(second.statusCode()).isEqualTo(201);
        assertThat(second.jsonPath().getString("totalWithheld"))
            .as("WHT-REMIT-003 — rerun returns the FROZEN first collection, not re-summed")
            .isEqualTo(first.jsonPath().getString("totalWithheld"));
        assertThat(second.jsonPath().getInt("postingCount")).isEqualTo(firstCount);
        assertThat(second.jsonPath().getString("id")).isEqualTo(first.jsonPath().getString("id"));
    }

    /**
     * WHT-REMIT-003 concurrency keystone (P1-64) — two concurrent collects on the SAME period both
     * pass the pre-check before either commits; the loser hits the uq(period) constraint. Both MUST
     * resolve to the SAME frozen run and NEITHER may surface a 500. The racy insert is isolated in a
     * REQUIRES_NEW inner tx so the loser's requery runs on an unpoisoned connection — on PostgreSQL a
     * same-tx requery would fail with 25P02 (500). H2 cannot reproduce 25P02, so this test proves the
     * happy-path contract; the structural REQUIRES_NEW lock in WithholdingSplitViolationProofTest is
     * the real regression guard against reverting the fix.
     */
    @Test @Tag("WHT-REMIT-003")
    void concurrentCollectSamePeriod_singleWinner_neither500() throws Exception {
        String period = freshPeriod();
        post("100", "0.1", period);                                    // one posting so the run has a basis
        int n = 2;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch start = new CountDownLatch(1);
        ConcurrentLinkedQueue<Integer> statusCodes = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<String> ids = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < n; i++) {
            pool.submit(() -> {
                start.await();
                ExtractableResponse<Response> r = collect(period);
                statusCodes.add(r.statusCode());
                if (r.statusCode() == 201) ids.add(r.jsonPath().getString("id"));
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        assertThat(statusCodes).as("neither racer surfaces an unmapped 500").allMatch(code -> code == 201);
        assertThat(ids.stream().distinct()).as("both collects resolve to the SAME frozen run").hasSize(1);
    }

    @Test @Tag("WHT-REMIT-003")
    void remittance_invalidPeriodFormat_rejected422_unknownPeriod_404() {
        assertThat(collect("not-a-period").statusCode()).isEqualTo(422);
        assertThat(given().header("Authorization", "Bearer " + member)
            .when().get("/api/withholding-split/remittances/1999-01").thenReturn().statusCode())
            .isEqualTo(404);
    }

    // ── WHT-IMMUTABLE-004 — a correction is a NEW reversing posting; the original is unchanged ──
    @Test @Tag("WHT-IMMUTABLE-004")
    void reversal_createsNewPosting_originalUntouched() {
        ExtractableResponse<Response> original = post("50", "0.3", freshPeriod());
        assertThat(original.statusCode()).isEqualTo(201);
        String originalId = original.jsonPath().getString("id");

        ExtractableResponse<Response> reversed = reverse(originalId);
        assertThat(reversed.statusCode()).isEqualTo(201);
        assertThat(reversed.jsonPath().getString("id")).isNotEqualTo(originalId);
        assertThat(reversed.jsonPath().getString("correctionOfPostingId")).isEqualTo(originalId);
        assertThat(new BigDecimal(reversed.jsonPath().getString("grossAmount"))).isEqualByComparingTo("-50");
        assertThat(legAmount(reversed, "WITHHOLDING").add(legAmount(reversed, "NET")))
            .isEqualByComparingTo("-50");

        ExtractableResponse<Response> afterReversal = getPosting(originalId);
        assertThat(new BigDecimal(afterReversal.jsonPath().getString("grossAmount")))
            .as("the original posting is byte-for-byte unchanged")
            .isEqualByComparingTo("50");
        assertThat(afterReversal.jsonPath().getString("correctionOfPostingId")).isNull();
    }
}
