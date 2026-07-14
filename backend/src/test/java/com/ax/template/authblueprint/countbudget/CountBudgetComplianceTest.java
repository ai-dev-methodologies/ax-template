package com.ax.template.authblueprint.countbudget;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

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
 * periodic-count-budget-l0 compliance — verified against the live countbudget reference workload. The
 * invariant: consume is row-lock serialized and fail-closed past the captured cap; the period boundary is
 * purely calendar-derived (never completion-triggered); every consume + first-touch is a permanent
 * queryable row; a cap change affects only NOT-YET-touched periods. Spec:
 * specs/periodic-count-budget-l0.yaml.
 *
 * Time determinism: all period-crossing instants are EXPLICIT fixed ISO-8601 instants on different UTC
 * calendar days (never a relative now()), so there is no real-clock flakiness.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("COUNT_BUDGET")
class CountBudgetComplianceTest {

    @LocalServerPort int port;
    String member;

    private static final String DAY1 = "2026-01-05T10:00:00Z";
    private static final String DAY1_LATER = "2026-01-05T18:00:00Z";
    private static final String DAY2 = "2026-01-06T09:00:00Z";

    @BeforeEach
    void setup() {
        CountBudgetTestSupport.useRandomPort(port);
        member = CountBudgetTestSupport.obtainToken(CountBudgetTestSupport.freshEmail("pcb-member"), "MEMBER");
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────
    private String createPolicy(String key, String cadence, int cap) {
        given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"subjectKey\":\"" + key + "\",\"cadence\":\"" + cadence + "\",\"cap\":" + cap + "}")
        .when().post("/api/count-budgets/policies").then().statusCode(201);
        return key;
    }

    private ExtractableResponse<Response> consume(String key, String asOf) {
        String body = asOf == null ? "{}" : "{\"asOf\":\"" + asOf + "\"}";
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body(body)
        .when().post("/api/count-budgets/policies/" + key + "/consumptions").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> updateCap(String key, int cap) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"cap\":" + cap + "}")
        .when().post("/api/count-budgets/policies/" + key + "/cap").thenReturn().then().extract();
    }

    // ── PCB-CONSUME-001 (keystone) — concurrent consumes serialize; accepted count never exceeds the cap ──
    @Test @Tag("PCB-CONSUME-001")
    void concurrentConsumes_neverExceedCap_rowLockSerialized() throws Exception {
        String key = createPolicy("pcb-" + UUID.randomUUID(), "DAILY", 3);
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        ConcurrentLinkedQueue<Integer> codes = new ConcurrentLinkedQueue<>();
        try {
            for (int i = 0; i < threads; i++) {
                pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    codes.add(consume(key, DAY1).statusCode());
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
        long accepted = codes.stream().filter(c -> c == 201).count();
        long rejected = codes.stream().filter(c -> c == 422).count();
        assertThat(accepted).as("PCB-CONSUME-001 — accepted count never exceeds the captured cap").isEqualTo(3);
        assertThat(accepted + rejected).isEqualTo(threads);
    }

    // ── PCB-RESET-001 — the period boundary is purely calendar-derived; a new day starts a FRESH counter ──
    @Test @Tag("PCB-RESET-001")
    void newCalendarPeriod_startsFreshCounter_noCloseActionRequired() {
        String key = createPolicy("pcb-" + UUID.randomUUID(), "DAILY", 1);

        assertThat(consume(key, DAY1).statusCode()).isEqualTo(201);
        // day 1 is exhausted (cap=1)
        assertThat(consume(key, DAY1_LATER).statusCode()).isEqualTo(422);

        // day 2 — a NEW calendar period, no explicit close call — fresh cap available
        ExtractableResponse<Response> day2 = consume(key, DAY2);
        assertThat(day2.statusCode()).isEqualTo(201);
        assertThat(day2.jsonPath().getLong("consumedCount")).isEqualTo(1);
        assertThat(day2.jsonPath().getString("periodKey")).isNotEqualTo("2026-01-05");
    }

    // ── PCB-AUDIT-001 — every consume + first-touch is a permanent queryable row ──
    @Test @Tag("PCB-AUDIT-001")
    void everyConsumeAndFirstTouch_isPermanentlyQueryable() {
        String key = createPolicy("pcb-" + UUID.randomUUID(), "DAILY", 5);
        consume(key, DAY1);
        consume(key, DAY1);
        consume(key, DAY1);

        ExtractableResponse<Response> ledger = given().header("Authorization", "Bearer " + member)
            .when().get("/api/count-budgets/policies/" + key + "/periods/2026-01-05/consumptions")
            .then().statusCode(200).extract();
        List<Long> seqs = ledger.jsonPath().getList("data.sequenceNo", Long.class);
        assertThat(seqs).containsExactly(1L, 2L, 3L);

        ExtractableResponse<Response> periods = given().header("Authorization", "Bearer " + member)
            .when().get("/api/count-budgets/policies/" + key + "/periods")
            .then().statusCode(200).extract();
        assertThat(periods.jsonPath().getList("data.periodKey")).containsExactly("2026-01-05");
        assertThat(periods.jsonPath().getInt("data[0].capAtPeriodStart")).isEqualTo(5);
    }

    // ── PCB-CAP-001 — a cap change affects only NOT-YET-touched periods ──
    @Test @Tag("PCB-CAP-001")
    void capChange_affectsOnlyFuturePeriods_touchedPeriodKeepsItsCapturedCap() {
        String key = createPolicy("pcb-" + UUID.randomUUID(), "DAILY", 2);

        assertThat(consume(key, DAY1).statusCode()).isEqualTo(201);
        assertThat(consume(key, DAY1_LATER).statusCode()).isEqualTo(201);   // period touched, captured cap = 2

        assertThat(updateCap(key, 5).statusCode()).isEqualTo(200);

        // still the SAME (already-touched) period — the captured cap of 2 still governs, NOT the new 5
        ExtractableResponse<Response> stillExhausted = consume(key, DAY1_LATER);
        assertThat(stillExhausted.statusCode())
            .as("PCB-CAP-001 — an already-touched period keeps its OWN captured cap").isEqualTo(422);

        // the NEXT period picks up the NEW cap (5)
        ExtractableResponse<Response> nextPeriod = consume(key, DAY2);
        assertThat(nextPeriod.statusCode()).isEqualTo(201);
        assertThat(nextPeriod.jsonPath().getInt("capAtPeriodStart"))
            .as("PCB-CAP-001 — a period first-touched AFTER the cap change captures the NEW cap").isEqualTo(5);
    }

    // ── RBAC — unauthenticated consume is rejected; unknown subject is an IDOR-safe 404 ──
    @Test
    void rbac_unauthenticatedConsumeIsRejected_unknownSubjectIs404() {
        given().header("Content-Type", "application/json").body("{}")
        .when().post("/api/count-budgets/policies/x/consumptions").then().statusCode(401);

        given().header("Authorization", "Bearer " + member)
            .when().get("/api/count-budgets/policies/" + UUID.randomUUID())
            .then().statusCode(404);
    }
}
