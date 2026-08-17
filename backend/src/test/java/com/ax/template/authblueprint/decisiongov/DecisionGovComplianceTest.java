package com.ax.template.authblueprint.decisiongov;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * decision-governance-l0 compliance — verified against the live decisiongov reference workload.
 * The invariant: a computed decision snapshots its appraisal-sufficient basis immutably; a
 * re-determination appends a reasoned NEW version (never an overwrite); a manual override
 * carries a justification AND a four-eyes approver distinct from the requester (DB-backstopped);
 * the per-scope chain is strictly monotonic and concurrent re-determinations serialize on the
 * scope row lock. Spec: specs/decision-governance-l0.yaml (ASOP 41 §3.2 + NIST SP 800-192 SOD).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("DECISIONGOV")
class DecisionGovComplianceTest {

    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbcTemplate;
    String member;

    @BeforeEach
    void setup() {
        member = DecisionTestSupport.obtainToken(DecisionTestSupport.freshEmail("dg-member"), "MEMBER");
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────
    private String compute(String scope, String basis, String outcome) {
        given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"scopeKey\":\"" + scope + "\",\"basisJson\":\"" + basis + "\",\"outcome\":\"" + outcome + "\"}")
        .when().post("/api/decisions").then().statusCode(201);
        return scope;
    }

    private ExtractableResponse<Response> recompute(String scope, String basis, String outcome, String reasonField) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"basisJson\":\"" + basis + "\",\"outcome\":\"" + outcome + "\"" + reasonField + "}")
        .when().post("/api/decisions/" + scope + "/recompute").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> override(String scope, String outcome, String reasonField, String approverField) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"outcome\":\"" + outcome + "\"" + reasonField + approverField + "}")
        .when().post("/api/decisions/" + scope + "/override").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> latest(String scope) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/decisions/" + scope).then().statusCode(200).extract();
    }

    // ── DG-BASIS-001 — the decision carries its basis; a blank basis is rejected ──
    @Test @Tag("DG-BASIS-001")
    void compute_persistsBasisVerbatim_andRejectsBlankBasis() {
        String s = compute("dg-" + UUID.randomUUID(), "{rate:v7,inputs:[a,b]}", "premium=1200");
        ExtractableResponse<Response> v1 = latest(s);
        assertThat(v1.jsonPath().getString("basisJson")).isEqualTo("{rate:v7,inputs:[a,b]}");
        assertThat(v1.jsonPath().getString("kind")).isEqualTo("COMPUTED");
        assertThat(v1.jsonPath().getInt("versionNo")).isEqualTo(1);

        // a blank basis never becomes a determination (bean validation rejects it at the boundary)
        int code = given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"scopeKey\":\"dg-" + UUID.randomUUID() + "\",\"basisJson\":\" \",\"outcome\":\"x\"}")
        .when().post("/api/decisions").thenReturn().statusCode();
        assertThat(code).isEqualTo(400);   // bean validation @NotBlank at the boundary
    }

    // ── DG-RECOMPUTE-001 — reasoned NEW version; prior intact; blank reason 422 ──
    @Test @Tag("DG-RECOMPUTE-001")
    void recompute_appendsReasonedVersion_neverOverwrites() {
        String s = compute("dg-" + UUID.randomUUID(), "basis-v1", "out-1");

        ExtractableResponse<Response> noReason = recompute(s, "basis-v2", "out-2", "");
        assertThat(noReason.statusCode()).isEqualTo(422);
        assertThat(noReason.jsonPath().getString("code")).isEqualTo("DECISION_REASON_REQUIRED");

        ExtractableResponse<Response> v2 = recompute(s, "basis-v2", "out-2", ",\"reason\":\"corrected input\"");
        assertThat(v2.statusCode()).isEqualTo(201);
        assertThat(v2.jsonPath().getInt("versionNo")).isEqualTo(2);
        assertThat(v2.jsonPath().getString("kind")).isEqualTo("RECOMPUTED");

        // v1 is intact and unchanged — correction-by-append, not overwrite
        ExtractableResponse<Response> chain = given().header("Authorization", "Bearer " + member)
            .when().get("/api/decisions/" + s + "/versions").then().statusCode(200).extract();
        assertThat(chain.jsonPath().getList("data.versionNo", Integer.class)).containsExactly(1, 2);
        assertThat(chain.jsonPath().getString("data[0].basisJson")).isEqualTo("basis-v1");
        assertThat(chain.jsonPath().getString("data[0].outcome")).isEqualTo("out-1");
    }

    // ── DG-OVERRIDE-001 — justification + four-eyes approver ≠ requester ──
    @Test @Tag("DG-OVERRIDE-001")
    void override_requiresReason_andDistinctApprover() {
        String s = compute("dg-" + UUID.randomUUID(), "basis-v1", "out-1");

        // approver == actor (the authenticated caller's name) → 422 four-eyes
        String self = given().header("Authorization", "Bearer " + member)
            .when().get("/api/decisions/" + s).then().extract().path("decidedBy");
        ExtractableResponse<Response> selfApproved = override(s, "out-x",
            ",\"reason\":\"fraud check\"", ",\"approvedBy\":\"" + self + "\"");
        assertThat(selfApproved.statusCode()).isEqualTo(422);
        assertThat(selfApproved.jsonPath().getString("code")).isEqualTo("DECISION_FOUR_EYES_REQUIRED");

        // missing approver → 422 four-eyes
        ExtractableResponse<Response> noApprover = override(s, "out-x", ",\"reason\":\"fraud check\"", "");
        assertThat(noApprover.statusCode()).isEqualTo(422);

        // blank reason → 422 reason
        ExtractableResponse<Response> noReason = override(s, "out-x", "", ",\"approvedBy\":\"supervisor-kim\"");
        assertThat(noReason.statusCode()).isEqualTo(422);
        assertThat(noReason.jsonPath().getString("code")).isEqualTo("DECISION_REASON_REQUIRED");

        // proper four-eyes override → 201; records the basis deviated FROM + both identities
        ExtractableResponse<Response> ok = override(s, "out-x",
            ",\"reason\":\"fraud check\"", ",\"approvedBy\":\"supervisor-kim\"");
        assertThat(ok.statusCode()).isEqualTo(201);
        assertThat(ok.jsonPath().getString("kind")).isEqualTo("OVERRIDE");
        assertThat(ok.jsonPath().getString("basisJson")).isEqualTo("basis-v1");
        assertThat(ok.jsonPath().getString("approvedBy")).isEqualTo("supervisor-kim");
        assertThat(ok.jsonPath().getString("decidedBy")).isEqualTo(self);
    }

    // ── DG-OVERRIDE-001 (negative) — the @Check actually rejects a self-approved native write ──
    @Test @Tag("DG-OVERRIDE-001")
    void dbCheck_rejectsSelfApprovedOverride_nativeWrite() {
        String s = compute("dg-" + UUID.randomUUID(), "b", "o");
        UUID scopeId = jdbcTemplate.queryForObject(
            "SELECT id FROM decision_scopes WHERE scope_key = ?", UUID.class, s);
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                jdbcTemplate.update(
                    "INSERT INTO decision_versions (id, scope_id, version_no, kind, basis_json, outcome,"
                        + " reason, decided_by, approved_by, decided_at)"
                        + " VALUES (?, ?, 2, 'OVERRIDE', 'b', 'o2', 'r', 'alice', 'alice', CURRENT_TIMESTAMP)",
                    UUID.randomUUID(), scopeId))
            .as("a code path that forgets the four-eyes gate must fail at the DB")
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    // ── DG-RECOMPUTE-001 (negative) — the @Check also rejects a BLANK reason natively ──
    @Test @Tag("DG-RECOMPUTE-001")
    void dbCheck_rejectsBlankReason_nativeWrite() {
        String s = compute("dg-" + UUID.randomUUID(), "b", "o");
        UUID scopeId = jdbcTemplate.queryForObject(
            "SELECT id FROM decision_scopes WHERE scope_key = ?", UUID.class, s);
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                jdbcTemplate.update(
                    "INSERT INTO decision_versions (id, scope_id, version_no, kind, basis_json, outcome,"
                        + " reason, decided_by, approved_by, decided_at)"
                        + " VALUES (?, ?, 2, 'RECOMPUTED', 'b', 'o2', '  ', 'alice', NULL, CURRENT_TIMESTAMP)",
                    UUID.randomUUID(), scopeId))
            .as("a blank reason must fail at the DB, not only in the service")
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    // ── DG-CHAIN-001 — monotonic chain; latest cheaply addressable ──
    @Test @Tag("DG-CHAIN-001")
    void chain_isMonotonic_latestAddressable() {
        String s = compute("dg-" + UUID.randomUUID(), "b1", "o1");
        recompute(s, "b2", "o2", ",\"reason\":\"r2\"");
        override(s, "o3", ",\"reason\":\"r3\"", ",\"approvedBy\":\"supervisor-kim\"");

        ExtractableResponse<Response> head = latest(s);
        assertThat(head.jsonPath().getInt("versionNo")).isEqualTo(3);
        assertThat(head.jsonPath().getString("kind")).isEqualTo("OVERRIDE");

        ExtractableResponse<Response> chain = given().header("Authorization", "Bearer " + member)
            .when().get("/api/decisions/" + s + "/versions").then().statusCode(200).extract();
        assertThat(chain.jsonPath().getList("data.versionNo", Integer.class)).containsExactly(1, 2, 3);
        assertThat(chain.jsonPath().getList("data.kind", String.class))
            .containsExactly("COMPUTED", "RECOMPUTED", "OVERRIDE");
    }

    // ── DG-CONCURRENT-001 — keystone: N concurrent recomputes → N distinct consecutive versions ──
    @Test @Tag("DG-CONCURRENT-001")
    void concurrentRecomputes_serializeOnScopeLock_noLostOrDuplicateVersions() throws Exception {
        String s = compute("dg-" + UUID.randomUUID(), "b1", "o1");
        int n = 8;

        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch start = new CountDownLatch(1);
        ConcurrentLinkedQueue<Integer> versions = new ConcurrentLinkedQueue<>();
        AtomicInteger failures = new AtomicInteger();
        for (int i = 0; i < n; i++) {
            final int k = i;
            pool.submit(() -> {
                start.await();
                ExtractableResponse<Response> r = recompute(s, "b-c" + k, "o-c" + k, ",\"reason\":\"r" + k + "\"");
                if (r.statusCode() == 201) {
                    versions.add(r.jsonPath().getInt("versionNo"));
                } else {
                    failures.incrementAndGet();
                }
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        assertThat(failures.get()).as("every concurrent recompute commits (lock serializes, none fail)").isZero();
        assertThat(versions).hasSize(n);
        assertThat(versions.stream().distinct().count()).as("no duplicate version slots").isEqualTo(n);
        assertThat(versions.stream().sorted().toList())
            .as("versions are consecutive 2..n+1 — none lost")
            .containsExactlyElementsOf(java.util.stream.IntStream.rangeClosed(2, n + 1).boxed().toList());

        assertThat(latest(s).jsonPath().getInt("versionNo")).isEqualTo(n + 1);
    }
}
