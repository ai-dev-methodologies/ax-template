package com.ax.template.authblueprint.thresholdterminal;

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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * threshold-terminal-derivation-l0 compliance — verified against the live thresholdterminal reference
 * workload. The invariant: the accrual that makes the anchor reach/cross the limit drives the
 * IRREVERSIBLE terminal (EXPIRED) in the SAME transaction; EXPIRED has zero outgoing edges; the derived
 * capability (use) is fail-closed on the same locked row; the anchor equals Σ accepted deltas exactly.
 * Spec: specs/threshold-terminal-derivation-l0.yaml (14 CFR §43.10 life-limited parts semantics).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("THRESHOLD_TERMINAL")
class ThresholdTerminalComplianceTest {

    @LocalServerPort int port;
    @org.springframework.beans.factory.annotation.Autowired
    org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    String member;

    @BeforeEach
    void setup() {
        ThresholdTestSupport.useRandomPort(port);
        member = ThresholdTestSupport.obtainToken(ThresholdTestSupport.freshEmail("ttd-member"), "MEMBER");
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────
    private String createRegister(String scope, String limit, String initial) {
        given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"scopeKey\":\"" + scope + "\",\"limit\":" + limit + ",\"initialAnchor\":" + initial + "}")
        .when().post("/api/threshold-registers").then().statusCode(201);
        return scope;
    }

    private ExtractableResponse<Response> accrue(String scope, String delta) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"delta\":" + delta + "}")
        .when().post("/api/threshold-registers/" + scope + "/accruals").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> use(String scope) {
        return given().header("Authorization", "Bearer " + member)
        .when().post("/api/threshold-registers/" + scope + "/use").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> getRegister(String scope) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/threshold-registers/" + scope).then().statusCode(200).extract();
    }

    // ── TTD-CROSS-001 — the crossing accrual drives EXPIRED in the SAME transaction ──
    @Test @Tag("TTD-CROSS-001")
    void crossingAccrual_drivesTerminal_inSameTransaction_withExactOvershoot() {
        String s = createRegister("ttd-" + UUID.randomUUID(), "100", "0");

        ExtractableResponse<Response> below = accrue(s, "60");
        assertThat(below.statusCode()).isEqualTo(200);
        assertThat(below.jsonPath().getString("status")).isEqualTo("ACTIVE");
        assertThat(new BigDecimal(below.jsonPath().getString("anchor"))).isEqualByComparingTo("60");

        // the crossing accrual is ACCEPTED (overshoot recorded exactly) AND returns EXPIRED immediately
        ExtractableResponse<Response> crossing = accrue(s, "45.5");
        assertThat(crossing.statusCode()).isEqualTo(200);
        assertThat(crossing.jsonPath().getString("status"))
            .as("TTD-CROSS-001 — the crossing response itself must expose the terminal status")
            .isEqualTo("EXPIRED");
        assertThat(new BigDecimal(crossing.jsonPath().getString("anchor")))
            .as("overshoot recorded exactly (105.5, not clamped to the limit)")
            .isEqualByComparingTo("105.5");

        // the persisted row agrees — no window in which anchor >= limit while live
        ExtractableResponse<Response> after = getRegister(s);
        assertThat(after.jsonPath().getString("status")).isEqualTo("EXPIRED");
        assertThat(new BigDecimal(after.jsonPath().getString("anchor"))).isEqualByComparingTo("105.5");
    }

    // ── TTD-TERMINAL-001 — terminal is irreversible; late accrual 409 with the anchor unchanged ──
    @Test @Tag("TTD-TERMINAL-001")
    void expiredRegister_rejectsLateAccrual_409_anchorUnchanged() {
        String s = createRegister("ttd-" + UUID.randomUUID(), "50", "0");
        accrue(s, "50");                                              // exact-at-limit crossing

        ExtractableResponse<Response> late = accrue(s, "1");
        assertThat(late.statusCode())
            .as("TTD-TERMINAL-001 — a late accrual on EXPIRED is a deterministic 409")
            .isEqualTo(409);
        assertThat(late.jsonPath().getString("code")).isEqualTo("THRESHOLD_TERMINAL");
        assertThat(late.jsonPath().getString("type")).isEqualTo("urn:problem:threshold-terminal");

        ExtractableResponse<Response> after = getRegister(s);
        assertThat(new BigDecimal(after.jsonPath().getString("anchor")))
            .as("the recorded life status at retirement is immutable evidence")
            .isEqualByComparingTo("50");
        assertThat(after.jsonPath().getString("status")).isEqualTo("EXPIRED");
    }

    // ── TTD-DERIVE-001 — the derived capability is fail-closed once terminal ──
    @Test @Tag("TTD-DERIVE-001")
    void use_succeedsWhileActive_thenFailClosedOnceExpired_andNeverAccrues() {
        String s = createRegister("ttd-" + UUID.randomUUID(), "100", "90");

        ExtractableResponse<Response> okUse = use(s);
        assertThat(okUse.statusCode()).isEqualTo(200);
        assertThat(new BigDecimal(okUse.jsonPath().getString("anchor")))
            .as("using is not accruing — the anchor is untouched")
            .isEqualByComparingTo("90");

        accrue(s, "10");                                              // crossing → EXPIRED

        ExtractableResponse<Response> blockedUse = use(s);
        assertThat(blockedUse.statusCode())
            .as("TTD-DERIVE-001 — the derived capability is rejected once terminal")
            .isEqualTo(409);
        assertThat(blockedUse.jsonPath().getString("code")).isEqualTo("THRESHOLD_TERMINAL");
    }

    // ── TTD-CHECK-001 — the DB @Check actually rejects a live over-limit row (native write,
    //    bypassing the service entirely — proves the backstop reached the ddl-auto schema) ──
    @Test @Tag("TTD-CHECK-001")
    void dbCheck_rejectsNativeWritePastLimit_withoutTerminal() {
        String s = createRegister("ttd-" + UUID.randomUUID(), "100", "10");
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                jdbcTemplate.update(
                    "UPDATE threshold_registers SET anchor_value = 150 WHERE scope_key = ? AND status = 'ACTIVE'", s))
            .as("TTD-CHECK-001 — a code path that forgets the terminal transition must fail at the DB")
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    // ── BACKLOG P2-9 — two @Digits-valid operands whose SUM overflows NUMERIC(19,4) must surface
    //    as a clean 422 NUMERIC_OVERFLOW (common advice, SQLState 22003), never an unmapped 500 ──
    @Test @Tag("TTD-CROSS-001")
    void numericOverflow_onAccrualSum_isMapped422_notUnmapped500() {
        String fifteenNines = "999999999999999";                       // @Digits(integer=15) max
        String s = createRegister("ttd-" + UUID.randomUUID(), fifteenNines, "0");
        accrue(s, "999999999999998");                                  // ACTIVE (just below the limit)

        ExtractableResponse<Response> overflow = accrue(s, fifteenNines);   // sum needs 16 digits
        assertThat(overflow.statusCode())
            .as("P2-9 — a NUMERIC(19,4) overflow at flush must be a mapped 422, not a 500")
            .isEqualTo(422);
        assertThat(overflow.jsonPath().getString("code")).isEqualTo("VALUE_OUT_OF_RANGE");

        // the failed accrual rolled back — the register is untouched and still usable.
        // (RestAssured's default JsonPath parses big JSON numbers as Double — 15+ digits lose
        // precision — so this assertion needs the BIG_DECIMAL number return type.)
        ExtractableResponse<Response> after = getRegister(s);
        io.restassured.path.json.JsonPath exact = after.body().jsonPath(
            new io.restassured.path.json.config.JsonPathConfig(
                io.restassured.path.json.config.JsonPathConfig.NumberReturnType.BIG_DECIMAL));
        assertThat(new BigDecimal(exact.getString("anchor")))
            .isEqualByComparingTo("999999999999998");
        assertThat(exact.getString("status")).isEqualTo("ACTIVE");
    }

    // ── TTD-CHECK-001 — registration guards: limit > 0, anchor in [0, limit) ──
    @Test @Tag("TTD-CHECK-001")
    void registration_rejectsNonPositiveLimit_andAnchorAtOrOverLimit() {
        // a register cannot be born at/over its limit (it would be a live over-limit row)
        ExtractableResponse<Response> bornDead = given()
            .header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"scopeKey\":\"ttd-" + UUID.randomUUID() + "\",\"limit\":10,\"initialAnchor\":10}")
        .when().post("/api/threshold-registers").thenReturn().then().extract();
        assertThat(bornDead.statusCode()).isEqualTo(422);
        assertThat(bornDead.jsonPath().getString("code")).isEqualTo("THRESHOLD_INVALID_VALUE");

        // bean validation bounds: a non-positive limit is rejected at the boundary (400)
        ExtractableResponse<Response> zeroLimit = given()
            .header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"scopeKey\":\"ttd-" + UUID.randomUUID() + "\",\"limit\":0,\"initialAnchor\":0}")
        .when().post("/api/threshold-registers").thenReturn().then().extract();
        assertThat(zeroLimit.statusCode()).isEqualTo(400);

        // a non-positive accrual delta is rejected at the boundary (400)
        String s = createRegister("ttd-" + UUID.randomUUID(), "100", "0");
        ExtractableResponse<Response> zeroDelta = accrue(s, "0");
        assertThat(zeroDelta.statusCode()).isEqualTo(400);
    }

    // ── TTD-CONCURRENT-001 — keystone: exactly one crossing; anchor == Σ accepted deltas ──
    @Test @Tag("TTD-CONCURRENT-001")
    void concurrentAccruals_exactlyOneCrossing_anchorEqualsSumOfAcceptedDeltas() throws Exception {
        String s = createRegister("ttd-" + UUID.randomUUID(), "100", "0");
        int n = 8;
        BigDecimal delta = new BigDecimal("30");                       // 8 × 30 = 240 ≫ 100 → mid-run crossing

        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch start = new CountDownLatch(1);
        ConcurrentLinkedQueue<ExtractableResponse<Response>> results = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < n; i++) {
            pool.submit(() -> {
                start.await();
                results.add(accrue(s, delta.toPlainString()));
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        long accepted = results.stream().filter(r -> r.statusCode() == 200).count();
        long rejected = results.stream().filter(r -> r.statusCode() == 409).count();
        long crossings = results.stream()
            .filter(r -> r.statusCode() == 200 && "EXPIRED".equals(r.jsonPath().getString("status")))
            .count();

        assertThat(accepted + rejected).as("every accrual either accepted or 409").isEqualTo(n);
        assertThat(crossings).as("TTD-CONCURRENT-001 — exactly ONE accrual is the crossing").isEqualTo(1);
        // every rejection is the deterministic terminal problem; every pre-crossing acceptance is
        // genuinely below the limit (no spurious EXPIRED, no anchor leak from a rejected accrual)
        results.stream().filter(r -> r.statusCode() == 409).forEach(r ->
            assertThat(r.jsonPath().getString("code")).isEqualTo("THRESHOLD_TERMINAL"));
        results.stream()
            .filter(r -> r.statusCode() == 200 && "ACTIVE".equals(r.jsonPath().getString("status")))
            .forEach(r -> assertThat(new BigDecimal(r.jsonPath().getString("anchor")))
                .as("a 200-ACTIVE accrual must still be below the limit")
                .isLessThan(new BigDecimal("100")));

        // conservation: the final anchor equals Σ accepted deltas exactly — none lost, none leaked
        ExtractableResponse<Response> after = getRegister(s);
        assertThat(new BigDecimal(after.jsonPath().getString("anchor")))
            .isEqualByComparingTo(delta.multiply(BigDecimal.valueOf(accepted)));
        assertThat(after.jsonPath().getString("status")).isEqualTo("EXPIRED");

        // and a post-crossing use is fail-closed
        assertThat(use(s).statusCode()).isEqualTo(409);
    }
}
