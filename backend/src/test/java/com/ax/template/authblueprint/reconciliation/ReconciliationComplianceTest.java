package com.ax.template.authblueprint.reconciliation;

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
 * external-reconciliation-l0 compliance — verified against the live reconciliation reference
 * workload. The invariant: each internal/external pair is classified EXACTLY ONCE with its basis
 * (internal/external amount + delta) recorded; a BREAK requires explicit human disposition before
 * the run can be RESOLVED (an undisposed break is 422); re-running on the SAME feed snapshot hash
 * returns the SAME run, a CHANGED feed appends a new run; concurrent disposes on one break
 * serialize so exactly one wins.
 * Spec: specs/external-reconciliation-l0.yaml (PCAOB AS 2305 + 17 CFR 210.5-02 allowance + CWE-362).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Tag("RECONCILIATION")
class ReconciliationComplianceTest {

    @LocalServerPort int port;
    @Autowired ReconciliationService service;
    String member;

    @BeforeEach
    void setup() {
        ReconciliationTestSupport.useRandomPort(port);
        member = ReconciliationTestSupport.obtainToken(ReconciliationTestSupport.freshEmail("recon-member"), "MEMBER");
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────
    /** A 4-key feed: K-MATCH agrees, K-BREAK differs, K-INTERNAL only internal, K-EXTERNAL only external. */
    private ExtractableResponse<Response> runFourKey(String source, String feedHash) {
        String body = "{\"sourceKey\":\"" + source + "\",\"feedSnapshotHash\":\"" + feedHash + "\","
            + "\"internal\":["
            + "{\"key\":\"K-MATCH\",\"amount\":100.00},"
            + "{\"key\":\"K-BREAK\",\"amount\":50.00},"
            + "{\"key\":\"K-INTERNAL\",\"amount\":25.00}"
            + "],"
            + "\"external\":["
            + "{\"key\":\"K-MATCH\",\"amount\":100.00},"
            + "{\"key\":\"K-BREAK\",\"amount\":30.00},"
            + "{\"key\":\"K-EXTERNAL\",\"amount\":75.00}"
            + "]}";
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body(body)
        .when().post("/api/reconciliation/runs").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> items(String runId) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/reconciliation/runs/" + runId + "/items").then().statusCode(200).extract();
    }

    private String breakItemId(String runId) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/reconciliation/runs/" + runId + "/items")
            .then().statusCode(200).extract().jsonPath()
            .getString("find { it.classification == 'BREAK' }.id");
    }

    private String itemIdByClassification(String runId, String classification) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/reconciliation/runs/" + runId + "/items")
            .then().statusCode(200).extract().jsonPath()
            .getString("find { it.classification == '" + classification + "' }.id");
    }

    private ExtractableResponse<Response> dispose(String runId, String itemId, String type, String reason) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"dispositionType\":\"" + type + "\",\"reason\":\"" + reason + "\"}")
        .when().post("/api/reconciliation/runs/" + runId + "/items/" + itemId + "/disposition")
            .thenReturn().then().extract();
    }

    private ExtractableResponse<Response> resolve(String runId) {
        return given().header("Authorization", "Bearer " + member)
            .when().post("/api/reconciliation/runs/" + runId + "/resolve").thenReturn().then().extract();
    }

    // ── RECON-CLASSIFY-001 — each pair classified once, with recorded basis ──
    @Test @Tag("RECON-CLASSIFY-001")
    void classify_eachPairOnce_withRecordedBasis() {
        ExtractableResponse<Response> run = runFourKey("ACCT-CLASSIFY", "feed-hash-classify-1");
        assertThat(run.statusCode()).isEqualTo(201);
        String runId = run.jsonPath().getString("id");

        ExtractableResponse<Response> items = items(runId);
        List<?> all = items.jsonPath().getList("$");
        assertThat(all).as("4 distinct keys → 4 classified items").hasSize(4);

        // MATCHED — amounts agree
        assertThat(items.jsonPath().getString("find { it.itemKey == 'K-MATCH' }.classification")).isEqualTo("MATCHED");

        // BREAK — amounts differ; the delta (internal - external) is recorded as basis
        assertThat(items.jsonPath().getString("find { it.itemKey == 'K-BREAK' }.classification")).isEqualTo("BREAK");
        assertThat(items.jsonPath().getDouble("find { it.itemKey == 'K-BREAK' }.internalAmount")).isEqualTo(50.0);
        assertThat(items.jsonPath().getDouble("find { it.itemKey == 'K-BREAK' }.externalAmount")).isEqualTo(30.0);
        assertThat(items.jsonPath().getDouble("find { it.itemKey == 'K-BREAK' }.delta")).isEqualTo(20.0);

        // INTERNAL_ONLY — present internally, absent externally
        assertThat(items.jsonPath().getString("find { it.itemKey == 'K-INTERNAL' }.classification")).isEqualTo("INTERNAL_ONLY");
        assertThat(items.jsonPath().getString("find { it.itemKey == 'K-INTERNAL' }.externalAmount")).isNull();

        // EXTERNAL_ONLY — present externally, absent internally
        assertThat(items.jsonPath().getString("find { it.itemKey == 'K-EXTERNAL' }.classification")).isEqualTo("EXTERNAL_ONLY");
        assertThat(items.jsonPath().getString("find { it.itemKey == 'K-EXTERNAL' }.internalAmount")).isNull();
    }

    // ── RECON-DISPOSE-001 — only a BREAK is disposed; who/when/reason recorded ──
    @Test @Tag("RECON-DISPOSE-001")
    void dispose_breakRecordsActorReason_nonBreakIs422_blankReasonIs422() {
        String runId = runFourKey("ACCT-DISPOSE", "feed-hash-dispose-1").jsonPath().getString("id");

        // dispose the BREAK → 200, the disposition (type/by/at/reason) is recorded
        String breakId = breakItemId(runId);
        ExtractableResponse<Response> ok = dispose(runId, breakId, "ACCEPT_INTERNAL", "manual review: internal is authoritative");
        assertThat(ok.statusCode()).isEqualTo(200);
        assertThat(ok.jsonPath().getBoolean("disposed")).isTrue();
        assertThat(ok.jsonPath().getString("dispositionType")).isEqualTo("ACCEPT_INTERNAL");
        assertThat(ok.jsonPath().getString("disposedBy")).isNotBlank();
        assertThat(ok.jsonPath().getString("disposedAt")).isNotBlank();
        assertThat(ok.jsonPath().getString("dispositionReason")).isEqualTo("manual review: internal is authoritative");

        // disposing a MATCHED item → 422 RECON_NOT_A_BREAK
        String matchedId = itemIdByClassification(runId, "MATCHED");
        ExtractableResponse<Response> notBreak = dispose(runId, matchedId, "ADJUST", "should be rejected");
        assertThat(notBreak.statusCode()).isEqualTo(422);
        assertThat(notBreak.jsonPath().getString("code")).isEqualTo("RECON_NOT_A_BREAK");

        // disposing with a blank reason → 422 (a fresh run's break)
        String runId2 = runFourKey("ACCT-DISPOSE-2", "feed-hash-dispose-2").jsonPath().getString("id");
        String breakId2 = breakItemId(runId2);
        ExtractableResponse<Response> blank = dispose(runId2, breakId2, "ACCEPT_EXTERNAL", "   ");
        assertThat(blank.statusCode()).isEqualTo(422);
        assertThat(blank.jsonPath().getString("code")).isEqualTo("RECON_BLANK_REASON");
    }

    // ── RECON-RESOLVE-001 — undisposed break blocks resolution (422); clean run resolves; idempotent ──
    @Test @Tag("RECON-RESOLVE-001")
    void resolve_blockedByUndisposedBreak_thenResolves_idempotent() {
        String runId = runFourKey("ACCT-RESOLVE", "feed-hash-resolve-1").jsonPath().getString("id");

        // an undisposed break blocks resolution
        ExtractableResponse<Response> blocked = resolve(runId);
        assertThat(blocked.statusCode()).isEqualTo(422);
        assertThat(blocked.jsonPath().getString("code")).isEqualTo("RECON_UNDISPOSED_BREAK");

        // dispose the break, then resolve → 200 RESOLVED with resolvedAt
        dispose(runId, breakItemId(runId), "ADJUST", "posting a correcting entry");
        ExtractableResponse<Response> resolved = resolve(runId);
        assertThat(resolved.statusCode()).isEqualTo(200);
        assertThat(resolved.jsonPath().getString("status")).isEqualTo("RESOLVED");
        assertThat(resolved.jsonPath().getString("resolvedAt")).isNotBlank();

        // resolving again is idempotent
        ExtractableResponse<Response> again = resolve(runId);
        assertThat(again.statusCode()).isEqualTo(200);
        assertThat(again.jsonPath().getString("status")).isEqualTo("RESOLVED");
    }

    // ── RECON-IDEMPOTENT-001 — same feed → same run; changed feed → new run, prior retained ──
    @Test @Tag("RECON-IDEMPOTENT-001")
    void idempotent_sameFeedReturnsSameRun_changedFeedAppendsNewRun() {
        String source = "ACCT-IDEM";
        ExtractableResponse<Response> first = runFourKey(source, "feed-hash-A");
        String r1 = first.jsonPath().getString("id");
        int r1Count = items(r1).jsonPath().getList("$").size();

        // SAME source + SAME feed hash → the SAME run (same id, same item count)
        ExtractableResponse<Response> replay = runFourKey(source, "feed-hash-A");
        assertThat(replay.jsonPath().getString("id")).isEqualTo(r1);
        assertThat(items(replay.jsonPath().getString("id")).jsonPath().getList("$").size()).isEqualTo(r1Count);

        // SAME source + CHANGED feed hash → a NEW run; the prior run is retained
        ExtractableResponse<Response> changed = runFourKey(source, "feed-hash-B");
        String r2 = changed.jsonPath().getString("id");
        assertThat(r2).isNotEqualTo(r1);
        given().header("Authorization", "Bearer " + member)
            .when().get("/api/reconciliation/runs/" + r1)
            .then().statusCode(200).body("id", org.hamcrest.Matchers.equalTo(r1));   // R1 still GET-able
    }

    // ── RECON-CONCURRENT-001 — keystone: N concurrent disposes on one break → exactly one wins ──
    @Test @Tag("RECON-CONCURRENT-001")
    void concurrentDisposes_onOneBreak_exactlyOneWins() throws Exception {
        String runId = runFourKey("ACCT-RACE", "feed-hash-race-1").jsonPath().getString("id");
        UUID runUuid = UUID.fromString(runId);
        UUID breakUuid = UUID.fromString(breakItemId(runId));

        int n = 8;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch start = new CountDownLatch(1);
        ConcurrentLinkedQueue<Integer> codes = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < n; i++) {
            pool.submit(() -> {
                start.await();
                try {
                    service.dispose(runUuid, breakUuid, DispositionType.ACCEPT_INTERNAL, "racer", "racer");
                    codes.add(200);
                } catch (ReconciliationException ex) {
                    codes.add(ex.status().value());
                }
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        assertThat(codes.stream().filter(c -> c == 200).count())
            .as("RECON-CONCURRENT-001 — exactly one dispose wins").isEqualTo(1);
        assertThat(codes.stream().filter(c -> c == 409).count()).isEqualTo(n - 1);

        // the break ends with exactly one recorded disposition
        ExtractableResponse<Response> items = items(runId);
        assertThat(items.jsonPath().getBoolean("find { it.classification == 'BREAK' }.disposed")).isTrue();
        assertThat(items.jsonPath().getString("find { it.classification == 'BREAK' }.dispositionType")).isEqualTo("ACCEPT_INTERNAL");
    }
}
