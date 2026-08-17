package com.ax.template.authblueprint.costshare;

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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * accumulator-consume-l0 + ordered-waterfall-l0 compliance — verified against the live costshare
 * reference workload. KEYSTONE (ACC-RACE-001): N concurrent consumes racing the last H of headroom
 * each succeed (NONE rejected — the non-rejecting posture) and Σapplied == H exactly (no over-draw)
 * — the partial-fill inverse of bounded-capacity-claim's exactly-N-grants. Spec: specs/accumulator-consume-l0.yaml.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("COSTSHARE")
class CostShareComplianceTest {

    @LocalServerPort int port;
    String admin;
    String member;

    @BeforeEach
    void setup() {
        // exact-decimal JSON parsing so applied/residual/memberPaid come back as BigDecimal, not float
        RestAssured.config = RestAssuredConfig.config().jsonConfig(
            JsonConfig.jsonConfig().numberReturnType(JsonPathConfig.NumberReturnType.BIG_DECIMAL));
        admin = CostShareTestSupport.obtainToken(CostShareTestSupport.freshEmail("cs-admin"), "ADMIN");
        member = CostShareTestSupport.obtainToken(CostShareTestSupport.freshEmail("cs-member"), "MEMBER");
    }

    // ── helpers ─────────────────────────────────────────────────────────────────
    private void createAccumulator(String scopeKey, String limit, String initialUsed) {
        given().header("Authorization", "Bearer " + admin).header("Content-Type", "application/json")
            .body("{\"scopeKey\":\"" + scopeKey + "\",\"limit\":" + limit + ",\"initialUsed\":" + initialUsed + "}")
        .when().post("/api/admin/cost-share/accumulators")
        .then().statusCode(201);
    }

    private ExtractableResponse<Response> consume(String scopeKey, String amount) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"amount\":" + amount + "}")
        .when().post("/api/cost-share/accumulators/" + scopeKey + "/consume").thenReturn().then().extract();
    }

    private BigDecimal usedOf(String scopeKey) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/cost-share/accumulators/" + scopeKey).then().statusCode(200).extract().path("used");
    }

    // ── ACC-ATOMIC-001 / ACC-CONSERVE-001 — non-rejecting partial draw, conserves ──
    @Test @Tag("ACC-ATOMIC-001") @Tag("ACC-CONSERVE-001")
    void consume_partialFill_neverRejects_andConserves() {
        createAccumulator("c1", "100.00", "0.00");
        // full draw under the limit
        ExtractableResponse<Response> r1 = consume("c1", "30.00");
        assertThat(r1.statusCode()).isEqualTo(200);
        assertThat((BigDecimal) r1.path("applied")).isEqualByComparingTo("30.00");
        assertThat((BigDecimal) r1.path("residual")).isEqualByComparingTo("0.00");
        // over-the-limit draw is NOT rejected — it absorbs the headroom (70) and returns residual (20)
        ExtractableResponse<Response> r2 = consume("c1", "90.00");
        assertThat(r2.statusCode()).as("over-limit consume is non-rejecting").isEqualTo(200);
        BigDecimal applied = r2.path("applied");
        BigDecimal residual = r2.path("residual");
        assertThat(applied).isEqualByComparingTo("70.00");
        assertThat(residual).isEqualByComparingTo("20.00");
        assertThat(applied.add(residual)).as("applied+residual==delta").isEqualByComparingTo("90.00");
        assertThat(usedOf("c1")).isEqualByComparingTo("100.00");   // capped at limit
    }

    // ── ACC-RACE-001 — the keystone: concurrent partial draws, none rejected, Σapplied == headroom ──
    @Test @Tag("ACC-RACE-001")
    void concurrentConsume_partialSumsToHeadroom_neverOversells() throws InterruptedException {
        createAccumulator("race", "1500.00", "1460.00");   // headroom = 40
        int threads = 4;                                    // demand 4*20 = 80 > 40
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger ok = new AtomicInteger();
        ConcurrentLinkedQueue<BigDecimal> applieds = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    go.await();
                    ExtractableResponse<Response> r = consume("race", "20.00");
                    if (r.statusCode() == 200) {
                        ok.incrementAndGet();
                        applieds.add(r.path("applied"));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        go.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        assertThat(ok.get()).as("every concurrent consume succeeds — non-rejecting").isEqualTo(threads);
        BigDecimal sum = applieds.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).as("Σapplied == headroom exactly (no over-draw)").isEqualByComparingTo("40.00");
        assertThat(usedOf("race")).as("used lands exactly on limit").isEqualByComparingTo("1500.00");
    }

    // ── ACC-CLAWBACK-001 — release decrements; over-release rejected ─────────────
    @Test @Tag("ACC-CLAWBACK-001")
    void release_decrementsUsed_overReleaseRejected() {
        createAccumulator("cb", "100.00", "0.00");
        consume("cb", "50.00");
        given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"amount\":30.00}")
        .when().post("/api/cost-share/accumulators/cb/release").then().statusCode(200);
        assertThat(usedOf("cb")).isEqualByComparingTo("20.00");
        // releasing more than is accumulated -> 422 (solvency floor)
        given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"amount\":50.00}")
        .when().post("/api/cost-share/accumulators/cb/release")
        .then().statusCode(422).body("code", equalTo("ACCUMULATOR_OVER_RELEASE"));
    }

    // ── ACC-RESET-001 — period reset zeroes used ────────────────────────────────
    @Test @Tag("ACC-RESET-001")
    void reset_zeroesUsed() {
        createAccumulator("rs", "100.00", "0.00");
        consume("rs", "40.00");
        assertThat(usedOf("rs")).isEqualByComparingTo("40.00");
        given().header("Authorization", "Bearer " + member)
        .when().post("/api/cost-share/accumulators/rs/reset").then().statusCode(200);
        assertThat(usedOf("rs")).isEqualByComparingTo("0.00");
    }

    // ── WF-CONSERVE-001 / WF-ATOMIC-001 — allocate conserves; both accumulators advance together ──
    @Test @Tag("WF-CONSERVE-001") @Tag("WF-ATOMIC-001")
    void allocate_conservesExactly_andAdvancesBothAccumulators() {
        createAccumulator("m1-ded", "100.00", "0.00");
        createAccumulator("m1-oop", "1000.00", "0.00");
        ExtractableResponse<Response> r = allocate("1000.00", "m1-ded", "m1-oop", "0.20");
        assertThat(r.statusCode()).isEqualTo(200);
        BigDecimal member = r.path("memberPaid");
        BigDecimal insurer = r.path("insurerPaid");
        // deductible 100 + coinsurance 0.2*900=180 = 280; OOP headroom 1000 -> no clamp
        assertThat(member).isEqualByComparingTo("280.00");
        assertThat(insurer).isEqualByComparingTo("720.00");
        assertThat(member.add(insurer)).as("memberPaid+insurerPaid==eligible").isEqualByComparingTo("1000.00");
        // WF-ATOMIC-001: BOTH accumulators advanced in the one transaction
        assertThat(usedOf("m1-ded")).isEqualByComparingTo("100.00");
        assertThat(usedOf("m1-oop")).isEqualByComparingTo("280.00");
    }

    // ── WF-CLAMP-001 — OOP-max retroactively reduces the coinsurance charge ──────
    @Test @Tag("WF-CLAMP-001")
    void allocate_oopMaxRetroactivelyClampsCoinsurance() {
        createAccumulator("m2-ded", "100.00", "0.00");
        createAccumulator("m2-oop", "150.00", "0.00");   // OOP headroom 150 < uncapped member 280
        ExtractableResponse<Response> r = allocate("1000.00", "m2-ded", "m2-oop", "0.20");
        BigDecimal member = r.path("memberPaid");
        BigDecimal insurer = r.path("insurerPaid");
        // uncapped member would be 280; OOP-max clamps to 150 (insurer absorbs the 130 coinsurance excess)
        assertThat(member).as("member clamped to OOP headroom").isEqualByComparingTo("150.00");
        assertThat(insurer).isEqualByComparingTo("850.00");
        assertThat(member.add(insurer)).as("conservation survives the clamp").isEqualByComparingTo("1000.00");
        assertThat(usedOf("m2-oop")).isEqualByComparingTo("150.00");
        // deductible counts in full here (OOP cap 150 >= deductible 100); the clamp only cut coinsurance
        assertThat(usedOf("m2-ded")).as("deductible accumulator consistent after clamp").isEqualByComparingTo("100.00");
    }

    // ── WF-CLAMP-001 — when OOP cap is BELOW the deductible, the clamp must also claw back the
    //    deductible accumulator (the adversarial-review HIGH bug: ded was advanced by the pre-clamp take) ──
    @Test @Tag("WF-CLAMP-001")
    void allocate_oopBelowDeductible_clampsDeductibleAccumulatorToo() {
        createAccumulator("m4-ded", "1000.00", "0.00");
        createAccumulator("m4-oop", "50.00", "0.00");    // OOP cap 50 < deductible draw
        ExtractableResponse<Response> r = allocate("1000.00", "m4-ded", "m4-oop", "0.00");
        assertThat((BigDecimal) r.path("memberPaid")).isEqualByComparingTo("50.00");
        assertThat((BigDecimal) r.path("insurerPaid")).isEqualByComparingTo("950.00");
        assertThat((BigDecimal) r.path("deductibleApplied"))
            .as("deductible accumulator must NOT record the 1000 pre-clamp draw — only the 50 the member paid")
            .isEqualByComparingTo("50.00");
        assertThat(usedOf("m4-ded")).as("persisted deductible used == member's actual contribution").isEqualByComparingTo("50.00");
        assertThat(usedOf("m4-oop")).isEqualByComparingTo("50.00");
    }

    // ── deductible and OOP-max must be distinct accumulators (else the advance double-counts) ──
    @Test @Tag("WF-LOCK-001")
    void allocate_sameScopeForBothTiers_rejected() {
        createAccumulator("same", "1000.00", "0.00");
        ExtractableResponse<Response> r = allocate("100.00", "same", "same", "0.20");
        assertThat(r.statusCode()).isEqualTo(422);
        assertThat(r.path("code").toString()).isEqualTo("ACCUMULATOR_SAME_SCOPE");
    }

    // ── WF-LOCK-001 — concurrent allocations on the same member are deadlock-free ──
    @Test @Tag("WF-LOCK-001")
    void concurrentAllocate_sameMember_deadlockFree() throws InterruptedException {
        createAccumulator("m3-ded", "1000.00", "0.00");
        createAccumulator("m3-oop", "5000.00", "0.00");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger ok = new AtomicInteger();
        for (int i = 0; i < 2; i++) {
            pool.submit(() -> {
                try {
                    go.await();
                    if (allocate("100.00", "m3-ded", "m3-oop", "0.20").statusCode() == 200) ok.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        go.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        assertThat(ok.get()).as("both allocations complete — no deadlock").isEqualTo(2);
        // two claims of 100, each fully under the deductible -> member paid 100 each -> ded used 200
        assertThat(usedOf("m3-ded")).isEqualByComparingTo("200.00");
    }

    // ── RBAC + IDOR ─────────────────────────────────────────────────────────────
    @Test
    void rbac_memberCannotProvision_unknownIs404_unauthIs401() {
        given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"scopeKey\":\"x\",\"limit\":100.00,\"initialUsed\":0.00}")
        .when().post("/api/admin/cost-share/accumulators").then().statusCode(403);

        given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"amount\":10.00}")
        .when().post("/api/cost-share/accumulators/nope/consume")
        .then().statusCode(404).body("type", equalTo("urn:problem:not-found"));

        given().when().get("/api/cost-share/accumulators/nope").then().statusCode(401);
    }

    private ExtractableResponse<Response> allocate(String eligible, String dedKey, String oopKey, String rate) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"eligible\":" + eligible + ",\"deductibleKey\":\"" + dedKey + "\",\"oopMaxKey\":\""
                + oopKey + "\",\"coinsuranceRate\":" + rate + "}")
        .when().post("/api/cost-share/allocate").thenReturn().then().extract();
    }
}
