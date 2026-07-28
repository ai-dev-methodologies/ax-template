package com.ax.template.authblueprint.approvalworkflow;

import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * P2-38a <b>Step 0</b> — pre-refactor CHARACTERIZATION of the approval authz surface.
 *
 * <p>This class exists to make the extraction in P2-38a provably behaviour-preserving.
 * The earlier claim that the existing suite already pinned these responses was FALSE:
 * {@code ApprovalComplianceTest} asserts status codes and the {@code code} property, but
 * never the {@code detail} STRING. An extraction that changed a message byte would have
 * passed every gate. So before {@code ApprovalActionGuards} exists, this class pins the
 * EXACT detail text of:
 * <ol>
 *   <li>the terminal-request 409 ({@code request … is already CANCELLED});</li>
 *   <li>the not-SUBMITTED 409 ({@code request … is not SUBMITTED (current: DRAFT)});</li>
 *   <li>the approver-mismatch 403 — which is also the R83 iter1 F8 PII lock: the message
 *       MUST NOT name the rightful approver;</li>
 *   <li>the strict-ordering 409 ({@code earlier step (orderIndex=0) is PENDING; …});</li>
 *   <li>the IDOR-safe ownership 404 ({@code approval request not found: <id>}).</li>
 * </ol>
 *
 * <p>These assertions are FROZEN: P2-38a is required to land with zero edits to them.
 *
 * <p>Black-box RestAssured against the live HTTP surface (PRACTICES-TEST-001).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Tag("WORKFLOW")
class ApprovalAuthzCharacterizationTest {

    @LocalServerPort int port;

    @BeforeEach
    void setup() {
        ApprovalWorkflowTestSupport.useRandomPort(port);
    }

    // ── seeding helpers ─────────────────────────────────────────────────────

    private record Fixture(String requesterToken, String requesterId,
                           String requestId, List<String> stepIds) {}

    /** Creates a DRAFT request with the given approvers (in order). */
    private Fixture seedDraft(String prefix, List<String> approverIds) {
        String requesterToken = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail(prefix + "-requester"), "MEMBER");
        String requesterId = ApprovalWorkflowTestSupport.resolveUserId(requesterToken);

        var body = Map.of(
            "type", "EXPENSE",
            "title", "characterization " + prefix,
            "payload", Map.of("amount", 1000),
            "approverUserIds", approverIds);

        var res = given()
            .header("Authorization", "Bearer " + requesterToken)
            .contentType(ContentType.JSON)
            .body(body)
        .when().post("/api/approvals")
        .then().statusCode(201).extract();

        return new Fixture(requesterToken, requesterId,
            res.path("id"), res.path("steps.id"));
    }

    private void submit(String token, String requestId) {
        given().header("Authorization", "Bearer " + token)
            .when().post("/api/approvals/" + requestId + "/submit")
            .then().statusCode(200);
    }

    // ── 1 + 2. RequestTerminalException, both arms ──────────────────────────

    @Test
    @Tag("WF-AUTHZ-CHAR")
    @DisplayName("Step 0: acting on a CANCELLED request → 409 REQUEST_TERMINAL with the exact 'is already' detail")
    void terminalRequest_exactMessage() {
        String approverToken = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("char-term-approver"), "MEMBER");
        String approverId = ApprovalWorkflowTestSupport.resolveUserId(approverToken);
        Fixture f = seedDraft("char-term", List.of(approverId));
        submit(f.requesterToken(), f.requestId());

        given().header("Authorization", "Bearer " + f.requesterToken())
            .when().post("/api/approvals/" + f.requestId() + "/cancel")
            .then().statusCode(200);

        given().header("Authorization", "Bearer " + approverToken)
            .contentType(ContentType.JSON).body(Map.of("comment", "x"))
        .when().post("/api/approvals/" + f.requestId() + "/steps/" + f.stepIds().get(0) + "/approve")
        .then()
            .statusCode(409)
            .body("code", Matchers.equalTo("REQUEST_TERMINAL"))
            .body("detail", Matchers.equalTo(
                "request " + f.requestId() + " is already CANCELLED"));
    }

    @Test
    @Tag("WF-AUTHZ-CHAR")
    @DisplayName("Step 0: acting on a DRAFT request → 409 REQUEST_TERMINAL with the exact 'is not SUBMITTED' detail")
    void notSubmittedRequest_exactMessage() {
        String approverToken = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("char-draft-approver"), "MEMBER");
        String approverId = ApprovalWorkflowTestSupport.resolveUserId(approverToken);
        Fixture f = seedDraft("char-draft", List.of(approverId));

        given().header("Authorization", "Bearer " + approverToken)
            .contentType(ContentType.JSON).body(Map.of("comment", "x"))
        .when().post("/api/approvals/" + f.requestId() + "/steps/" + f.stepIds().get(0) + "/approve")
        .then()
            .statusCode(409)
            .body("code", Matchers.equalTo("REQUEST_TERMINAL"))
            .body("detail", Matchers.equalTo(
                "request " + f.requestId() + " is not SUBMITTED (current: DRAFT)"));
    }

    // ── 3. NotApproverException (R83 iter1 F8 PII lock) ─────────────────────

    @Test
    @Tag("WF-AUTHZ-CHAR")
    @DisplayName("Step 0: wrong approver → 403 NOT_APPROVER, exact detail, and the rightful approver is NEVER named")
    void approverMismatch_exactMessage_andNoApproverIdLeak() {
        String rightfulToken = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("char-rightful"), "MEMBER");
        String rightfulId = ApprovalWorkflowTestSupport.resolveUserId(rightfulToken);
        String intruderToken = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("char-intruder"), "MEMBER");

        Fixture f = seedDraft("char-mismatch", List.of(rightfulId));
        submit(f.requesterToken(), f.requestId());
        String stepId = f.stepIds().get(0);

        String bodyText = given().header("Authorization", "Bearer " + intruderToken)
            .contentType(ContentType.JSON).body(Map.of("comment", "x"))
        .when().post("/api/approvals/" + f.requestId() + "/steps/" + stepId + "/approve")
        .then()
            .statusCode(403)
            .body("code", Matchers.equalTo("NOT_APPROVER"))
            .body("detail", Matchers.equalTo("step " + stepId + " is not assigned to the caller"))
            .extract().body().asString();

        // R83 iter1 F8 — step ownership is policy-sensitive; the response must not
        // disclose WHO the assigned approver is to a caller who is not them.
        org.assertj.core.api.Assertions.assertThat(bodyText).doesNotContain(rightfulId);
    }

    // ── 4. StepOutOfOrderException ──────────────────────────────────────────

    @Test
    @Tag("WF-AUTHZ-CHAR")
    @DisplayName("Step 0: acting on step 2 before step 1 → 409 STEP_OUT_OF_ORDER with the exact detail")
    void stepOutOfOrder_exactMessage() {
        String firstToken = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("char-order-1"), "MEMBER");
        String secondToken = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("char-order-2"), "MEMBER");
        String firstId = ApprovalWorkflowTestSupport.resolveUserId(firstToken);
        String secondId = ApprovalWorkflowTestSupport.resolveUserId(secondToken);

        Fixture f = seedDraft("char-order", List.of(firstId, secondId));
        submit(f.requesterToken(), f.requestId());

        given().header("Authorization", "Bearer " + secondToken)
            .contentType(ContentType.JSON).body(Map.of("comment", "x"))
        .when().post("/api/approvals/" + f.requestId() + "/steps/" + f.stepIds().get(1) + "/approve")
        .then()
            .statusCode(409)
            .body("code", Matchers.equalTo("STEP_OUT_OF_ORDER"))
            .body("detail", Matchers.equalTo(
                "earlier step (orderIndex=0) is PENDING; expected APPROVED before acting on this step"));
    }

    // ── 5. loadOwn ownership (IDOR-safe 404) ────────────────────────────────

    @Test
    @Tag("WF-AUTHZ-CHAR")
    @DisplayName("Step 0: submit/cancel by a non-requester → 404 REQUEST_NOT_FOUND with the exact IDOR-safe detail")
    void ownershipViolation_exactMessage() {
        String approverToken = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("char-own-approver"), "MEMBER");
        String approverId = ApprovalWorkflowTestSupport.resolveUserId(approverToken);
        Fixture f = seedDraft("char-own", List.of(approverId));

        // the ASSIGNED APPROVER is not the requester: submit must 404, not 403,
        // so the existence of another user's DRAFT is never disclosed.
        given().header("Authorization", "Bearer " + approverToken)
            .when().post("/api/approvals/" + f.requestId() + "/submit")
            .then()
                .statusCode(404)
                .body("code", Matchers.equalTo("REQUEST_NOT_FOUND"))
                .body("detail", Matchers.equalTo("approval request not found: " + f.requestId()));

        given().header("Authorization", "Bearer " + approverToken)
            .when().post("/api/approvals/" + f.requestId() + "/cancel")
            .then()
                .statusCode(404)
                .body("code", Matchers.equalTo("REQUEST_NOT_FOUND"))
                .body("detail", Matchers.equalTo("approval request not found: " + f.requestId()));
    }
}
