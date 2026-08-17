package com.ax.template.authblueprint.mandate;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
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
 * mandate-fanout-l0 compliance — verified against the live mandate reference workload.
 * The invariant: ONE directive fans out to EXACTLY N child tasks whose completion is a DERIVED
 * conserved recall (Σ terminal == issuedCount), gated by a pass-all check battery; concurrent
 * explicit completes on one task serialize so exactly one wins. (The deemed-default election is
 * proven in {@link MandateDeemedSweepTest}, which runs with a negative deemed window so a task is
 * immediately overdue.)
 * Spec: specs/mandate-fanout-l0.yaml (van der Aalst WCP-2/3 fan-out + 16 CFR 310.2 deemed + CWE-362).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("MANDATE")
class MandateComplianceTest {

    @LocalServerPort int port;
    @Autowired MandateService service;
    String member;

    @BeforeEach
    void setup() {
        member = MandateTestSupport.obtainToken(MandateTestSupport.freshEmail("mandate-member"), "MEMBER");
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────
    private ExtractableResponse<Response> issueRaw(String directive, int taskCount, String checksJsonArray) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"directive\":\"" + directive + "\",\"taskCount\":" + taskCount
                + ",\"checkKeys\":" + checksJsonArray + "}")
        .when().post("/api/mandate/mandates").thenReturn().then().extract();
    }

    private String issue(String directive, int taskCount, String checksJsonArray) {
        ExtractableResponse<Response> r = issueRaw(directive, taskCount, checksJsonArray);
        assertThat(r.statusCode()).isEqualTo(201);
        return r.path("id");
    }

    private List<String> taskIds(String mandateId) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/mandate/mandates/" + mandateId + "/tasks")
            .then().statusCode(200).extract().jsonPath().getList("id");
    }

    private ExtractableResponse<Response> completeTask(String taskId, String target) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"target\":\"" + target + "\"}")
        .when().post("/api/mandate/tasks/" + taskId + "/complete").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> recordCheck(String mandateId, String key, String verdict) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"checkKey\":\"" + key + "\",\"verdict\":\"" + verdict + "\"}")
        .when().post("/api/mandate/mandates/" + mandateId + "/checks").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> satisfy(String mandateId) {
        return given().header("Authorization", "Bearer " + member)
            .when().post("/api/mandate/mandates/" + mandateId + "/satisfy").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> getMandate(String mandateId) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/mandate/mandates/" + mandateId).then().statusCode(200).extract();
    }

    // ── MANDATE-FANOUT-001 — issue creates exactly N children; completion is a derived recall ──
    @Test @Tag("MANDATE-FANOUT-001")
    void issue_createsExactlyN_completionIsDerivedRecall() {
        String id = issue("Inspect aircraft A320", 3, "[]");

        // issuedCount recorded, exactly 3 PENDING tasks, not complete
        assertThat(getMandate(id).jsonPath().getInt("issuedCount")).isEqualTo(3);
        List<String> tasks = taskIds(id);
        assertThat(tasks).hasSize(3);
        assertThat(getMandate(id).jsonPath().getBoolean("complete")).isFalse();
        assertThat(getMandate(id).jsonPath().getLong("terminalCount")).isEqualTo(0L);

        // complete one → still not complete (the recall counts terminal children)
        assertThat(completeTask(tasks.get(0), "DONE").statusCode()).isEqualTo(200);
        assertThat(getMandate(id).jsonPath().getBoolean("complete")).isFalse();
        assertThat(getMandate(id).jsonPath().getLong("terminalCount")).isEqualTo(1L);

        // complete the rest (mix DONE/DECLINED) → complete=true is DERIVED (Σ terminal == issuedCount)
        assertThat(completeTask(tasks.get(1), "DECLINED").statusCode()).isEqualTo(200);
        assertThat(completeTask(tasks.get(2), "DONE").statusCode()).isEqualTo(200);
        ExtractableResponse<Response> done = getMandate(id);
        assertThat(done.jsonPath().getLong("terminalCount")).isEqualTo(3L);
        assertThat(done.jsonPath().getBoolean("complete")).isTrue();
    }

    // ── MANDATE-FANOUT-001 — N <= 0 is unrepresentable ──
    @Test @Tag("MANDATE-FANOUT-001")
    void issue_withZeroFanout_is422() {
        ExtractableResponse<Response> bad = issueRaw("Empty directive", 0, "[]");
        assertThat(bad.statusCode()).isEqualTo(422);
        assertThat(bad.jsonPath().getString("code")).isEqualTo("MANDATE_EMPTY_FANOUT");
    }

    // ── MANDATE-CONCURRENT-001 — an explicit response is DONE or DECLINED only ──
    @Test @Tag("MANDATE-FANOUT-001")
    void completeTask_withNonTerminalTarget_is422() {
        String id = issue("Has one task", 1, "[]");
        String taskId = taskIds(id).get(0);
        ExtractableResponse<Response> bad = completeTask(taskId, "PENDING");
        assertThat(bad.statusCode()).isEqualTo(422);
        assertThat(bad.jsonPath().getString("code")).isEqualTo("MANDATE_INVALID_TASK_TARGET");
    }

    // ── MANDATE-BATTERY-001 — SATISFIED only when EVERY declared check is PASSED ──
    @Test @Tag("MANDATE-BATTERY-001")
    void battery_satisfiedOnlyWhenAllChecksPassed() {
        String id = issue("Gated directive", 1, "[\"SAFETY\",\"AUTHZ\"]");

        // only SAFETY passed → satisfy 422
        assertThat(recordCheck(id, "SAFETY", "PASSED").statusCode()).isEqualTo(200);
        ExtractableResponse<Response> partial = satisfy(id);
        assertThat(partial.statusCode()).isEqualTo(422);
        assertThat(partial.jsonPath().getString("code")).isEqualTo("MANDATE_BATTERY_INCOMPLETE");

        // AUTHZ failed → satisfy still 422
        assertThat(recordCheck(id, "AUTHZ", "FAILED").statusCode()).isEqualTo(200);
        assertThat(satisfy(id).statusCode()).isEqualTo(422);

        // AUTHZ passed → satisfy 200 SATISFIED
        assertThat(recordCheck(id, "AUTHZ", "PASSED").statusCode()).isEqualTo(200);
        ExtractableResponse<Response> ok = satisfy(id);
        assertThat(ok.statusCode()).isEqualTo(200);
        assertThat(ok.jsonPath().getString("status")).isEqualTo("SATISFIED");

        // the per-check verdicts are recorded individually (not a bare aggregate)
        List<String> verdicts = given().header("Authorization", "Bearer " + member)
            .when().get("/api/mandate/mandates/" + id + "/checks")
            .then().statusCode(200).extract().jsonPath().getList("verdict");
        assertThat(verdicts).containsExactlyInAnyOrder("PASSED", "PASSED");
    }

    // ── MANDATE-BATTERY-001 — an empty battery cannot vacuously satisfy ──
    @Test @Tag("MANDATE-BATTERY-001")
    void battery_emptyBatteryCannotSatisfy() {
        String id = issue("No checks declared", 1, "[]");
        ExtractableResponse<Response> bad = satisfy(id);
        assertThat(bad.statusCode()).isEqualTo(422);
        assertThat(bad.jsonPath().getString("code")).isEqualTo("MANDATE_BATTERY_INCOMPLETE");
    }

    // ── MANDATE-BATTERY-001 — a verdict for an undeclared key is rejected; re-record is idempotent ──
    @Test @Tag("MANDATE-BATTERY-001")
    void battery_unknownCheckRejected_andRecordIsIdempotentOnKey() {
        String id = issue("Two-check directive", 1, "[\"SAFETY\",\"AUTHZ\"]");

        // an undeclared key → 422
        ExtractableResponse<Response> unknown = recordCheck(id, "DOCS", "PASSED");
        assertThat(unknown.statusCode()).isEqualTo(422);
        assertThat(unknown.jsonPath().getString("code")).isEqualTo("MANDATE_UNKNOWN_CHECK");

        // re-recording SAFETY supersedes on the same row — no duplicate row
        assertThat(recordCheck(id, "SAFETY", "FAILED").statusCode()).isEqualTo(200);
        assertThat(recordCheck(id, "SAFETY", "PASSED").statusCode()).isEqualTo(200);
        List<?> checks = given().header("Authorization", "Bearer " + member)
            .when().get("/api/mandate/mandates/" + id + "/checks")
            .then().statusCode(200).extract().jsonPath().getList("checkKey");
        assertThat(checks).hasSize(2);   // SAFETY + AUTHZ, never a third SAFETY row
    }

    // ── MANDATE-CONCURRENT-001 — keystone: N concurrent explicit completes → exactly one wins ──
    @Test @Tag("MANDATE-CONCURRENT-001")
    void concurrentCompletes_exactlyOneWins() throws Exception {
        String id = issue("Race directive", 1, "[]");
        UUID taskId = UUID.fromString(taskIds(id).get(0));

        int n = 8;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch start = new CountDownLatch(1);
        ConcurrentLinkedQueue<Integer> codes = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < n; i++) {
            pool.submit(() -> {
                start.await();
                try {
                    service.completeTask(taskId, MandateTaskState.DONE, "racer");
                    codes.add(200);
                } catch (MandateException ex) {
                    codes.add(ex.status().value());
                }
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        assertThat(codes.stream().filter(c -> c == 200).count())
            .as("MANDATE-CONCURRENT-001 — exactly one explicit complete wins").isEqualTo(1);
        assertThat(codes.stream().filter(c -> c == 409).count()).isEqualTo(n - 1);

        // the task is terminal with exactly one resolver/resolved_at; the mandate is complete (1/1)
        ExtractableResponse<Response> task = given().header("Authorization", "Bearer " + member)
            .when().get("/api/mandate/mandates/" + id + "/tasks").then().statusCode(200).extract();
        assertThat(task.jsonPath().getString("[0].state")).isEqualTo("DONE");
        assertThat(task.jsonPath().getString("[0].resolveReason")).isEqualTo("EXPLICIT");
        assertThat(task.jsonPath().getString("[0].resolvedBy")).isEqualTo("racer");
        assertThat(getMandate(id).jsonPath().getBoolean("complete")).isTrue();
    }

    // ── IDOR / 404 — a non-existent mandate is a problem+json 404 ──
    @Test @Tag("MANDATE-FANOUT-001")
    void get_unknownMandate_is404() {
        ExtractableResponse<Response> missing = given().header("Authorization", "Bearer " + member)
            .when().get("/api/mandate/mandates/" + UUID.randomUUID()).then().extract();
        assertThat(missing.statusCode()).isEqualTo(404);
        assertThat(missing.jsonPath().getString("code")).isEqualTo("RESOURCE_NOT_FOUND");
    }

    // ── AUTHZ — an unauthenticated caller is rejected ──
    @Test @Tag("MANDATE-FANOUT-001")
    void unauthenticated_isRejected() {
        int code = given().header("Content-Type", "application/json")
            .body("{\"directive\":\"x\",\"taskCount\":1,\"checkKeys\":[]}")
        .when().post("/api/mandate/mandates").thenReturn().statusCode();
        assertThat(code).isIn(401, 403);
    }
}
