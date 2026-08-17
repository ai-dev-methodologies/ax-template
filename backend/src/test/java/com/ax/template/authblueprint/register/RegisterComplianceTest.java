package com.ax.template.authblueprint.register;

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

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * monotone-register-l0 compliance — verified against the live register reference workload. The
 * invariant: a cumulative register only increases; consumption is delta = curr − prior under the row
 * lock; a NORMAL decrease is a 422; a wrap is a governed ROLLOVER (wrapped delta); a swap is a governed
 * EXCHANGE (downward baseline reset, delta 0). Σ deltas (totalConsumption) is the billed quantity and is
 * robust across rollover/exchange; the identity Σ deltas == final − initial holds ONLY across a
 * NORMAL-only run. Spec: specs/monotone-register-l0.yaml (RFC 2578 Counter semantics).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("REGISTER")
class RegisterComplianceTest {

    @LocalServerPort int port;
    String member;

    @BeforeEach
    void setup() {
        member = RegisterTestSupport.obtainToken(RegisterTestSupport.freshEmail("reg-member"), "MEMBER");
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────
    private String createRegister(String scope, String modulus, String initial) {
        given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"scopeKey\":\"" + scope + "\",\"modulus\":" + modulus + ",\"initialAnchor\":" + initial + "}")
        .when().post("/api/registers").then().statusCode(201);
        return scope;
    }

    private ExtractableResponse<Response> append(String scope, String kind, String value, String reasonField) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"kind\":\"" + kind + "\",\"readingValue\":" + value + reasonField + "}")
        .when().post("/api/registers/" + scope + "/readings").thenReturn().then().extract();
    }

    private void assertRegister(String scope, String anchor, String total) {
        ExtractableResponse<Response> r = given().header("Authorization", "Bearer " + member)
            .when().get("/api/registers/" + scope).then().statusCode(200).extract();
        assertThat(new BigDecimal(r.jsonPath().getString("anchor"))).as("anchor").isEqualByComparingTo(anchor);
        assertThat(new BigDecimal(r.jsonPath().getString("totalConsumption"))).as("total").isEqualByComparingTo(total);
    }

    // ── REG-MONOTONE-001 / REG-DELTA-001 — monotone reads with delta = curr − prior; decrease 422 ──
    @Test @Tag("REG-MONOTONE-001") @Tag("REG-DELTA-001")
    void normalReads_areMonotone_withCorrectDelta_decreaseRejected() {
        String s = createRegister("mtr-" + UUID.randomUUID(), "100000", "100");
        ExtractableResponse<Response> a = append(s, "NORMAL", "150", "");
        assertThat(a.statusCode()).isEqualTo(201);
        assertThat(new BigDecimal(a.jsonPath().getString("delta"))).isEqualByComparingTo("50");
        append(s, "NORMAL", "175", "");        // +25
        assertRegister(s, "175", "75");        // anchor 175, Σ delta 50+25

        // a NORMAL read below the anchor is rejected (no negative delta); anchor unchanged
        ExtractableResponse<Response> down = append(s, "NORMAL", "120", "");
        assertThat(down.statusCode()).isEqualTo(422);
        assertThat(down.path("code").toString()).isEqualTo("REGISTER_NOT_MONOTONE");
        assertRegister(s, "175", "75");

        // history is append-only and reconstructs every interval; Σ delta == final − initial
        ExtractableResponse<Response> hist = given().header("Authorization", "Bearer " + member)
            .when().get("/api/registers/" + s + "/readings?size=50").then().statusCode(200).extract();
        assertThat(hist.jsonPath().getInt("data.size()")).isEqualTo(2);
        assertThat(hist.jsonPath().getList("data.sequenceNo")).containsExactly(1, 2);
    }

    // ── REG-ROLLOVER-001 — an odometer wrap is a governed read with wrapped-delta, never a negative ──
    @Test @Tag("REG-ROLLOVER-001")
    void rollover_recordsWrappedDelta_reasonRequired() {
        String s = createRegister("mtr-" + UUID.randomUUID(), "100000", "99998");
        append(s, "NORMAL", "99999", "");                       // +1, anchor 99999
        assertRegister(s, "99999", "1");

        // wrap: 99999 → 2, delta = (100000 − 99999) + 2 = 3
        ExtractableResponse<Response> roll = append(s, "ROLLOVER", "2", ",\"reason\":\"odometer-wrap\"");
        assertThat(roll.statusCode()).isEqualTo(201);
        assertThat(new BigDecimal(roll.jsonPath().getString("delta"))).isEqualByComparingTo("3");
        assertRegister(s, "2", "4");                            // anchor 2, Σ delta 1+3

        // ROLLOVER without a reason → 422
        assertThat(append(s, "ROLLOVER", "1", "").statusCode()).isEqualTo(422);
        // ROLLOVER that is NOT a wrap (read >= anchor) → 422 invalid
        assertThat(append(s, "ROLLOVER", "5", ",\"reason\":\"x\"").statusCode()).isEqualTo(422);
        assertRegister(s, "2", "4");
    }

    // ── REG-EXCHANGE-001 — a device swap resets the baseline with delta 0; reason required ──
    @Test @Tag("REG-EXCHANGE-001")
    void exchange_resetsBaseline_zeroSeamConsumption_reasonRequired() {
        String s = createRegister("mtr-" + UUID.randomUUID(), "100000", "5000");
        append(s, "NORMAL", "5200", "");                        // +200
        // swap: new meter opens at 7 (BELOW the old anchor) — allowed only via EXCHANGE, delta 0
        ExtractableResponse<Response> ex = append(s, "EXCHANGE", "7", ",\"reason\":\"meter-replaced\"");
        assertThat(ex.statusCode()).isEqualTo(201);
        assertThat(new BigDecimal(ex.jsonPath().getString("delta"))).isEqualByComparingTo("0");
        assertRegister(s, "7", "200");                          // anchor reset to 7; total unchanged (seam adds nothing)

        // subsequent NORMAL read on the new meter resumes monotone delta
        append(s, "NORMAL", "30", "");                          // +23
        assertRegister(s, "30", "223");

        // conservation truth: totalConsumption (Σ delta) is the billed quantity (223) and DIVERGES from
        // (final − initial) = 30 − 5000 = −4970 across the exchange — never reconcile on final − initial
        ExtractableResponse<Response> reg = given().header("Authorization", "Bearer " + member)
            .when().get("/api/registers/" + s).then().statusCode(200).extract();
        BigDecimal total = new BigDecimal(reg.jsonPath().getString("totalConsumption"));
        BigDecimal finalMinusInitial = new BigDecimal(reg.jsonPath().getString("anchor")).subtract(new BigDecimal("5000"));
        assertThat(total).isEqualByComparingTo("223");
        assertThat(total).as("Σ delta MUST NOT equal final − initial once a reset occurred")
            .isNotEqualByComparingTo(finalMinusInitial);

        // an UPWARD exchange (read ≥ anchor) is rejected — it would silently erase consumption (delta 0)
        ExtractableResponse<Response> up = append(s, "EXCHANGE", "900", ",\"reason\":\"bogus-upward-swap\"");
        assertThat(up.statusCode()).isEqualTo(422);
        assertThat(up.path("code").toString()).isEqualTo("REGISTER_INVALID_READING");
        assertRegister(s, "30", "223");                         // unchanged — no free anchor advance

        // EXCHANGE without a reason → 422
        assertThat(append(s, "EXCHANGE", "5", "").statusCode()).isEqualTo(422);
    }

    // ── REG-CONCURRENT-001 (keystone) — concurrent appends serialize; Σ deltas == final − initial ──
    @Test @Tag("REG-CONCURRENT-001")
    void concurrentAppends_serialize_deltaNeverVsStaleAnchor() throws InterruptedException {
        String s = createRegister("mtr-" + UUID.randomUUID(), "1000000", "0");
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        ConcurrentLinkedQueue<Integer> codes = new ConcurrentLinkedQueue<>();
        try {
            for (int i = 1; i <= threads; i++) {
                String value = String.valueOf(i * 100);   // distinct increasing reads: 100,200,...,800
                pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    codes.add(append(s, "NORMAL", value, "").statusCode());  // some 201, some 422 (raced below anchor)
                    return null;
                });
            }
            ready.await();
            go.countDown();
            pool.shutdown();
            assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }
        assertThat(codes.stream().anyMatch(c -> c == 201)).as("at least one append commits").isTrue();

        // CONSERVATION: Σ(committed deltas) == final anchor − initial(0); reads strictly monotone by sequence.
        ExtractableResponse<Response> reg = given().header("Authorization", "Bearer " + member)
            .when().get("/api/registers/" + s).then().statusCode(200).extract();
        BigDecimal finalAnchor = new BigDecimal(reg.jsonPath().getString("anchor"));
        BigDecimal total = new BigDecimal(reg.jsonPath().getString("totalConsumption"));
        assertThat(total).as("Σ deltas == final anchor − 0 (no delta vs a stale anchor)").isEqualByComparingTo(finalAnchor);

        ExtractableResponse<Response> hist = given().header("Authorization", "Bearer " + member)
            .when().get("/api/registers/" + s + "/readings?size=50").then().statusCode(200).extract();
        List<String> values = hist.jsonPath().getList("data.readingValue").stream().map(String::valueOf).toList();
        BigDecimal prev = BigDecimal.valueOf(-1);
        for (String v : values) {
            BigDecimal cur = new BigDecimal(v);
            assertThat(cur).as("committed reads are strictly monotone by sequence").isGreaterThan(prev);
            prev = cur;
        }
    }

    // ── RBAC — unauthenticated append is rejected ──
    @Test
    void rbac_unauthenticatedAppendIsRejected() {
        given().header("Content-Type", "application/json")
            .body("{\"kind\":\"NORMAL\",\"readingValue\":1}")
        .when().post("/api/registers/x/readings").then().statusCode(401);

        given().header("Authorization", "Bearer " + member)
            .when().get("/api/registers/" + UUID.randomUUID())
            .then().statusCode(404).body("type", equalTo("urn:problem:not-found"));
    }
}
