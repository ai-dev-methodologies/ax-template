package com.ax.template.authblueprint.saturatingbalance;

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
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * saturating-balance-l0 compliance — verified against the live saturatingbalance reference
 * workload. The invariant: accrual clamps at the cap, debit clamps at zero, neither ever
 * errors; every operation records requested AND applied on an append-only ledger; concurrent
 * accrual near the cap converges to EXACTLY the cap. Spec: specs/saturating-balance-l0.yaml.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("SATURATINGBALANCE")
class SaturatingBalanceComplianceTest {

    @LocalServerPort int port;
    @Autowired SaturatingBalanceService service;
    String token;

    @BeforeEach
    void setup() {
        token = SaturatingBalanceTestSupport.obtainToken(
            SaturatingBalanceTestSupport.freshEmail("satbal"), "MEMBER");
    }

    private String createBalance(BigDecimal cap) {
        return given().header("Authorization", "Bearer " + token).header("Content-Type", "application/json")
            .body("{\"cap\":" + cap + "}")
        .when().post("/api/saturating-balances").then().statusCode(201).extract().path("id");
    }

    private ExtractableResponse<Response> accrue(String id, BigDecimal amount) {
        return given().header("Authorization", "Bearer " + token).header("Content-Type", "application/json")
            .body("{\"amount\":" + amount + "}")
        .when().post("/api/saturating-balances/" + id + "/accrue").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> debit(String id, BigDecimal amount) {
        return given().header("Authorization", "Bearer " + token).header("Content-Type", "application/json")
            .body("{\"amount\":" + amount + "}")
        .when().post("/api/saturating-balances/" + id + "/debit").thenReturn().then().extract();
    }

    // ── SATBAL-CEILING-001 — accrual beyond the cap clamps AT the cap, never errors ──
    @Test @Tag("SATBAL-CEILING-001")
    void accrue_beyondCap_clampsAtCap_neverErrors() {
        String id = createBalance(new BigDecimal("100.0000"));
        assertThat(accrue(id, new BigDecimal("90.0000")).statusCode()).isEqualTo(201);

        ExtractableResponse<Response> over = accrue(id, new BigDecimal("20.0000"));
        assertThat(over.statusCode()).isEqualTo(201);                     // never an error
        assertThat(over.jsonPath().getFloat("appliedAmount")).isEqualTo(10.0f);

        ExtractableResponse<Response> balance = given().header("Authorization", "Bearer " + token)
            .when().get("/api/saturating-balances/" + id).thenReturn().then().extract();
        assertThat(balance.jsonPath().getFloat("current")).isEqualTo(100.0f);

        // already at cap — a further accrual is a normal 200/201 clamped no-op, not an error
        ExtractableResponse<Response> alreadyFull = accrue(id, new BigDecimal("5.0000"));
        assertThat(alreadyFull.statusCode()).isEqualTo(201);
        assertThat(alreadyFull.jsonPath().getFloat("appliedAmount")).isEqualTo(0.0f);
    }

    // ── SATBAL-FLOOR-002 — debit beyond the current balance clamps at 0, never errors or rejects ──
    @Test @Tag("SATBAL-FLOOR-002")
    void debit_beyondCurrent_clampsAtZero_neverErrors() {
        String id = createBalance(new BigDecimal("100.0000"));
        assertThat(accrue(id, new BigDecimal("10.0000")).statusCode()).isEqualTo(201);

        ExtractableResponse<Response> over = debit(id, new BigDecimal("15.0000"));
        assertThat(over.statusCode()).isEqualTo(201);                     // never a rejection
        assertThat(over.jsonPath().getFloat("appliedAmount")).isEqualTo(-10.0f);

        ExtractableResponse<Response> balance = given().header("Authorization", "Bearer " + token)
            .when().get("/api/saturating-balances/" + id).thenReturn().then().extract();
        assertThat(balance.jsonPath().getFloat("current")).isEqualTo(0.0f);

        ExtractableResponse<Response> alreadyEmpty = debit(id, new BigDecimal("5.0000"));
        assertThat(alreadyEmpty.statusCode()).isEqualTo(201);
        assertThat(alreadyEmpty.jsonPath().getFloat("appliedAmount")).isEqualTo(0.0f);
    }

    // ── SATBAL-LEDGER-003 — requested vs applied recorded on every entry; Σ(applied) == current ──
    @Test @Tag("SATBAL-LEDGER-003")
    void ledger_recordsRequestedAndApplied_conservationHolds() {
        String id = createBalance(new BigDecimal("100.0000"));
        accrue(id, new BigDecimal("90.0000"));
        accrue(id, new BigDecimal("20.0000"));                            // clamped: requested 20, applied 10
        debit(id, new BigDecimal("200.0000"));                            // clamped: requested 200, applied -100

        ExtractableResponse<Response> ledger = given().header("Authorization", "Bearer " + token)
            .when().get("/api/saturating-balances/" + id + "/ledger").thenReturn().then().extract();
        List<Float> requested = ledger.jsonPath().getList("requestedAmount", Float.class);
        List<Float> applied = ledger.jsonPath().getList("appliedAmount", Float.class);
        assertThat(requested).containsExactly(90.0f, 20.0f, 200.0f);
        assertThat(applied).containsExactly(90.0f, 10.0f, -100.0f);

        double sumApplied = applied.stream().mapToDouble(Float::doubleValue).sum();
        ExtractableResponse<Response> balance = given().header("Authorization", "Bearer " + token)
            .when().get("/api/saturating-balances/" + id).thenReturn().then().extract();
        assertThat(sumApplied).isEqualTo((double) balance.jsonPath().getFloat("current"));
    }

    // ── SATBAL-CONCURRENT-004 — keystone: N concurrent accruals near the cap converge on EXACTLY the cap ──
    @Test @Tag("SATBAL-CONCURRENT-004")
    void concurrentAccruals_convergeOnExactlyTheCap() throws Exception {
        Balance balance = service.create("concurrent-owner", new BigDecimal("100.0000"));
        service.accrue(balance.getId(), new BigDecimal("95.0000"));       // headroom = 5

        int n = 8;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch start = new CountDownLatch(1);
        ConcurrentLinkedQueue<BigDecimal> applied = new ConcurrentLinkedQueue<>();
        UUID balanceId = balance.getId();
        for (int i = 0; i < n; i++) {
            pool.submit(() -> {
                start.await();
                applied.add(service.accrue(balanceId, new BigDecimal("10.0000")).getAppliedAmount());
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        assertThat(applied).as("every one of the N concurrent calls got its own ledger entry").hasSize(n);
        BigDecimal sumApplied = applied.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sumApplied).isEqualByComparingTo("5.0000");

        Balance reFetched = service.get(balanceId);
        assertThat(reFetched.getCurrent()).as("final balance converges to EXACTLY the cap")
            .isEqualByComparingTo("100.0000");
        assertThat(service.ledgerOf(balanceId)).hasSize(n + 1);           // +1 for the initial 95 accrual
    }

    // ── AuthZ — every endpoint requires a JWT ──
    @Test @Tag("SATBAL-CEILING-001")
    void create_withoutToken_is401() {
        assertThat(given().header("Content-Type", "application/json").body("{\"cap\":100.0000}")
            .when().post("/api/saturating-balances").thenReturn().statusCode()).isEqualTo(401);
    }
}
