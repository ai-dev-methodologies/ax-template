package com.ax.template.authblueprint.tieredeligibility;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * tiered-eligibility-l0 compliance — verified against the live tieredeligibility reference workload. The
 * invariant: a threshold-crossing accrual atomically drives the corresponding tier (possibly skipping
 * several boundaries at once); automatic degradation never improves the tier; an explicit, audited restore
 * is the ONLY path back up; the derived `use` capability is fail-closed at the worst tier under the same
 * row lock. Spec: specs/tiered-eligibility-l0.yaml (composes threshold-terminal-derivation-l0).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("TIERED_ELIGIBILITY")
class TierLadderComplianceTest {

    @LocalServerPort int port;
    String member;

    @BeforeEach
    void setup() {
        member = TierTestSupport.obtainToken(TierTestSupport.freshEmail("tier-member"), "MEMBER");
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────
    /** 4-tier ladder: FULL(0) → WARN(20) → REDUCED(50) → SUSPENDED(100). */
    private String createFourTierLadder(String key) {
        given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"ladderKey\":\"" + key + "\",\"tierNames\":[\"FULL\",\"WARN\",\"REDUCED\",\"SUSPENDED\"],"
                + "\"thresholds\":[20,50,100],\"initialCount\":0}")
        .when().post("/api/tiered-eligibility/ladders").then().statusCode(201);
        return key;
    }

    private ExtractableResponse<Response> accrue(String key, int delta) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"delta\":" + delta + "}")
        .when().post("/api/tiered-eligibility/ladders/" + key + "/accruals").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> use(String key) {
        return given().header("Authorization", "Bearer " + member)
        .when().post("/api/tiered-eligibility/ladders/" + key + "/use").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> restore(String key, int newCount, String reason) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"newCount\":" + newCount + ",\"reason\":\"" + reason + "\"}")
        .when().post("/api/tiered-eligibility/ladders/" + key + "/restore").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> getLadder(String key) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/tiered-eligibility/ladders/" + key).then().statusCode(200).extract();
    }

    // ── TIER-LADDER-001 — a crossing accrual atomically drives the tier; a big delta MAY skip a boundary ──
    @Test @Tag("TIER-LADDER-001")
    void crossingAccrual_drivesTierAtomically_mayCrossMultipleBoundaries() {
        String s = createFourTierLadder("tier-" + UUID.randomUUID());

        // below the first threshold — stays FULL (tier 0)
        ExtractableResponse<Response> below = accrue(s, 15);
        assertThat(below.statusCode()).isEqualTo(200);
        assertThat(below.jsonPath().getInt("currentTierIndex")).isEqualTo(0);
        assertThat(below.jsonPath().getString("currentTierName")).isEqualTo("FULL");

        // 15 + 60 = 75: crosses WARN(20) AND REDUCED(50) in ONE call → lands on REDUCED (tier 2), skipping WARN
        ExtractableResponse<Response> jump = accrue(s, 60);
        assertThat(jump.statusCode()).isEqualTo(200);
        assertThat(jump.jsonPath().getInt("count")).isEqualTo(75);
        assertThat(jump.jsonPath().getInt("currentTierIndex"))
            .as("TIER-LADDER-001 — a single accrual may cross multiple boundaries at once").isEqualTo(2);
        assertThat(jump.jsonPath().getString("currentTierName")).isEqualTo("REDUCED");
    }

    // ── TIER-MONOTONE-001 — automatic degradation never improves; restore is the ONLY audited path up ──
    @Test @Tag("TIER-MONOTONE-001")
    void restoreIsTheOnlyPathUp_auditedSeparatelyFromAccruals_rejectsNonReducingCount() {
        String s = createFourTierLadder("tier-" + UUID.randomUUID());
        accrue(s, 75);   // → REDUCED (tier 2), count 75

        // a restore that does not actually reduce the count is rejected
        assertThat(restore(s, 75, "no-op attempt").statusCode()).isEqualTo(422);
        assertThat(restore(s, 80, "would INCREASE count").statusCode()).isEqualTo(422);

        // a blank reason is rejected at the validation boundary
        assertThat(restore(s, 10, "").statusCode()).isEqualTo(400);

        // a genuine restore: count 75 → 10 moves the tier back to FULL (0), WITH a reason, audited
        ExtractableResponse<Response> ok = restore(s, 10, "compliance review cleared the violation");
        assertThat(ok.statusCode()).isEqualTo(200);
        assertThat(ok.jsonPath().getInt("count")).isEqualTo(10);
        assertThat(ok.jsonPath().getInt("currentTierIndex")).isEqualTo(0);

        // the restore ledger has exactly 1 entry; the accrual ledger is untouched by it (still 1 entry)
        ExtractableResponse<Response> restores = given().header("Authorization", "Bearer " + member)
            .when().get("/api/tiered-eligibility/ladders/" + s + "/restores").then().statusCode(200).extract();
        assertThat(restores.jsonPath().getList("data")).hasSize(1);
        assertThat(restores.jsonPath().getString("data[0].reason")).isEqualTo("compliance review cleared the violation");

        ExtractableResponse<Response> accruals = given().header("Authorization", "Bearer " + member)
            .when().get("/api/tiered-eligibility/ladders/" + s + "/accruals").then().statusCode(200).extract();
        assertThat(accruals.jsonPath().getList("data"))
            .as("TIER-MONOTONE-001 — restore is recorded in a ledger SEPARATE from accruals").hasSize(1);
    }

    // ── TIER-DERIVE-001 — use() is fail-closed ONLY at the worst tier, under the same lock ──
    @Test @Tag("TIER-DERIVE-001")
    void useSucceedsExceptAtWorstTier_neverAccrues() {
        String s = createFourTierLadder("tier-" + UUID.randomUUID());
        accrue(s, 30);   // → WARN (tier 1), not worst

        ExtractableResponse<Response> okUse = use(s);
        assertThat(okUse.statusCode()).isEqualTo(200);
        assertThat(okUse.jsonPath().getInt("count")).as("using is not accruing").isEqualTo(30);

        accrue(s, 200);  // → count 230, well past SUSPENDED(100) — worst tier (index 3)
        ExtractableResponse<Response> blocked = use(s);
        assertThat(blocked.statusCode()).isEqualTo(409);
        assertThat(blocked.path("code").toString()).isEqualTo("TIER_SUSPENDED");
    }

    // ── TIER-TERMINAL-001 — the worst tier is a one-way trip EXCEPT via explicit restore; accruals there
    //    still succeed (count keeps advancing) but tier stays put ──
    @Test @Tag("TIER-TERMINAL-001")
    void worstTier_isOneWay_exceptViaExplicitRestore_furtherAccrualsStillSucceed() {
        String s = createFourTierLadder("tier-" + UUID.randomUUID());
        accrue(s, 150);  // well into SUSPENDED (worst, index 3)
        ExtractableResponse<Response> afterFirst = getLadder(s);
        assertThat(afterFirst.jsonPath().getInt("currentTierIndex")).isEqualTo(3);

        // further accrual at the worst tier is ACCEPTED — count advances, tier stays at the worst index
        ExtractableResponse<Response> more = accrue(s, 25);
        assertThat(more.statusCode()).isEqualTo(200);
        assertThat(more.jsonPath().getInt("count")).isEqualTo(175);
        assertThat(more.jsonPath().getInt("currentTierIndex")).isEqualTo(3);

        // the ONLY way off the worst tier is the explicit, audited restore path
        ExtractableResponse<Response> restored = restore(s, 5, "explicit compliance reset");
        assertThat(restored.statusCode()).isEqualTo(200);
        assertThat(restored.jsonPath().getInt("currentTierIndex")).isEqualTo(0);
    }

    // ── keystone — concurrent accruals crossing the same threshold: no lost update, tier always consistent ──
    @Test @Tag("TIER-LADDER-001") @Tag("TIER-DERIVE-001")
    void concurrentAccruals_noLostUpdate_finalTierConsistentWithFinalCount() throws Exception {
        String s = createFourTierLadder("tier-" + UUID.randomUUID());
        int threads = 8;
        int delta = 15;   // 8 × 15 = 120 — crosses every boundary, lands past SUSPENDED(100)
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        ConcurrentLinkedQueue<Integer> codes = new ConcurrentLinkedQueue<>();
        try {
            for (int i = 0; i < threads; i++) {
                pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    codes.add(accrue(s, delta).statusCode());
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
        assertThat(codes).as("every accrual is accepted — accrual never rejects").allMatch(c -> c == 200);

        ExtractableResponse<Response> after = getLadder(s);
        assertThat(after.jsonPath().getInt("count")).isEqualTo(threads * delta);
        assertThat(after.jsonPath().getInt("currentTierIndex"))
            .as("no interleaving may leave a stale tier — always the derived function of the final count")
            .isEqualTo(3);   // 120 >= 100 → SUSPENDED
    }

    // ── TIER-DERIVE-001 (CWE-362) — use() racing the crossing accrual: BOTH write-paths share the SAME
    //    row lock, so neither request can crash/deadlock/drop under concurrent mixed read+write load, no
    //    accrual is lost, and every use() response is self-consistent with its own reported tier ──
    @Test @Tag("TIER-DERIVE-001") @Tag("TIER-LADDER-001")
    void useRacingCrossingAccrual_neitherWritePathLosesOrCorrupts_underMixedConcurrentLoad() throws Exception {
        String s = createFourTierLadder("tier-" + UUID.randomUUID());
        int accrualThreads = 4;
        int useThreads = 4;
        int delta = 30;   // 4 × 30 = 120 — crosses SUSPENDED(100), same magnitude as the accrual-only keystone
        ExecutorService pool = Executors.newFixedThreadPool(accrualThreads + useThreads);
        CountDownLatch ready = new CountDownLatch(accrualThreads + useThreads);
        CountDownLatch go = new CountDownLatch(1);
        ConcurrentLinkedQueue<Integer> accrueCodes = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<ExtractableResponse<Response>> useResponses = new ConcurrentLinkedQueue<>();
        try {
            for (int i = 0; i < accrualThreads; i++) {
                pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    accrueCodes.add(accrue(s, delta).statusCode());
                    return null;
                });
            }
            for (int i = 0; i < useThreads; i++) {
                pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    useResponses.add(use(s));
                    return null;
                });
            }
            ready.await();
            go.countDown();
            pool.shutdown();
            assertThat(pool.awaitTermination(30, TimeUnit.SECONDS))
                .as("BOTH write-paths share ONE row lock — a mismatched lock order would deadlock/time out here")
                .isTrue();
        } finally {
            pool.shutdownNow();
        }

        // no accrual is lost or double-applied, EVEN with use() calls interleaved on the SAME lock
        assertThat(accrueCodes).as("every accrual is accepted — accrual never rejects, even under a race")
            .hasSize(accrualThreads).allMatch(c -> c == 200);

        // every use() response is well-formed and self-consistent: a 200 reports the live (non-worst) tier
        // it actually observed under its OWN lock acquisition; a 409 is the deterministic terminal code
        assertThat(useResponses).hasSize(useThreads);
        for (ExtractableResponse<Response> r : useResponses) {
            assertThat(r.statusCode()).as("use() must be 200 or 409 — never a crash/timeout artifact")
                .isIn(200, 409);
            if (r.statusCode() == 200) {
                assertThat(r.jsonPath().getInt("currentTierIndex"))
                    .as("a 200 use() must report the tier it actually saw under its lock — never worst").isNotEqualTo(3);
            } else {
                assertThat(r.path("code").toString()).isEqualTo("TIER_SUSPENDED");
            }
        }

        // the final state is deterministic regardless of interleaving — the crossing accrual always wins
        // eventually, and use()'s row lock guarantees it never straddles a half-applied accrual
        ExtractableResponse<Response> after = getLadder(s);
        assertThat(after.jsonPath().getInt("count")).isEqualTo(accrualThreads * delta);
        assertThat(after.jsonPath().getInt("currentTierIndex")).isEqualTo(3);
    }

    // ── RBAC — unauthenticated create is rejected; unknown ladder is an IDOR-safe 404 ──
    @Test
    void rbac_unauthenticatedCreateIsRejected_unknownLadderIs404() {
        given().header("Content-Type", "application/json")
            .body("{\"ladderKey\":\"x\",\"tierNames\":[\"A\",\"B\"],\"thresholds\":[1],\"initialCount\":0}")
        .when().post("/api/tiered-eligibility/ladders").then().statusCode(401);

        given().header("Authorization", "Bearer " + member)
            .when().get("/api/tiered-eligibility/ladders/" + UUID.randomUUID())
            .then().statusCode(404);
    }
}
