package com.ax.template.authblueprint.approvalworkflow;

import io.restassured.http.ContentType;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Compliance tests for the approval-workflow domain (R31). RestAssured black-box.
 * State-machine unit coverage lives in {@link ApprovalRequestStateMachineTest} +
 * {@link ApprovalStepStateMachineTest}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("WORKFLOW")
class ApprovalComplianceTest {

    @LocalServerPort int port;

    @BeforeEach
    void setup() {
        ApprovalWorkflowTestSupport.useRandomPort(port);
    }

    // ─── LIFECYCLE family ────────────────────────────────────────────────────

    @Test
    @Tag("WF-LIFECYCLE-001")
    void lifecycle_001_postCreatesDraftInvisibleToApprovers() {
        String requesterToken = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("wf1-req"), "MEMBER");
        String approverToken = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("wf1-app"), "MEMBER");
        String approverId = ApprovalWorkflowTestSupport.resolveUserId(approverToken);

        given()
            .header("Authorization", "Bearer " + requesterToken)
            .contentType(ContentType.JSON)
            .body("{\"type\":\"budget\",\"approverUserIds\":[\"" + approverId + "\"]}")
        .when().post("/api/approvals")
        .then()
            .statusCode(201)
            .body("status", Matchers.equalTo("DRAFT"))
            .body("steps[0].status", Matchers.equalTo("PENDING"));

        // Approver's inbox is empty because the request is still DRAFT.
        given()
            .header("Authorization", "Bearer " + approverToken)
        .when().get("/api/approvals/inbox")
        .then().statusCode(200)
            .body("totalElements", Matchers.equalTo(0));
    }

    @Test
    @Tag("WF-LIFECYCLE-002")
    void lifecycle_002_lastStepApproveCascadesRequestApproved() {
        String requesterToken = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("wf2-req"), "MEMBER");
        String app1Token = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("wf2-a1"), "MEMBER");
        String app2Token = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("wf2-a2"), "MEMBER");
        String app1Id = ApprovalWorkflowTestSupport.resolveUserId(app1Token);
        String app2Id = ApprovalWorkflowTestSupport.resolveUserId(app2Token);

        UUID requestId = createSubmittedRequest(requesterToken, List.of(app1Id, app2Id));
        List<String> stepIds = listStepIds(requesterToken, requestId);

        // Approve step 0 → request stays SUBMITTED.
        given()
            .header("Authorization", "Bearer " + app1Token)
            .contentType(ContentType.JSON)
            .body("{}")
        .when().post("/api/approvals/" + requestId + "/steps/" + stepIds.get(0) + "/approve")
        .then().statusCode(200)
            .body("status", Matchers.equalTo("SUBMITTED"));

        // Approve step 1 → cascades to APPROVED in the same response.
        given()
            .header("Authorization", "Bearer " + app2Token)
            .contentType(ContentType.JSON)
            .body("{}")
        .when().post("/api/approvals/" + requestId + "/steps/" + stepIds.get(1) + "/approve")
        .then().statusCode(200)
            .body("status", Matchers.equalTo("APPROVED"));
    }

    @Test
    @Tag("WF-LIFECYCLE-003")
    void lifecycle_003_stepRejectionCascadesAndShortCircuitsLaterSteps() {
        String requesterToken = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("wf3-req"), "MEMBER");
        String app1Token = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("wf3-a1"), "MEMBER");
        String app2Token = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("wf3-a2"), "MEMBER");
        String app3Token = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("wf3-a3"), "MEMBER");
        String app1Id = ApprovalWorkflowTestSupport.resolveUserId(app1Token);
        String app2Id = ApprovalWorkflowTestSupport.resolveUserId(app2Token);
        String app3Id = ApprovalWorkflowTestSupport.resolveUserId(app3Token);

        UUID requestId = createSubmittedRequest(requesterToken, List.of(app1Id, app2Id, app3Id));
        List<String> stepIds = listStepIds(requesterToken, requestId);

        // app1 approves; app2 rejects → request REJECTED.
        given()
            .header("Authorization", "Bearer " + app1Token).contentType(ContentType.JSON).body("{}")
        .when().post("/api/approvals/" + requestId + "/steps/" + stepIds.get(0) + "/approve")
        .then().statusCode(200);

        given()
            .header("Authorization", "Bearer " + app2Token).contentType(ContentType.JSON).body("{}")
        .when().post("/api/approvals/" + requestId + "/steps/" + stepIds.get(1) + "/reject")
        .then().statusCode(200)
            .body("status", Matchers.equalTo("REJECTED"));

        // app3 attempts to approve step 2 → 409 REQUEST_TERMINAL.
        given()
            .header("Authorization", "Bearer " + app3Token).contentType(ContentType.JSON).body("{}")
        .when().post("/api/approvals/" + requestId + "/steps/" + stepIds.get(2) + "/approve")
        .then().statusCode(409)
            .body("code", Matchers.equalTo("REQUEST_TERMINAL"));
    }

    // WF-LIFECYCLE-004 covered by ApprovalRequestStateMachineTest + ApprovalStepStateMachineTest.

    // ─── AUTHZ family ────────────────────────────────────────────────────────

    @Test
    @Tag("WF-AUTHZ-001")
    void authz_001_unauthenticatedReturns401() {
        given().contentType(ContentType.JSON).body("{}")
        .when().post("/api/approvals").then().statusCode(401);

        given().when().get("/api/approvals").then().statusCode(401);
        given().when().get("/api/approvals/inbox").then().statusCode(401);
        given().when().get("/api/approvals/" + UUID.randomUUID()).then().statusCode(401);
    }

    @Test
    @Tag("WF-AUTHZ-002")
    void authz_002_crossUserGetReturns404() {
        String requesterToken = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("wfA2-req"), "MEMBER");
        String approverToken = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("wfA2-app"), "MEMBER");
        String approverId = ApprovalWorkflowTestSupport.resolveUserId(approverToken);
        String unrelatedToken = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("wfA2-other"), "MEMBER");

        UUID requestId = createDraftRequest(requesterToken, List.of(approverId));

        given()
            .header("Authorization", "Bearer " + unrelatedToken)
        .when().get("/api/approvals/" + requestId)
        .then().statusCode(404);

        // The unrelated user cannot cancel either.
        given()
            .header("Authorization", "Bearer " + unrelatedToken)
        .when().post("/api/approvals/" + requestId + "/cancel")
        .then().statusCode(404);
    }

    @Test
    @Tag("WF-AUTHZ-003")
    void authz_003_wrongApproverReturns403() {
        String requesterToken = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("wfA3-req"), "MEMBER");
        String app1Token = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("wfA3-a1"), "MEMBER");
        String app2Token = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("wfA3-a2"), "MEMBER");
        String app1Id = ApprovalWorkflowTestSupport.resolveUserId(app1Token);
        String app2Id = ApprovalWorkflowTestSupport.resolveUserId(app2Token);

        UUID requestId = createSubmittedRequest(requesterToken, List.of(app1Id, app2Id));
        List<String> stepIds = listStepIds(requesterToken, requestId);

        // app2 cannot act on step 0 (assigned to app1).
        given()
            .header("Authorization", "Bearer " + app2Token).contentType(ContentType.JSON).body("{}")
        .when().post("/api/approvals/" + requestId + "/steps/" + stepIds.get(0) + "/approve")
        .then().statusCode(403)
            .body("code", Matchers.equalTo("NOT_APPROVER"));
    }

    // ─── STEP family ─────────────────────────────────────────────────────────

    @Test
    @Tag("WF-STEP-001")
    void step_001_outOfOrderReturns409() {
        String requesterToken = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("wfS1-req"), "MEMBER");
        String app1Token = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("wfS1-a1"), "MEMBER");
        String app2Token = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("wfS1-a2"), "MEMBER");
        String app1Id = ApprovalWorkflowTestSupport.resolveUserId(app1Token);
        String app2Id = ApprovalWorkflowTestSupport.resolveUserId(app2Token);

        UUID requestId = createSubmittedRequest(requesterToken, List.of(app1Id, app2Id));
        List<String> stepIds = listStepIds(requesterToken, requestId);

        // app2 tries to skip step 0.
        given()
            .header("Authorization", "Bearer " + app2Token).contentType(ContentType.JSON).body("{}")
        .when().post("/api/approvals/" + requestId + "/steps/" + stepIds.get(1) + "/approve")
        .then().statusCode(409)
            .body("code", Matchers.equalTo("STEP_OUT_OF_ORDER"));
    }

    @Test
    @Tag("WF-STEP-002")
    void step_002_commentAndActorAndTimestampPersisted() {
        String requesterToken = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("wfS2-req"), "MEMBER");
        String app1Token = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("wfS2-a1"), "MEMBER");
        String app1Id = ApprovalWorkflowTestSupport.resolveUserId(app1Token);

        UUID requestId = createSubmittedRequest(requesterToken, List.of(app1Id));
        List<String> stepIds = listStepIds(requesterToken, requestId);

        given()
            .header("Authorization", "Bearer " + app1Token).contentType(ContentType.JSON)
            .body("{\"comment\":\"looks good to me\"}")
        .when().post("/api/approvals/" + requestId + "/steps/" + stepIds.get(0) + "/approve")
        .then().statusCode(200)
            .body("steps[0].comment", Matchers.equalTo("looks good to me"))
            .body("steps[0].actedByUserId", Matchers.equalTo(app1Id))
            .body("steps[0].actedAt", Matchers.notNullValue());
    }

    // WF-STEP-003 covered by ApprovalStepStateMachineTest.nullActor_isRejectedBeforeAnyMutation

    // ─── QUERY family ────────────────────────────────────────────────────────

    @Test
    @Tag("WF-QUERY-001")
    void query_001_inboxReturnsPendingStepsOldestFirst() {
        String app1Token = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("wfQ1-a1"), "MEMBER");
        String app1Id = ApprovalWorkflowTestSupport.resolveUserId(app1Token);
        String r1Token = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("wfQ1-r1"), "MEMBER");
        String r2Token = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("wfQ1-r2"), "MEMBER");

        UUID req1 = createSubmittedRequest(r1Token, List.of(app1Id));
        // Best-effort ordering — short sleep so the createdAt differs.
        sleepMs(20);
        UUID req2 = createSubmittedRequest(r2Token, List.of(app1Id));

        List<java.util.Map<String, Object>> items = given()
            .header("Authorization", "Bearer " + app1Token)
        .when().get("/api/approvals/inbox")
        .then().statusCode(200)
            .extract().path("items");

        assertThat(items).hasSize(2);
        assertThat(UUID.fromString(items.get(0).get("requestId").toString())).isEqualTo(req1);
        assertThat(UUID.fromString(items.get(1).get("requestId").toString())).isEqualTo(req2);
    }

    @Test
    @Tag("WF-QUERY-002")
    void query_002_listReturnsOnlyMyOwnRequests() {
        String reqAToken = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("wfQ2-a"), "MEMBER");
        String reqBToken = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("wfQ2-b"), "MEMBER");
        String reqAId = ApprovalWorkflowTestSupport.resolveUserId(reqAToken);
        String reqBId = ApprovalWorkflowTestSupport.resolveUserId(reqBToken);

        // A submits a request with B as approver.
        createSubmittedRequest(reqAToken, List.of(reqBId));

        // B lists — should NOT see A's request (B is approver, not requester).
        given()
            .header("Authorization", "Bearer " + reqBToken)
        .when().get("/api/approvals")
        .then().statusCode(200)
            .body("totalElements", Matchers.equalTo(0));

        // A lists — should see exactly 1.
        given()
            .header("Authorization", "Bearer " + reqAToken)
        .when().get("/api/approvals")
        .then().statusCode(200)
            .body("totalElements", Matchers.equalTo(1));

        // Sanity check the user ids — the test was meaningful only if they differ.
        assertThat(reqAId).isNotEqualTo(reqBId);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private UUID createDraftRequest(String requesterToken, List<String> approverIds) {
        StringBuilder body = new StringBuilder("{\"type\":\"budget\",\"approverUserIds\":[");
        for (int i = 0; i < approverIds.size(); i++) {
            if (i > 0) body.append(',');
            body.append('"').append(approverIds.get(i)).append('"');
        }
        body.append("]}");

        return UUID.fromString(given()
            .header("Authorization", "Bearer " + requesterToken)
            .contentType(ContentType.JSON)
            .body(body.toString())
        .when().post("/api/approvals")
        .then().statusCode(201)
            .extract().path("id"));
    }

    private UUID createSubmittedRequest(String requesterToken, List<String> approverIds) {
        UUID id = createDraftRequest(requesterToken, approverIds);
        given()
            .header("Authorization", "Bearer " + requesterToken)
        .when().post("/api/approvals/" + id + "/submit")
        .then().statusCode(200);
        return id;
    }

    private List<String> listStepIds(String requesterToken, UUID requestId) {
        List<java.util.Map<String, Object>> steps = given()
            .header("Authorization", "Bearer " + requesterToken)
        .when().get("/api/approvals/" + requestId)
        .then().statusCode(200)
            .extract().path("steps");
        return steps.stream().map(m -> m.get("id").toString()).toList();
    }

    private static void sleepMs(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    }
}
