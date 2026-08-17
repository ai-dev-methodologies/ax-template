package com.ax.template.authblueprint.approvalworkflow;

import io.restassured.http.ContentType;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Black-box HTTP-surface IT for the approval-workflow (전자결재/결재선) domain.
 *
 * <p><b>Provenance note (Wave-2 Lane E / CELL 6):</b> the coverage-map cell
 * {@code S1.approval-workflow.XB} was marked {@code gap} using a filename
 * census ({@code find backend/src/test -iname '*E2E*' -o -iname '*IT.java'}).
 * That census under-counts: {@link ApprovalComplianceTest} is ALREADY a
 * genuine RestAssured black-box test ({@code @SpringBootTest(webEnvironment =
 * RANDOM_PORT)} hitting the real {@code /api/approvals/**} HTTP surface) — it
 * is just named {@code *ComplianceTest}, not {@code *IT}/{@code *E2E*}, so the
 * naming-pattern census missed it. This class is therefore additive, not a
 * from-scratch fill: it covers reachability-matrix rows the existing suite
 * does not yet assert —
 * <ul>
 *   <li>a truly unrelated non-participant actor (vs. a legitimate approver
 *       acting on someone else's step, which {@link ApprovalComplianceTest
 *       #authz_003_wrongApproverReturns403} already covers),</li>
 *   <li>the ProblemDetail response envelope SHAPE (type/title/status keys) on
 *       the out-of-order 409 (the existing suite only asserts the custom
 *       {@code code} property),</li>
 *   <li>a comprehensive end-to-end 팀장 → 본부장 결재선 walk with final-state
 *       assertions read back through {@code GET} (not just the mutation
 *       response), and</li>
 *   <li>a PageEnvelope-conformance probe on the inbox endpoint — see the
 *       {@code inbox_*} test below for a genuine, previously-undocumented
 *       finding.</li>
 * </ul>
 *
 * Spec: specs/approval-workflow-l0.yaml (WF-LIFECYCLE-002, WF-AUTHZ-003,
 * WF-STEP-001/004/005, WF-QUERY-001).
 *
 * Run: ./gradlew testApprovalWorkflow
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Tag("WORKFLOW")
class ApprovalFlowIT {

    // P2-120: the field stays — com.ax.template.authblueprint.common.AxPort reads it by
    // reflection before every test and is the single writer of RestAssured.port. The manual
    // publish that used to live in a per-test setup method here is gone.
    @LocalServerPort int port;

    // ─── HAPPY — 2-step 결재선 (팀장 → 본부장) end-to-end to APPROVED ───────────

    @Test
    @Tag("WF-LIFECYCLE-002")
    void happyPath_twoStepChain_teamLeadThenDirector_reachesApproved() {
        String requesterToken = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("flow-req"), "MEMBER");
        String teamLeadToken = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("flow-teamlead"), "MANAGER");
        String directorToken = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("flow-director"), "MANAGER");
        String teamLeadId = ApprovalWorkflowTestSupport.resolveUserId(teamLeadToken);
        String directorId = ApprovalWorkflowTestSupport.resolveUserId(directorToken);

        UUID requestId = UUID.fromString(given()
            .header("Authorization", "Bearer " + requesterToken)
            .contentType(ContentType.JSON)
            .body("{\"type\":\"expense\",\"title\":\"Q3 team dinner\","
                + "\"approverUserIds\":[\"" + teamLeadId + "\",\"" + directorId + "\"]}")
        .when().post("/api/approvals")
        .then().statusCode(201)
            .body("status", Matchers.equalTo("DRAFT"))
        .extract().path("id"));

        given()
            .header("Authorization", "Bearer " + requesterToken)
        .when().post("/api/approvals/" + requestId + "/submit")
        .then().statusCode(200)
            .body("status", Matchers.equalTo("SUBMITTED"));

        List<String> stepIds = stepIdsOf(requesterToken, requestId);

        // 팀장 (step 0) approves first — request stays SUBMITTED, step 1 untouched.
        given()
            .header("Authorization", "Bearer " + teamLeadToken).contentType(ContentType.JSON).body("{}")
        .when().post("/api/approvals/" + requestId + "/steps/" + stepIds.get(0) + "/approve")
        .then().statusCode(200)
            .body("status", Matchers.equalTo("SUBMITTED"))
            .body("steps[0].status", Matchers.equalTo("APPROVED"))
            .body("steps[1].status", Matchers.equalTo("PENDING"));

        // 본부장 (step 1) approves last — cascades to APPROVED in the same response.
        given()
            .header("Authorization", "Bearer " + directorToken).contentType(ContentType.JSON).body("{}")
        .when().post("/api/approvals/" + requestId + "/steps/" + stepIds.get(1) + "/approve")
        .then().statusCode(200)
            .body("status", Matchers.equalTo("APPROVED"))
            .body("steps[1].status", Matchers.equalTo("APPROVED"));

        // Read back through GET — final state is durable, not just the mutation response.
        given()
            .header("Authorization", "Bearer " + requesterToken)
        .when().get("/api/approvals/" + requestId)
        .then().statusCode(200)
            .body("status", Matchers.equalTo("APPROVED"))
            .body("steps[0].status", Matchers.equalTo("APPROVED"))
            .body("steps[0].approverUserId", Matchers.equalTo(teamLeadId))
            .body("steps[1].status", Matchers.equalTo("APPROVED"))
            .body("steps[1].approverUserId", Matchers.equalTo(directorId));
    }

    // ─── AUTHZ — reachability matrix ────────────────────────────────────────

    /**
     * A caller with NO relationship to the request at all (not requester, not
     * an approver on any step) attempts to decide on step 0. Distinct from
     * {@link ApprovalComplianceTest#authz_003_wrongApproverReturns403} (which
     * exercises a legitimate approver of a LATER step acting on someone
     * else's step) — this row is the true non-participant case, plus the
     * visibility leg (the outsider cannot even GET the request — 404, not 403).
     */
    @Test
    @Tag("WF-AUTHZ-003")
    void authz_nonParticipant_decidingOnStep_returns403AndCannotView() {
        String requesterToken = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("flow-np-req"), "MEMBER");
        String approverToken = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("flow-np-app"), "MANAGER");
        String approverId = ApprovalWorkflowTestSupport.resolveUserId(approverToken);
        String outsiderToken = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("flow-np-outsider"), "MEMBER");

        UUID requestId = createSubmitted(requesterToken, List.of(approverId));
        String stepId = stepIdsOf(requesterToken, requestId).get(0);

        given()
            .header("Authorization", "Bearer " + outsiderToken).contentType(ContentType.JSON).body("{}")
        .when().post("/api/approvals/" + requestId + "/steps/" + stepId + "/approve")
        .then().statusCode(403)
            .body("code", Matchers.equalTo("NOT_APPROVER"));

        // The outsider also cannot see the request (IDOR-safe 404, not 403 — WF-AUTHZ-002 posture).
        given()
            .header("Authorization", "Bearer " + outsiderToken)
        .when().get("/api/approvals/" + requestId)
        .then().statusCode(404);
    }

    @Test
    @Tag("WF-STEP-005")
    void authz_selfApprove_rejectedAtCreation() {
        String requesterToken = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("flow-self-req"), "MEMBER");
        String requesterId = ApprovalWorkflowTestSupport.resolveUserId(requesterToken);

        given()
            .header("Authorization", "Bearer " + requesterToken)
            .contentType(ContentType.JSON)
            .body("{\"type\":\"expense\",\"approverUserIds\":[\"" + requesterId + "\"]}")
        .when().post("/api/approvals")
        .then().statusCode(400)
            .body("code", Matchers.equalTo("SELF_APPROVE_FORBIDDEN"));
    }

    @Test
    @Tag("WF-STEP-004")
    void authz_duplicateApprover_rejectedAtCreation() {
        String requesterToken = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("flow-dup-req"), "MEMBER");
        String approverToken = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("flow-dup-app"), "MANAGER");
        String approverId = ApprovalWorkflowTestSupport.resolveUserId(approverToken);

        given()
            .header("Authorization", "Bearer " + requesterToken)
            .contentType(ContentType.JSON)
            .body("{\"type\":\"expense\",\"approverUserIds\":[\""
                + approverId + "\",\"" + approverId + "\"]}")
        .when().post("/api/approvals")
        .then().statusCode(400)
            .body("code", Matchers.equalTo("DUPLICATE_APPROVER"));
    }

    // ─── OUT-OF-ORDER — 409 ProblemDetail body SHAPE ────────────────────────

    /**
     * 본부장 (step 1) approves before 팀장 (step 0) has acted. Distinct from
     * {@link ApprovalComplianceTest#step_001_outOfOrderReturns409} — that test
     * only asserts the custom {@code code} property; this one asserts the
     * ProblemDetail envelope SHAPE (title/status/detail keys are present in
     * the body), which nothing in the existing suite checks. {@code type} is
     * NOT asserted: Spring's {@code ProblemDetail} OMITS the {@code type} key
     * entirely when it equals the default {@code about:blank} (RFC 9457
     * compliant), and {@code ApprovalController} builds the ProblemDetail via
     * {@code ProblemDetail.forStatusAndDetail} without setting a custom type
     * URI — so {@code type} correctly does not appear in the body. The shape
     * (key presence for title/status/detail) is the contract this test pins,
     * not the value.
     */
    @Test
    @Tag("WF-STEP-001")
    void outOfOrder_directorBeforeTeamLead_returns409ProblemDetailShape() {
        String requesterToken = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("flow-ooo-req"), "MEMBER");
        String teamLeadToken = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("flow-ooo-teamlead"), "MANAGER");
        String directorToken = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("flow-ooo-director"), "MANAGER");
        String teamLeadId = ApprovalWorkflowTestSupport.resolveUserId(teamLeadToken);
        String directorId = ApprovalWorkflowTestSupport.resolveUserId(directorToken);

        UUID requestId = createSubmitted(requesterToken, List.of(teamLeadId, directorId));
        List<String> stepIds = stepIdsOf(requesterToken, requestId);

        given()
            .header("Authorization", "Bearer " + directorToken).contentType(ContentType.JSON).body("{}")
        .when().post("/api/approvals/" + requestId + "/steps/" + stepIds.get(1) + "/approve")
        .then().statusCode(409)
            .body("code", Matchers.equalTo("STEP_OUT_OF_ORDER"))
            .body("status", Matchers.equalTo(409))
            .body("$", Matchers.hasKey("title"))
            .body("$", Matchers.hasKey("detail"));
    }

    // ─── INBOX — PageEnvelope conformance probe (documents a real finding) ──

    /**
     * {@code GET /api/approvals/inbox} is a list endpoint but does NOT emit
     * the catalog's canonical {@code PageEnvelope} shape ({@code {data,
     * pagination:{page,pageSize,totalElements,totalPages,hasMore}}} — see
     * {@code common/PageEnvelope.java}, which exists specifically to prevent
     * per-domain list-response shape drift). It emits an ad-hoc {@code
     * {items, totalElements}} shape instead (see {@link
     * ApprovalInboxResponse}).
     *
     * <p>This test PINS today's actual (ad-hoc) shape — green — and
     * explicitly documents the divergence. <b>BACKLOG CANDIDATE:</b> migrate
     * {@code /api/approvals/inbox} (and {@code GET /api/approvals}, which has
     * the same {@code {items, totalElements}} shape via {@link
     * ApprovalListResponse}) to {@code PageEnvelope} for consistency with
     * other list endpoints in the catalog. Not fixed here — this task's scope
     * is adding the missing black-box IT coverage, not a breaking response-
     * shape migration; flagged per instruction rather than silently
     * papered over.
     */
    @Test
    @Tag("WF-QUERY-001")
    void inbox_currentShape_isAdHocNotPageEnvelope_backlogCandidate() {
        String approverToken = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("flow-inbox-app"), "MANAGER");
        String approverId = ApprovalWorkflowTestSupport.resolveUserId(approverToken);
        String requesterToken = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("flow-inbox-req"), "MEMBER");

        createSubmitted(requesterToken, List.of(approverId));

        Map<String, Object> body = given()
            .header("Authorization", "Bearer " + approverToken)
        .when().get("/api/approvals/inbox")
        .then().statusCode(200)
            .extract().path("$");

        // Actual (current) shape — passes today.
        assertThat(body).containsKeys("items", "totalElements");

        // NOT the canonical PageEnvelope shape. If this assertion ever starts
        // failing because "data"/"pagination" appear, the migration landed —
        // flip this assertion (and close the backlog candidate) then.
        assertThat(body)
            .as("BACKLOG candidate: /api/approvals/inbox should migrate to the "
                + "canonical PageEnvelope {data, pagination:{...}} shape like other "
                + "list endpoints in the catalog — currently ad-hoc {items, totalElements}")
            .doesNotContainKeys("data", "pagination");
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private UUID createSubmitted(String requesterToken, List<String> approverIds) {
        StringBuilder ids = new StringBuilder();
        for (int i = 0; i < approverIds.size(); i++) {
            if (i > 0) ids.append(',');
            ids.append('"').append(approverIds.get(i)).append('"');
        }
        UUID id = UUID.fromString(given()
            .header("Authorization", "Bearer " + requesterToken)
            .contentType(ContentType.JSON)
            .body("{\"type\":\"expense\",\"approverUserIds\":[" + ids + "]}")
        .when().post("/api/approvals")
        .then().statusCode(201)
            .extract().path("id"));

        given()
            .header("Authorization", "Bearer " + requesterToken)
        .when().post("/api/approvals/" + id + "/submit")
        .then().statusCode(200);
        return id;
    }

    private List<String> stepIdsOf(String requesterToken, UUID requestId) {
        List<Map<String, Object>> steps = given()
            .header("Authorization", "Bearer " + requesterToken)
        .when().get("/api/approvals/" + requestId)
        .then().statusCode(200)
            .extract().path("steps");
        return steps.stream().map(m -> m.get("id").toString()).toList();
    }
}
