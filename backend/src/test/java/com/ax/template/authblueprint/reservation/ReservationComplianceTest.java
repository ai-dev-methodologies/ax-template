package com.ax.template.authblueprint.reservation;

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
import static org.hamcrest.Matchers.equalTo;

/**
 * reserve-settle-balance-l0 compliance — verified against the live reservation reference workload.
 * The invariant: a pooled balance is drawn in TWO phases — an over-reserve-safe reserve places a hold,
 * a settle commits actual (≤ the hold) AND returns the unused remainder in one tx, and value is
 * conserved (funded == committed + reserved + available) across reserve/settle/release/sweep, even
 * under concurrency. Spec: specs/reserve-settle-balance-l0.yaml (RFC 4006 Credit-Control).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("RESERVATION")
class ReservationComplianceTest {

    @LocalServerPort int port;
    @Autowired ReservationSweeper sweeper;
    String member;

    @BeforeEach
    void setup() {
        member = ReservationTestSupport.obtainToken(ReservationTestSupport.freshEmail("rsv-member"), "MEMBER");
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────
    private String createBalance(String scope, String funded) {
        given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"scopeKey\":\"" + scope + "\",\"funded\":" + funded + "}")
        .when().post("/api/balances").then().statusCode(201);
        return scope;
    }

    private ExtractableResponse<Response> reserve(String scope, String amount, long ttl) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"scopeKey\":\"" + scope + "\",\"amount\":" + amount + ",\"ttlSeconds\":" + ttl + "}")
        .when().post("/api/reservations").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> settle(String holdId, String actual) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"actual\":" + actual + "}")
        .when().post("/api/reservations/" + holdId + "/settle").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> release(String holdId) {
        return given().header("Authorization", "Bearer " + member)
        .when().post("/api/reservations/" + holdId + "/release").thenReturn().then().extract();
    }

    private void assertBalance(String scope, String committed, String reserved, String available) {
        ExtractableResponse<Response> r = given().header("Authorization", "Bearer " + member)
            .when().get("/api/balances/" + scope).then().statusCode(200).extract();
        assertThat(new BigDecimal(r.jsonPath().getString("committed"))).as("committed").isEqualByComparingTo(committed);
        assertThat(new BigDecimal(r.jsonPath().getString("reserved"))).as("reserved").isEqualByComparingTo(reserved);
        assertThat(new BigDecimal(r.jsonPath().getString("available"))).as("available").isEqualByComparingTo(available);
    }

    // ── RSV-RESERVE-001 — over-reserve-safe atomic hold; the rejecting dual ──
    @Test @Tag("RSV-RESERVE-001")
    void reserve_withinAvailable_holds_overReserveRejected() {
        String s = createBalance("acct-" + UUID.randomUUID(), "100");
        reserve(s, "30", 3600).statusCode();
        assertBalance(s, "0", "30", "70");

        // over-available (80 > 70) → reject, never clamp; balance unchanged
        ExtractableResponse<Response> over = reserve(s, "80", 3600);
        assertThat(over.statusCode()).isEqualTo(409);
        assertThat(over.path("code").toString()).isEqualTo("RESERVATION_INSUFFICIENT_FUNDS");
        assertBalance(s, "0", "30", "70");

        // exactly-available is allowed; then nothing more
        assertThat(reserve(s, "70", 3600).statusCode()).isEqualTo(201);
        assertBalance(s, "0", "100", "0");
        assertThat(reserve(s, "1", 3600).statusCode()).isEqualTo(409);
    }

    // ── RSV-RESERVE-001 — an over-large ttlSeconds is a clean 400 (bounded at the validation boundary),
    //    never an Instant.plusSeconds overflow that escapes as an unmapped 500/403 ──
    @Test @Tag("RSV-RESERVE-001")
    void reserve_overLargeTtl_isCleanValidationError_notOverflow() {
        String s = createBalance("acct-" + UUID.randomUUID(), "100");
        int code = given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"scopeKey\":\"" + s + "\",\"amount\":10,\"ttlSeconds\":9223372036854775807}")
        .when().post("/api/reservations").thenReturn().statusCode();
        assertThat(code).as("Long.MAX_VALUE ttlSeconds → bounded 400, never a 500/403 overflow").isEqualTo(400);
        assertBalance(s, "0", "0", "100");   // nothing reserved on a rejected request
    }

    // ── RSV-SETTLE-001 — settle commits actual ≤ hold AND refunds remainder ──
    @Test @Tag("RSV-SETTLE-001")
    void settle_commitsActual_refundsRemainder_overSettle422_doubleSettle409() {
        String s = createBalance("acct-" + UUID.randomUUID(), "100");
        String holdA = reserve(s, "50", 3600).path("id");

        ExtractableResponse<Response> settled = settle(holdA, "30");
        assertThat(settled.statusCode()).isEqualTo(200);
        assertThat(settled.path("status").toString()).isEqualTo("SETTLED");
        assertThat(new BigDecimal(settled.jsonPath().getString("settledAmount"))).isEqualByComparingTo("30");
        // committed 30, reserved 0, available 70 (the 20 remainder returned)
        assertBalance(s, "30", "0", "70");

        // over-settle: a 60 actual against a 50 hold → 422, balance unchanged
        String holdB = reserve(s, "50", 3600).path("id");
        ExtractableResponse<Response> over = settle(holdB, "60");
        assertThat(over.statusCode()).isEqualTo(422);
        assertThat(over.path("code").toString()).isEqualTo("RESERVATION_OVER_SETTLE");
        assertBalance(s, "30", "50", "20");

        // double-settle a terminal hold → 409, no double-commit
        assertThat(settle(holdA, "10").statusCode()).isEqualTo(409);
        assertBalance(s, "30", "50", "20");
    }

    // ── RSV-RELEASE-001 — release returns the whole hold; one terminal transition ──
    @Test @Tag("RSV-RELEASE-001")
    void release_returnsWholeHold_settleThenRelease409() {
        String s = createBalance("acct-" + UUID.randomUUID(), "100");
        String holdA = reserve(s, "40", 3600).path("id");
        assertThat(release(holdA).statusCode()).isEqualTo(200);
        assertBalance(s, "0", "0", "100");   // whole hold returned, committed untouched

        String holdB = reserve(s, "40", 3600).path("id");
        assertThat(settle(holdB, "25").statusCode()).isEqualTo(200);
        // a SETTLED hold cannot be released (one terminal transition)
        ExtractableResponse<Response> rel = release(holdB);
        assertThat(rel.statusCode()).isEqualTo(409);
        assertThat(rel.path("code").toString()).isEqualTo("RESERVATION_NOT_OUTSTANDING");
        assertBalance(s, "25", "0", "75");   // no second balance movement
    }

    // ── RSV-SWEEP-001 — timeout sweep reclaims a stranded hold and loses the race to a live settle ──
    @Test @Tag("RSV-SWEEP-001")
    void sweep_reclaimsExpiredHold_butLosesRaceToLiveSettle() throws InterruptedException {
        String s = createBalance("acct-" + UUID.randomUUID(), "100");

        // (a) an abandoned (expired) hold is reclaimed: full amount returns to available
        String stranded = reserve(s, "40", 1).path("id");
        assertBalance(s, "0", "40", "60");
        Thread.sleep(1200);                  // cross the 1s TTL
        sweeper.sweepOnce();
        assertThat(given().header("Authorization", "Bearer " + member)
            .when().get("/api/reservations/" + stranded).then().statusCode(200)
            .extract().path("status").toString()).isEqualTo("EXPIRED");
        assertBalance(s, "0", "0", "100");

        // (b) a settled hold is NOT double-returned by a later sweep (sweep loses to the live settle)
        String settledHold = reserve(s, "40", 1).path("id");
        assertThat(settle(settledHold, "30").statusCode()).isEqualTo(200);
        assertBalance(s, "30", "0", "70");
        Thread.sleep(1200);                  // the hold is now past its TTL but already SETTLED
        sweeper.sweepOnce();                 // must skip it — no double-return
        assertBalance(s, "30", "0", "70");
        assertThat(given().header("Authorization", "Bearer " + member)
            .when().get("/api/reservations/" + settledHold).then().statusCode(200)
            .extract().path("status").toString()).isEqualTo("SETTLED");

        // (c) reconciliation truth: reserved == Σ amount of OUTSTANDING holds
        reserve(s, "25", 3600);
        ExtractableResponse<Response> holds = given().header("Authorization", "Bearer " + member)
            .when().get("/api/balances/" + s + "/reservations?size=50").then().statusCode(200).extract();
        BigDecimal sumOutstanding = BigDecimal.ZERO;
        int n = holds.jsonPath().getInt("data.size()");
        for (int i = 0; i < n; i++) {
            if ("OUTSTANDING".equals(holds.jsonPath().getString("data[" + i + "].status"))) {
                sumOutstanding = sumOutstanding.add(new BigDecimal(holds.jsonPath().getString("data[" + i + "].amount")));
            }
        }
        assertThat(sumOutstanding).as("reserved == Σ OUTSTANDING holds").isEqualByComparingTo("25");
        assertBalance(s, "30", "25", "45");
    }

    // ── RSV-CONSERVE-001 (keystone) — N concurrent reserves never over-reserve; settle conserves ──
    @Test @Tag("RSV-CONSERVE-001")
    void concurrentReserves_neverOverReserve_thenSettle_conserves() throws InterruptedException {
        String s = createBalance("acct-" + UUID.randomUUID(), "100");
        int threads = 8;
        BigDecimal each = new BigDecimal("20");          // 8×20 = 160 demanded against 100 funded → only 5 fit

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        ConcurrentLinkedQueue<Integer> codes = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<String> grantedHoldIds = new ConcurrentLinkedQueue<>();
        try {
            for (int i = 0; i < threads; i++) {
                pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    ExtractableResponse<Response> r = reserve(s, "20", 3600);
                    codes.add(r.statusCode());
                    if (r.statusCode() == 201) {
                        grantedHoldIds.add(r.path("id"));
                    }
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

        long granted = codes.stream().filter(c -> c == 201).count();
        long rejected = codes.stream().filter(c -> c == 409).count();
        assertThat(granted).as("exactly floor(100/20)=5 reserves fit — never over-reserved").isEqualTo(5);
        assertThat(rejected).as("the other 3 are deterministically refused").isEqualTo(3);
        assertBalance(s, "0", "100", "0");               // Σ granted holds == funded, available exhausted

        // settle each granted hold with actual 12 (< 20) → committed 60, remainder 40 returned
        for (String holdId : grantedHoldIds) {
            assertThat(settle(holdId, "12").statusCode()).isEqualTo(200);
        }
        // conservation keystone: funded(100) == committed(60) + reserved(0) + available(40); committed == Σ actual
        assertBalance(s, "60", "0", "40");
    }

    // ── RBAC — unauthenticated reserve is rejected ──
    @Test
    void rbac_unauthenticatedReserveIsRejected() {
        given().header("Content-Type", "application/json")
            .body("{\"scopeKey\":\"x\",\"amount\":1,\"ttlSeconds\":60}")
        .when().post("/api/reservations").then().statusCode(401);

        given().header("Authorization", "Bearer " + member)
            .when().get("/api/balances/" + UUID.randomUUID())
            .then().statusCode(404).body("type", equalTo("urn:problem:not-found"));
    }
}
