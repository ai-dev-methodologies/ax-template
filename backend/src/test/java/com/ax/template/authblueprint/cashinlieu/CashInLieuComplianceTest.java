package com.ax.template.authblueprint.cashinlieu;

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
 * cash-in-lieu-l0 compliance — verified against the live cashinlieu reference workload. The
 * invariant: a fractional entitlement splits into integer units-in-kind + a fractional remainder
 * that reconstructs the entitlement exactly; the fraction is NEVER allocated in kind — it is
 * monetized at a recorded rate snapshot; allocation is idempotent per (subject, event).
 * Spec: specs/cash-in-lieu-l0.yaml.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("CASH_IN_LIEU")
class CashInLieuComplianceTest {

    @LocalServerPort int port;
    String member;

    @BeforeEach
    void setup() {
        member = CashInLieuTestSupport.obtainToken(CashInLieuTestSupport.freshEmail("cil-member"), "MEMBER");
    }

    private ExtractableResponse<Response> allocate(String subject, String event, String qty, String ratio, String rate) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"subjectRef\":\"" + subject + "\",\"eventRef\":\"" + event + "\",\"holdingQuantity\":"
                + qty + ",\"ratio\":" + ratio + ",\"cashRate\":" + rate + "}")
        .when().post("/api/cash-in-lieu/allocations").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> get(String subject, String event) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/cash-in-lieu/allocations/" + subject + "/" + event).then().statusCode(200).extract();
    }

    // ── CIL-FRACTION-001 — integer units-in-kind + fraction NEVER allocated in kind, monetized at the rate snapshot ──
    @Test @Tag("CIL-FRACTION-001")
    void fractionalEntitlement_splitsIntoWholeUnits_andCashForTheRemainder() {
        String subject = "sub-" + UUID.randomUUID();
        String event = "evt-" + UUID.randomUUID();
        ExtractableResponse<Response> alloc = allocate(subject, event, "10", "0.35", "7.33");
        assertThat(alloc.statusCode()).isEqualTo(201);
        assertThat(alloc.jsonPath().getLong("unitsInKind"))
            .as("CIL-FRACTION-001 — the integer part is allocated in kind")
            .isEqualTo(3L);
        assertThat(new BigDecimal(alloc.jsonPath().getString("fractionalRemainder"))).isEqualByComparingTo("0.5");
        assertThat(new BigDecimal(alloc.jsonPath().getString("cashRate")))
            .as("the rate snapshot that valued the remainder")
            .isEqualByComparingTo("7.33");
        assertThat(new BigDecimal(alloc.jsonPath().getString("cashValue")))
            .as("0.5 * 7.33 rounded HALF_UP")
            .isEqualByComparingTo("3.67");
    }

    @Test @Tag("CIL-FRACTION-001")
    void invalidInputs_rejected422() {
        String subject = "sub-" + UUID.randomUUID();
        assertThat(allocate(subject, "evt-a", "0", "0.35", "7.33").statusCode()).isEqualTo(422);
        assertThat(allocate(subject, "evt-b", "10", "0", "7.33").statusCode()).isEqualTo(422);
        assertThat(allocate(subject, "evt-c", "10", "0.35", "0").statusCode()).isEqualTo(422);
    }

    @Test @Tag("CIL-FRACTION-001")
    void allocationNotFound_is404() {
        assertThat(given().header("Authorization", "Bearer " + member)
            .when().get("/api/cash-in-lieu/allocations/nope/nope").thenReturn().statusCode())
            .isEqualTo(404);
    }

    // ── CIL-CONSERVE-002 — units + fractional remainder reconstructs the entitlement exactly ──
    @Test @Tag("CIL-CONSERVE-002")
    void unitsPlusFractionalRemainder_reconstructsRawEntitlementExactly() {
        String subject = "sub-" + UUID.randomUUID();
        String event = "evt-" + UUID.randomUUID();
        // a repeating-decimal ratio — exercises a remainder that does not resolve to a tidy fraction
        ExtractableResponse<Response> alloc = allocate(subject, event, "103", "0.333333", "12.5");
        assertThat(alloc.statusCode()).isEqualTo(201);
        // RestAssured's default JsonPath parses JSON numbers as Double — a 6-decimal figure loses
        // precision that way, so this exactness assertion needs the BIG_DECIMAL number return type
        // (same workaround ThresholdTerminalComplianceTest uses).
        io.restassured.path.json.JsonPath exact = alloc.body().jsonPath(
            new io.restassured.path.json.config.JsonPathConfig(
                io.restassured.path.json.config.JsonPathConfig.NumberReturnType.BIG_DECIMAL));
        BigDecimal raw = new BigDecimal(exact.getString("rawEntitlement"));
        BigDecimal units = BigDecimal.valueOf(exact.getLong("unitsInKind"));
        BigDecimal remainder = new BigDecimal(exact.getString("fractionalRemainder"));
        assertThat(units.add(remainder))
            .as("CIL-CONSERVE-002 — exact reconstruction, independent of the separate cash rounding")
            .isEqualByComparingTo(raw);
        assertThat(remainder).isGreaterThanOrEqualTo(BigDecimal.ZERO).isLessThan(BigDecimal.ONE);
    }

    // ── CIL-IDEMPOTENT-003 — idempotent per (subject, event); a rerun with DIFFERENT inputs still returns the frozen first allocation ──
    @Test @Tag("CIL-IDEMPOTENT-003")
    void allocation_idempotentPerSubjectEvent_rerunReturnsFrozenFirstAllocation() {
        String subject = "sub-" + UUID.randomUUID();
        String event = "evt-" + UUID.randomUUID();
        ExtractableResponse<Response> first = allocate(subject, event, "10", "0.35", "7.33");
        assertThat(first.statusCode()).isEqualTo(201);

        ExtractableResponse<Response> replay = allocate(subject, event, "999", "0.9", "1.0");   // different inputs
        assertThat(replay.statusCode()).isEqualTo(201);
        assertThat(replay.jsonPath().getString("id")).isEqualTo(first.jsonPath().getString("id"));
        assertThat(replay.jsonPath().getLong("unitsInKind"))
            .as("CIL-IDEMPOTENT-003 — the FIRST allocation is authoritative and frozen")
            .isEqualTo(first.jsonPath().getLong("unitsInKind"));
        assertThat(replay.jsonPath().getString("cashValue")).isEqualTo(first.jsonPath().getString("cashValue"));

        ExtractableResponse<Response> fetched = get(subject, event);
        assertThat(fetched.jsonPath().getString("id")).isEqualTo(first.jsonPath().getString("id"));
    }

    /**
     * CIL-IDEMPOTENT-003 concurrency keystone (P1-64) — two concurrent allocates on the SAME
     * (subject, event) both pass the pre-check before either commits; the loser hits the
     * uq(subject,event) constraint. Both MUST resolve to the SAME winner row and NEITHER may surface
     * a 500. The racy insert is isolated in a REQUIRES_NEW inner tx so the loser's requery runs on an
     * unpoisoned connection — on PostgreSQL a same-tx requery would fail with 25P02 (500). H2 cannot
     * reproduce 25P02, so this test proves the happy-path contract holds; the structural REQUIRES_NEW
     * lock in CashInLieuViolationProofTest is the real regression guard against reverting the fix.
     */
    @Test @Tag("CIL-IDEMPOTENT-003")
    void concurrentAllocateSameKey_singleWinner_neither500() throws Exception {
        String subject = "sub-" + UUID.randomUUID();
        String event = "evt-" + UUID.randomUUID();
        int n = 2;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch start = new CountDownLatch(1);
        ConcurrentLinkedQueue<Integer> statusCodes = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<String> ids = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < n; i++) {
            pool.submit(() -> {
                start.await();
                ExtractableResponse<Response> r = allocate(subject, event, "10", "0.35", "7.33");
                statusCodes.add(r.statusCode());
                if (r.statusCode() == 201) ids.add(r.jsonPath().getString("id"));
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        assertThat(statusCodes).as("neither racer surfaces an unmapped 500").allMatch(code -> code == 201);
        assertThat(ids.stream().distinct()).as("both calls resolve to the SAME winner row").hasSize(1);
    }
}
