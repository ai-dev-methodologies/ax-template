package com.ax.template.authblueprint.trueup;

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

/**
 * remeasurement-trueup-l0 compliance — verified against the live trueup reference workload.
 * The invariant: readings supersede append-only (no ACTUAL→ESTIMATED downgrade); runs are
 * versioned with their input basis and recompute idempotently; a CLOSED period is corrected
 * by a net-delta posting into an open period (conservation holds); the grid never computes
 * with silent gaps; the period lifecycle is one-way; concurrent recomputes converge.
 * Spec: specs/remeasurement-trueup-l0.yaml (IAS 8 prospective + PJM M29 §1.5 net-adjustment).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("TRUEUP")
class TrueUpComplianceTest {

    @LocalServerPort int port;
    @Autowired TrueUpService service;
    String member;

    @BeforeEach
    void setup() {
        member = TrueUpTestSupport.obtainToken(TrueUpTestSupport.freshEmail("tu-member"), "MEMBER");
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────
    private String createPeriod(String label, int gridSlots) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"label\":\"" + label + "\",\"gridSlots\":" + gridSlots + "}")
        .when().post("/api/trueup/periods").then().statusCode(201).extract().path("id");
    }

    private ExtractableResponse<Response> record(String periodId, int slot, String value,
                                                 String source, String method) {
        String body = "{\"slotIndex\":" + slot + ",\"value\":" + value + ",\"source\":\"" + source + "\""
            + (method == null ? "" : ",\"estimationMethod\":\"" + method + "\"") + "}";
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body(body)
        .when().post("/api/trueup/periods/" + periodId + "/readings").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> recompute(String periodId, String targetPeriodId) {
        String body = targetPeriodId == null ? "{}" : "{\"targetPeriodId\":\"" + targetPeriodId + "\"}";
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body(body)
        .when().post("/api/trueup/periods/" + periodId + "/recompute").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> post(String path) {
        return given().header("Authorization", "Bearer " + member)
            .when().post("/api/trueup/periods/" + path).thenReturn().then().extract();
    }

    private ExtractableResponse<Response> get(String path) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/trueup/periods/" + path).then().statusCode(200).extract();
    }

    /** A complete 2-slot OPEN period: slot0=100 ACTUAL, slot1=50 ESTIMATED. */
    private String completePeriod(String label) {
        String id = createPeriod(label, 2);
        assertThat(record(id, 0, "100", "ACTUAL", null).statusCode()).isEqualTo(201);
        assertThat(record(id, 1, "50", "ESTIMATED", "METER_PROFILE").statusCode()).isEqualTo(201);
        return id;
    }

    // ── TUP-SUPERSEDE-001 — append-only supersession; no downgrade ──
    @Test @Tag("TUP-SUPERSEDE-001")
    void supersede_appendsNewVersion_pointsForward_andRefusesDowngrade() {
        String id = createPeriod("2026-05", 2);
        String estimateId = record(id, 0, "100", "ESTIMATED", "METER_PROFILE")
            .jsonPath().getString("id");

        ExtractableResponse<Response> actual = record(id, 0, "120", "ACTUAL", null);
        assertThat(actual.statusCode()).isEqualTo(201);
        assertThat(actual.jsonPath().getInt("slotVersion")).isEqualTo(2);

        // the trail keeps both rows: the estimate superseded w/ pointer, value retained verbatim
        java.util.List<java.util.Map<String, Object>> trail =
            get(id + "/readings").jsonPath().getList("$");
        assertThat(trail).hasSize(2);
        java.util.Map<String, Object> superseded = trail.get(0);
        assertThat(superseded.get("id")).isEqualTo(estimateId);
        assertThat(superseded.get("status")).isEqualTo("SUPERSEDED");
        assertThat(superseded.get("supersededById")).isEqualTo(actual.jsonPath().getString("id"));
        assertThat(String.valueOf(superseded.get("value"))).startsWith("100");
        assertThat(superseded.get("source")).isEqualTo("ESTIMATED");
        assertThat(trail.get(1).get("status")).isEqualTo("ACTIVE");           // new row ACTIVE v2

        // facts never degrade back to estimates
        ExtractableResponse<Response> downgrade = record(id, 0, "90", "ESTIMATED", "METER_PROFILE");
        assertThat(downgrade.statusCode()).isEqualTo(422);
        assertThat(downgrade.jsonPath().getString("code")).isEqualTo("TRUEUP_DOWNGRADE");

        // source-method pairing is enforced at the boundary — both directions
        ExtractableResponse<Response> noMethod = record(id, 1, "10", "ESTIMATED", null);
        assertThat(noMethod.statusCode()).isEqualTo(422);
        assertThat(noMethod.jsonPath().getString("code")).isEqualTo("TRUEUP_INVALID_METHOD");
        ExtractableResponse<Response> methodOnActual = record(id, 1, "10", "ACTUAL", "METER_PROFILE");
        assertThat(methodOnActual.statusCode()).isEqualTo(422);
        assertThat(methodOnActual.jsonPath().getString("code")).isEqualTo("TRUEUP_INVALID_METHOD");

        // a reading lands inside the declared grid: slot == gridSlots → domain 422;
        // a negative slot never reaches the service (Bean Validation 400)
        ExtractableResponse<Response> highSlot = record(id, 2, "10", "ACTUAL", null);
        assertThat(highSlot.statusCode()).isEqualTo(422);
        assertThat(highSlot.jsonPath().getString("code")).isEqualTo("TRUEUP_SLOT_RANGE");
        assertThat(record(id, -1, "10", "ACTUAL", null).statusCode()).isEqualTo(400);
    }

    // ── TUP-RUNVERSION-001 — versioned runs with basis; unchanged basis is idempotent ──
    @Test @Tag("TUP-RUNVERSION-001")
    void recompute_versionsRunsWithBasis_andIsIdempotentOnUnchangedBasis() {
        String id = completePeriod("2026-06");

        ExtractableResponse<Response> v1 = recompute(id, null);
        assertThat(v1.statusCode()).isEqualTo(200);
        assertThat(v1.jsonPath().getInt("runVersion")).isEqualTo(1);
        assertThat(v1.jsonPath().getString("basisJson")).contains("\"slot\":0").contains("\"slot\":1");
        assertThat(v1.jsonPath().getString("basisHash")).hasSize(64);
        assertThat(v1.jsonPath().getDouble("totalValue")).isEqualTo(150.0);

        // unchanged basis → the SAME run, no phantom version
        ExtractableResponse<Response> again = recompute(id, null);
        assertThat(again.jsonPath().getString("id")).isEqualTo(v1.jsonPath().getString("id"));
        assertThat(get(id + "/runs").jsonPath().getList("$")).hasSize(1);

        // a supersession changes the basis → version 2; both runs retained
        record(id, 1, "70", "ACTUAL", null);
        ExtractableResponse<Response> v2 = recompute(id, null);
        assertThat(v2.jsonPath().getInt("runVersion")).isEqualTo(2);
        assertThat(v2.jsonPath().getDouble("totalValue")).isEqualTo(170.0);
        assertThat(get(id + "/runs").jsonPath().getList("$")).hasSize(2);
    }

    // ── TUP-DELTA-001 — closed period corrected by net-delta postings; conservation holds ──
    @Test @Tag("TUP-DELTA-001")
    void closedPeriod_correctsForwardAsNetDelta_conservationHolds() {
        String source = completePeriod("2026-01");                 // total 150 at v1
        recompute(source, null);
        assertThat(post(source + "/close").jsonPath().getString("status")).isEqualTo("CLOSED");
        String target = createPeriod("2026-02", 1);                // the open period corrections ride

        // estimate 50 → actual 80: recompute v2 total 180 → posting +30
        record(source, 1, "80", "ACTUAL", null);
        ExtractableResponse<Response> v2 = recompute(source, target);
        assertThat(v2.jsonPath().getInt("runVersion")).isEqualTo(2);
        // actual 80 → corrected actual 60: v3 total 160 → posting = 160 − (150 + 30) = −20
        record(source, 1, "60", "ACTUAL", null);
        ExtractableResponse<Response> v3 = recompute(source, target);
        assertThat(v3.jsonPath().getInt("runVersion")).isEqualTo(3);

        java.util.List<java.util.Map<String, Object>> postings =
            get(source + "/postings").jsonPath().getList("$");
        assertThat(postings).hasSize(2);
        assertThat(((Number) postings.get(0).get("amount")).doubleValue()).isEqualTo(30.0);
        assertThat(((Number) postings.get(1).get("amount")).doubleValue()).isEqualTo(-20.0);
        assertThat(postings.get(0).get("targetPeriodId")).isEqualTo(target);

        // the run-of-record never moved; conservation: 150 + (30 − 20) == 160 == latest total
        ExtractableResponse<Response> period = get(source);
        String runOfRecordId = period.jsonPath().getString("runOfRecordId");
        java.util.List<java.util.Map<String, Object>> runs = get(source + "/runs").jsonPath().getList("$");
        assertThat(runs.get(0).get("id")).isEqualTo(runOfRecordId);
        double recordTotal = ((Number) runs.get(0).get("totalValue")).doubleValue();
        double latestTotal = ((Number) runs.get(2).get("totalValue")).doubleValue();
        double postedSum = postings.stream().mapToDouble(p -> ((Number) p.get("amount")).doubleValue()).sum();
        assertThat(recordTotal + postedSum).isEqualTo(latestTotal);

        // a true-up cannot land in a non-OPEN period
        String closedTarget = completePeriod("2026-03");
        recompute(closedTarget, null);
        post(closedTarget + "/close");
        record(source, 1, "65", "ACTUAL", null);
        ExtractableResponse<Response> bad = recompute(source, closedTarget);
        assertThat(bad.statusCode()).isEqualTo(422);
        assertThat(bad.jsonPath().getString("code")).isEqualTo("TRUEUP_TARGET_NOT_OPEN");

        // ...and recomputing a closed period without naming a target is refused
        ExtractableResponse<Response> noTarget = recompute(source, null);
        assertThat(noTarget.statusCode()).isEqualTo(422);
        assertThat(noTarget.jsonPath().getString("code")).isEqualTo("TRUEUP_TARGET_REQUIRED");
    }

    // ── TUP-GRID-001 — missing slots block the run by name; gap-fill is explicit + recorded ──
    @Test @Tag("TUP-GRID-001")
    void incompleteGrid_blocksNamingSlots_estimateMissingIsExplicitAndRecorded() {
        String id = createPeriod("2026-04", 3);
        record(id, 0, "10", "ACTUAL", null);
        record(id, 1, "20", "ACTUAL", null);                       // slot 2 missing

        ExtractableResponse<Response> blocked = recompute(id, null);
        assertThat(blocked.statusCode()).isEqualTo(422);
        assertThat(blocked.jsonPath().getString("code")).isEqualTo("TRUEUP_GRID_INCOMPLETE");
        assertThat(blocked.jsonPath().getString("detail")).contains("[2]");

        // explicit gap-fill appends an ESTIMATED row with its method recorded
        java.util.List<java.util.Map<String, Object>> created =
            post(id + "/estimate-missing").jsonPath().getList("$");
        assertThat(created).hasSize(1);
        assertThat(created.get(0).get("slotIndex")).isEqualTo(2);
        assertThat(created.get(0).get("source")).isEqualTo("ESTIMATED");
        assertThat(created.get(0).get("estimationMethod")).isEqualTo("CARRY_FORWARD");
        assertThat(((Number) created.get(0).get("value")).doubleValue()).isEqualTo(20.0);

        ExtractableResponse<Response> run = recompute(id, null);
        assertThat(run.statusCode()).isEqualTo(200);
        assertThat(run.jsonPath().getDouble("totalValue")).isEqualTo(50.0);

        // the estimate is later superseded by the actual — the normal TUP-SUPERSEDE path
        record(id, 2, "25", "ACTUAL", null);
        assertThat(recompute(id, null).jsonPath().getInt("runVersion")).isEqualTo(2);
    }

    // ── TUP-SEALED-001 — one-way lifecycle; sealed is fail-closed ──
    @Test @Tag("TUP-SEALED-001")
    void lifecycle_isOneWay_sealedIsFailClosed() {
        String id = completePeriod("2026-07");

        // close without a run → 422; seal an OPEN period → 409
        ExtractableResponse<Response> noRun = post(id + "/close");
        assertThat(noRun.statusCode()).isEqualTo(422);
        assertThat(noRun.jsonPath().getString("code")).isEqualTo("TRUEUP_NO_RUN");
        assertThat(post(id + "/seal").statusCode()).isEqualTo(409);

        recompute(id, null);
        assertThat(post(id + "/close").jsonPath().getString("status")).isEqualTo("CLOSED");
        assertThat(post(id + "/close").statusCode()).isEqualTo(409);          // one-way
        assertThat(post(id + "/seal").jsonPath().getString("status")).isEqualTo("SEALED");

        // sealed accepts nothing
        ExtractableResponse<Response> reading = record(id, 0, "999", "ACTUAL", null);
        assertThat(reading.statusCode()).isEqualTo(409);
        assertThat(reading.jsonPath().getString("code")).isEqualTo("TRUEUP_PERIOD_SEALED");
        ExtractableResponse<Response> sealedRecompute = recompute(id, null);
        assertThat(sealedRecompute.statusCode()).isEqualTo(409);
        assertThat(sealedRecompute.jsonPath().getString("code")).isEqualTo("TRUEUP_PERIOD_SEALED");
        assertThat(post(id + "/seal").statusCode()).isEqualTo(409);           // and stays sealed
    }

    // ── TUP-CONCURRENT-001 — keystone: N concurrent recomputes → ONE new version, ONE posting ──
    @Test @Tag("TUP-CONCURRENT-001")
    void concurrentRecomputes_convergeOnOneVersionAndOnePosting() throws Exception {
        String source = completePeriod("2026-08");
        recompute(source, null);
        post(source + "/close");
        String target = createPeriod("2026-09", 1);
        record(source, 1, "90", "ACTUAL", null);                   // ONE supersession

        UUID sourceId = UUID.fromString(source);
        UUID targetId = UUID.fromString(target);
        int n = 8;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch start = new CountDownLatch(1);
        ConcurrentLinkedQueue<Integer> versions = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < n; i++) {
            pool.submit(() -> {
                start.await();
                versions.add(service.recompute(sourceId, targetId).getRunVersion());
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        assertThat(versions).hasSize(n);
        assertThat(versions.stream().distinct().count())
            .as("TUP-CONCURRENT-001 — all recomputes converge on the same run version").isEqualTo(1);
        assertThat(versions.peek()).isEqualTo(2);
        assertThat(get(source + "/runs").jsonPath().getList("$")).hasSize(2);
        java.util.List<java.util.Map<String, Object>> postings =
            get(source + "/postings").jsonPath().getList("$");
        assertThat(postings).as("exactly one true-up posting").hasSize(1);
        assertThat(((Number) postings.get(0).get("amount")).doubleValue()).isEqualTo(40.0);
    }
}
