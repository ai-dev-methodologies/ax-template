package com.ax.template.authblueprint.netting;

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
import static org.hamcrest.Matchers.equalTo;

/**
 * collection-conservation-l0 compliance — verified against the live netting reference workload. The
 * invariant: reducing an N×N set of directed gross obligations yields one signed net per member
 * (net = Σ received − Σ sent) whose SET sums to EXACTLY 0 per currency; the reduction runs once
 * (NETTED is terminal); inputs are append-only; currencies never co-net. Spec:
 * specs/collection-conservation-l0.yaml (BIS CPMI multilateral netting).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("NETTING")
class NettingComplianceTest {

    @LocalServerPort int port;
    String member;

    @BeforeEach
    void setup() {
        NettingTestSupport.useRandomPort(port);
        member = NettingTestSupport.obtainToken(NettingTestSupport.freshEmail("net-member"), "MEMBER");
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────
    private String createRun(String runKey, String currency) {
        given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"runKey\":\"" + runKey + "\",\"currency\":\"" + currency + "\"}")
        .when().post("/api/netting/runs").then().statusCode(201);
        return runKey;
    }

    private ExtractableResponse<Response> addObl(String runKey, String from, String to, String amt, String ccy) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"fromMember\":\"" + from + "\",\"toMember\":\"" + to + "\",\"amount\":" + amt + ",\"currency\":\"" + ccy + "\"}")
        .when().post("/api/netting/runs/" + runKey + "/obligations").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> net(String runKey) {
        return given().header("Authorization", "Bearer " + member)
        .when().post("/api/netting/runs/" + runKey + "/net").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> positions(String runKey) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/netting/runs/" + runKey + "/positions?size=100").then().statusCode(200).extract();
    }

    private BigDecimal netOf(ExtractableResponse<Response> pos, String m) {
        int n = pos.jsonPath().getInt("data.size()");
        for (int i = 0; i < n; i++) {
            if (m.equals(pos.jsonPath().getString("data[" + i + "].member"))) {
                return new BigDecimal(pos.jsonPath().getString("data[" + i + "].netAmount"));
            }
        }
        return null;
    }

    // ── NET-SETWIDE-ZERO-001 / NET-PER-NODE-001 — per-node nets + set-wide Σ == 0 ──
    @Test @Tag("NET-SETWIDE-ZERO-001") @Tag("NET-PER-NODE-001")
    void multilateralNetting_perNodeAndSetWide() {
        String r = createRun("run-" + UUID.randomUUID(), "USD");
        // A→B 100, B→C 60, C→A 30  ⇒  A=−70, B=+40, C=+30, Σ=0
        addObl(r, "A", "B", "100", "USD").statusCode();
        addObl(r, "B", "C", "60", "USD").statusCode();
        addObl(r, "C", "A", "30", "USD").statusCode();

        ExtractableResponse<Response> netted = net(r);
        assertThat(netted.statusCode()).isEqualTo(200);
        assertThat(netted.path("status").toString()).isEqualTo("NETTED");
        assertThat(new BigDecimal(netted.jsonPath().getString("netTotal"))).as("set-wide rollup").isEqualByComparingTo("0");

        ExtractableResponse<Response> pos = positions(r);
        assertThat(pos.jsonPath().getInt("data.size()")).isEqualTo(3);
        assertThat(netOf(pos, "A")).as("A net = 30 − 100").isEqualByComparingTo("-70");
        assertThat(netOf(pos, "B")).as("B net = 100 − 60").isEqualByComparingTo("40");
        assertThat(netOf(pos, "C")).as("C net = 60 − 30").isEqualByComparingTo("30");
        // set-wide closure: the member nets sum to exactly zero
        BigDecimal sum = netOf(pos, "A").add(netOf(pos, "B")).add(netOf(pos, "C"));
        assertThat(sum).as("Σ all member nets == 0").isEqualByComparingTo("0");
    }

    // ── NET-PARTITION-001 — no cross-currency netting ──
    @Test @Tag("NET-PARTITION-001")
    void obligationCurrencyMustMatchRun() {
        String r = createRun("run-" + UUID.randomUUID(), "USD");
        ExtractableResponse<Response> bad = addObl(r, "A", "B", "100", "EUR");
        assertThat(bad.statusCode()).isEqualTo(422);
        assertThat(bad.path("code").toString()).isEqualTo("NETTING_CURRENCY_MISMATCH");
        // self-obligation rejected by the service contract (422); a non-positive amount is caught earlier
        // at the @Positive validation boundary (400) — both are rejected, at the appropriate layer.
        assertThat(addObl(r, "A", "A", "10", "USD").statusCode()).isEqualTo(422);
        assertThat(addObl(r, "A", "B", "0", "USD").statusCode()).isEqualTo(400);
    }

    // ── NET-PARTITION-001 / NET-PER-NODE-001 — currency case + member whitespace are normalized,
    //    so a variant never spuriously mismatches or splits a member's net into two positions ──
    @Test @Tag("NET-PARTITION-001") @Tag("NET-PER-NODE-001")
    void currencyAndMemberAreNormalized() {
        String r = createRun("run-" + UUID.randomUUID(), "USD");
        // lowercase currency "usd" normalizes to USD (matches the run); member " A " trims to "A"
        assertThat(addObl(r, " A ", "B", "100", "usd").statusCode()).isEqualTo(201);
        assertThat(addObl(r, "A", "B", "50", "USD").statusCode()).isEqualTo(201);
        assertThat(net(r).statusCode()).isEqualTo(200);

        ExtractableResponse<Response> pos = positions(r);
        // " A " and "A" are the SAME member — exactly two positions (A, B), A not split in two
        assertThat(pos.jsonPath().getInt("data.size()")).isEqualTo(2);
        assertThat(netOf(pos, "A")).as("A owes 150 total").isEqualByComparingTo("-150");
        assertThat(netOf(pos, "B")).as("B is owed 150").isEqualByComparingTo("150");
    }

    // ── NET-INPUTS-IMMUTABLE-001 / NET-ONCE-001 — NETTED is terminal; no add, no re-net ──
    @Test @Tag("NET-INPUTS-IMMUTABLE-001") @Tag("NET-ONCE-001")
    void nettedRunIsTerminal() {
        String r = createRun("run-" + UUID.randomUUID(), "USD");
        addObl(r, "A", "B", "50", "USD");
        addObl(r, "B", "A", "50", "USD");        // A=0, B=0, Σ=0
        assertThat(net(r).statusCode()).isEqualTo(200);

        // re-net → 409 already netted
        ExtractableResponse<Response> renet = net(r);
        assertThat(renet.statusCode()).isEqualTo(409);
        assertThat(renet.path("code").toString()).isEqualTo("NETTING_ALREADY_NETTED");
        // add after NETTED → 409 run not open
        ExtractableResponse<Response> late = addObl(r, "A", "B", "10", "USD");
        assertThat(late.statusCode()).isEqualTo(409);
        assertThat(late.path("code").toString()).isEqualTo("NETTING_RUN_NOT_OPEN");
    }

    // ── NET-ONCE-001 (keystone) — two concurrent reductions → exactly one NETTED, positions once ──
    @Test @Tag("NET-ONCE-001")
    void concurrentNet_runsExactlyOnce() throws InterruptedException {
        String r = createRun("run-" + UUID.randomUUID(), "USD");
        addObl(r, "A", "B", "100", "USD");
        addObl(r, "B", "C", "60", "USD");
        addObl(r, "C", "A", "30", "USD");

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        ConcurrentLinkedQueue<Integer> codes = new ConcurrentLinkedQueue<>();
        try {
            for (int i = 0; i < 2; i++) {
                pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    codes.add(net(r).statusCode());
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
        assertThat(codes.stream().filter(c -> c == 200).count()).as("exactly one reduction commits").isEqualTo(1);
        assertThat(codes.stream().filter(c -> c == 409).count()).as("the other is rejected").isEqualTo(1);

        // positions computed exactly once (3 members, not 6), set-wide Σ still 0
        ExtractableResponse<Response> pos = positions(r);
        assertThat(pos.jsonPath().getInt("data.size()")).isEqualTo(3);
        assertThat(netOf(pos, "A").add(netOf(pos, "B")).add(netOf(pos, "C"))).isEqualByComparingTo("0");
    }

    // ── RBAC — unauthenticated reduction is rejected ──
    @Test
    void rbac_unauthenticatedIsRejected() {
        given().when().post("/api/netting/runs/x/net").then().statusCode(401);
        given().header("Authorization", "Bearer " + member)
            .when().get("/api/netting/runs/" + UUID.randomUUID())
            .then().statusCode(404).body("type", equalTo("urn:problem:not-found"));
    }
}
