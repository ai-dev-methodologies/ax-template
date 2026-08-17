package com.ax.template.authblueprint.authzparity;

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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * authorization-parity-l0 compliance — verified against the live authzparity reference workload.
 * The invariant: an action is APPROVED into an envelope, and EXECUTION is admissible only when
 * (1) the execution parameters re-hash to the SAME canonical parity hash the envelope recorded
 * (executed-matches-authorized), (2) a high-value action carries TWO DISTINCT approver signoffs
 * separated from the requester (four-eyes / NIST two-person rule), and (3) every declared
 * MANDATORY companion gate is recorded present (positive-gates); concurrent executes serialize
 * on the action's row lock and exactly one executes.
 * Spec: specs/authorization-parity-l0.yaml (NIST SP 800-192 SoD + dual-authorization + CWE-362).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("AUTHZPARITY")
class AuthzParityComplianceTest {

    @LocalServerPort int port;
    @Autowired AuthorizationParityService service;
    String requester;       // the action's requester principal
    String approver1;
    String approver2;

    @BeforeEach
    void setup() {
        requester = AuthzParityTestSupport.obtainToken(AuthzParityTestSupport.freshEmail("ap-req"), "MEMBER");
        approver1 = AuthzParityTestSupport.obtainToken(AuthzParityTestSupport.freshEmail("ap-a1"), "MEMBER");
        approver2 = AuthzParityTestSupport.obtainToken(AuthzParityTestSupport.freshEmail("ap-a2"), "MEMBER");
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────
    private String jsonParams(Map<String, String> params) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(e.getKey()).append("\":\"").append(e.getValue()).append("\"");
            first = false;
        }
        return sb.append("}").toString();
    }

    private ExtractableResponse<Response> authorize(String token, String type, Map<String, String> params,
                                                    boolean highValue, List<String> gates) {
        StringBuilder gj = new StringBuilder("[");
        for (int i = 0; i < gates.size(); i++) {
            if (i > 0) gj.append(",");
            gj.append("\"").append(gates.get(i)).append("\"");
        }
        gj.append("]");
        String body = "{\"actionType\":\"" + type + "\",\"authorizedParams\":" + jsonParams(params)
            + ",\"highValue\":" + highValue + ",\"requiredGates\":" + gj + "}";
        return given().header("Authorization", "Bearer " + token).header("Content-Type", "application/json")
            .body(body).when().post("/api/authz-parity/actions").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> signoff(String token, String actionId) {
        return given().header("Authorization", "Bearer " + token)
            .when().post("/api/authz-parity/actions/" + actionId + "/signoffs").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> satisfyGate(String token, String actionId, String gateKey) {
        return given().header("Authorization", "Bearer " + token).header("Content-Type", "application/json")
            .body("{\"gateKey\":\"" + gateKey + "\"}")
            .when().post("/api/authz-parity/actions/" + actionId + "/gates").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> execute(String token, String actionId, Map<String, String> params) {
        return given().header("Authorization", "Bearer " + token).header("Content-Type", "application/json")
            .body("{\"executionParams\":" + jsonParams(params) + "}")
            .when().post("/api/authz-parity/actions/" + actionId + "/execute").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> getAction(String actionId) {
        return given().header("Authorization", "Bearer " + requester)
            .when().get("/api/authz-parity/actions/" + actionId).then().statusCode(200).extract();
    }

    // ── AUTHZPARITY-ENVELOPE-001 — authorize records the immutable envelope ──
    @Test @Tag("AUTHZPARITY-ENVELOPE-001")
    void authorize_recordsEnvelopeWithParityHashAndGates() {
        ExtractableResponse<Response> r = authorize(requester, "WIRE_TRANSFER",
            Map.of("amount", "1000000", "target", "ACC-99"), false, List.of("BUDGET_CHECK"));
        assertThat(r.statusCode()).isEqualTo(201);
        assertThat(r.jsonPath().getString("status")).isEqualTo("AUTHORIZED");
        assertThat(r.jsonPath().getString("parityHash")).isNotBlank().hasSize(64);
        assertThat(r.jsonPath().getString("authorizedParams")).contains("amount=1000000").contains("target=ACC-99");
        assertThat(r.jsonPath().getList("requiredGates")).containsExactly("BUDGET_CHECK");
        assertThat(r.jsonPath().getString("requesterUserId")).isNotBlank();
    }

    // ── AUTHZPARITY-EXEC-001 — a plain action executes only with matching params ──
    @Test @Tag("AUTHZPARITY-EXEC-001")
    void execute_matchingParams_succeeds_andReExecuteIs409() {
        Map<String, String> params = Map.of("amount", "1000000", "target", "ACC-1");
        String id = authorize(requester, "WIRE_TRANSFER", params, false, List.of()).jsonPath().getString("id");

        ExtractableResponse<Response> ok = execute(requester, id, params);
        assertThat(ok.statusCode()).isEqualTo(200);
        assertThat(ok.jsonPath().getString("status")).isEqualTo("EXECUTED");
        assertThat(ok.jsonPath().getString("executedAt")).isNotBlank();

        // re-execute → 409 ALREADY_EXECUTED
        ExtractableResponse<Response> again = execute(requester, id, params);
        assertThat(again.statusCode()).isEqualTo(409);
        assertThat(again.jsonPath().getString("code")).isEqualTo("ALREADY_EXECUTED");
    }

    // ── AUTHZPARITY-EXEC-001 — a substituted parameter is refused 409 + recorded BLOCKED ──
    @Test @Tag("AUTHZPARITY-EXEC-001")
    void execute_substitutedParam_is409_andRecordsBlockedAttempt() {
        Map<String, String> authorized = Map.of("amount", "1000000", "target", "ACC-1");
        String id = authorize(requester, "WIRE_TRANSFER", authorized, false, List.of()).jsonPath().getString("id");
        String authorizedHash = getAction(id).jsonPath().getString("parityHash");

        // escalate the amount — a different artifact
        Map<String, String> escalated = Map.of("amount", "10000000", "target", "ACC-1");
        ExtractableResponse<Response> blocked = execute(requester, id, escalated);
        assertThat(blocked.statusCode()).isEqualTo(409);
        assertThat(blocked.jsonPath().getString("code")).isEqualTo("PARITY_MISMATCH");

        // the action did NOT execute
        assertThat(getAction(id).jsonPath().getString("status")).isEqualTo("AUTHORIZED");

        // the mismatch was recorded as a BLOCKED attempt (offered != authorized)
        List<Map<String, Object>> attempts = given().header("Authorization", "Bearer " + requester)
            .when().get("/api/authz-parity/actions/" + id + "/blocked-attempts")
            .then().statusCode(200).extract().jsonPath().getList("$");
        assertThat(attempts).hasSize(1);
        assertThat(attempts.get(0).get("authorizedHash")).isEqualTo(authorizedHash);
        assertThat(attempts.get(0).get("offeredHash")).isNotEqualTo(authorizedHash);

        // and a CORRECT retry still works (BLOCKED is not terminal)
        assertThat(execute(requester, id, authorized).statusCode()).isEqualTo(200);
    }

    // ── AUTHZPARITY-EXEC-001 — parity is order-independent (canonical hash) ──
    @Test @Tag("AUTHZPARITY-EXEC-001")
    void execute_paramOrderIndependent() {
        // authorize with one insertion order; execute with the SAME pairs (Map has no guaranteed order)
        Map<String, String> authorized = new java.util.LinkedHashMap<>();
        authorized.put("zebra", "1");
        authorized.put("alpha", "2");
        String id = authorize(requester, "REORDER", authorized, false, List.of()).jsonPath().getString("id");

        Map<String, String> reordered = new java.util.LinkedHashMap<>();
        reordered.put("alpha", "2");
        reordered.put("zebra", "1");
        assertThat(execute(requester, id, reordered).statusCode()).isEqualTo(200);
    }

    // ── AUTHZPARITY-FOUREYES-001 — two distinct signoffs; self/duplicate rejected ──
    @Test @Tag("AUTHZPARITY-FOUREYES-001")
    void fourEyes_needsTwoDistinctApprovers_selfAndDuplicateRejected() {
        Map<String, String> params = Map.of("amount", "50000000", "target", "ACC-HV");
        String id = authorize(requester, "WIRE_TRANSFER", params, true, List.of()).jsonPath().getString("id");

        // requester cannot sign their own action → 422 SELF_SIGNOFF
        ExtractableResponse<Response> self = signoff(requester, id);
        assertThat(self.statusCode()).isEqualTo(422);
        assertThat(self.jsonPath().getString("code")).isEqualTo("SELF_SIGNOFF");

        // first distinct approver → 201
        assertThat(signoff(approver1, id).statusCode()).isEqualTo(201);

        // same approver again → 422 DUPLICATE_SIGNOFF
        ExtractableResponse<Response> dup = signoff(approver1, id);
        assertThat(dup.statusCode()).isEqualTo(422);
        assertThat(dup.jsonPath().getString("code")).isEqualTo("DUPLICATE_SIGNOFF");

        // execute with ONE signoff → 422 INSUFFICIENT_SIGNOFFS
        ExtractableResponse<Response> tooFew = execute(requester, id, params);
        assertThat(tooFew.statusCode()).isEqualTo(422);
        assertThat(tooFew.jsonPath().getString("code")).isEqualTo("INSUFFICIENT_SIGNOFFS");

        // second distinct approver → execute now succeeds
        assertThat(signoff(approver2, id).statusCode()).isEqualTo(201);
        ExtractableResponse<Response> ok = execute(requester, id, params);
        assertThat(ok.statusCode()).isEqualTo(200);
        assertThat(ok.jsonPath().getString("status")).isEqualTo("EXECUTED");

        // two distinct signoffs are recorded
        List<Map<String, Object>> signoffs = given().header("Authorization", "Bearer " + requester)
            .when().get("/api/authz-parity/actions/" + id + "/signoffs")
            .then().statusCode(200).extract().jsonPath().getList("$");
        assertThat(signoffs).hasSize(2);
    }

    // ── AUTHZPARITY-GATES-001 — every declared companion gate must be present; unknown rejected ──
    @Test @Tag("AUTHZPARITY-GATES-001")
    void positiveGates_executeRefusedUntilAllDeclaredGatesPresent() {
        Map<String, String> params = Map.of("amount", "1000000", "target", "ACC-G");
        String id = authorize(requester, "PROCUREMENT", params, false,
            List.of("BUDGET_CHECK", "AML_SCREEN")).jsonPath().getString("id");

        // execute before any gate → 422 MISSING_COMPANION_GATE
        ExtractableResponse<Response> none = execute(requester, id, params);
        assertThat(none.statusCode()).isEqualTo(422);
        assertThat(none.jsonPath().getString("code")).isEqualTo("MISSING_COMPANION_GATE");

        // satisfy an UNDECLARED gate → 422 UNKNOWN_GATE
        ExtractableResponse<Response> unknown = satisfyGate(approver1, id, "BRIBE_CHECK");
        assertThat(unknown.statusCode()).isEqualTo(422);
        assertThat(unknown.jsonPath().getString("code")).isEqualTo("UNKNOWN_GATE");

        // satisfy ONE declared gate, execute → still 422 (the other is missing)
        assertThat(satisfyGate(approver1, id, "BUDGET_CHECK").statusCode()).isEqualTo(201);
        ExtractableResponse<Response> partial = execute(requester, id, params);
        assertThat(partial.statusCode()).isEqualTo(422);
        assertThat(partial.jsonPath().getString("code")).isEqualTo("MISSING_COMPANION_GATE");

        // re-satisfying the same gate → 409 GATE_ALREADY_SATISFIED
        ExtractableResponse<Response> resat = satisfyGate(approver1, id, "BUDGET_CHECK");
        assertThat(resat.statusCode()).isEqualTo(409);
        assertThat(resat.jsonPath().getString("code")).isEqualTo("GATE_ALREADY_SATISFIED");

        // satisfy the last gate → execute now succeeds
        assertThat(satisfyGate(approver2, id, "AML_SCREEN").statusCode()).isEqualTo(201);
        assertThat(execute(requester, id, params).jsonPath().getString("status")).isEqualTo("EXECUTED");
    }

    // ── AUTHZPARITY-GATES + FOUREYES + EXEC — the full high-value path composes ──
    @Test @Tag("AUTHZPARITY-GATES-001") @Tag("AUTHZPARITY-FOUREYES-001")
    void fullHighValuePath_requiresParity_twoSignoffs_andAllGates() {
        Map<String, String> params = Map.of("amount", "99000000", "target", "ACC-BIG");
        String id = authorize(requester, "WIRE_TRANSFER", params, true,
            List.of("AML_SCREEN")).jsonPath().getString("id");

        // gates + signoffs present but a WRONG param at execute → 409 PARITY_MISMATCH first
        assertThat(signoff(approver1, id).statusCode()).isEqualTo(201);
        assertThat(signoff(approver2, id).statusCode()).isEqualTo(201);
        assertThat(satisfyGate(approver1, id, "AML_SCREEN").statusCode()).isEqualTo(201);
        assertThat(execute(requester, id, Map.of("amount", "1", "target", "ACC-BIG"))
            .jsonPath().getString("code")).isEqualTo("PARITY_MISMATCH");

        // correct params → EXECUTED
        assertThat(execute(requester, id, params).jsonPath().getString("status")).isEqualTo("EXECUTED");
    }

    // ── AUTHZPARITY-CONCURRENT-001 — keystone: N concurrent executes → exactly one wins ──
    @Test @Tag("AUTHZPARITY-CONCURRENT-001")
    void concurrentExecutes_exactlyOneWins() throws Exception {
        Map<String, String> params = Map.of("amount", "1000000", "target", "ACC-RACE");
        UUID id = UUID.fromString(
            authorize(requester, "WIRE_TRANSFER", params, false, List.of()).jsonPath().getString("id"));

        int n = 8;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch start = new CountDownLatch(1);
        ConcurrentLinkedQueue<Integer> codes = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < n; i++) {
            pool.submit(() -> {
                start.await();
                try {
                    service.execute(id, params, "racer");
                    codes.add(200);
                } catch (AuthorizationParityException ex) {
                    codes.add(ex.status().value());
                }
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        assertThat(codes.stream().filter(c -> c == 200).count())
            .as("AUTHZPARITY-CONCURRENT-001 — exactly one execute wins").isEqualTo(1);
        assertThat(codes.stream().filter(c -> c == 409).count()).isEqualTo(n - 1);
        assertThat(getAction(id.toString()).jsonPath().getString("status")).isEqualTo("EXECUTED");
    }
}
