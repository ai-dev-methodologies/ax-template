package com.ax.template.authblueprint.approvalworkflow;

import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * P3-63 + P2-38a/b over the LIVE HTTP surface.
 *
 * <p>Two things are proven here that a unit-level test cannot:
 * <ol>
 *   <li><b>P3-63</b> — the narrow widening as an actual GET: an approver who ACTED can read
 *       the request after it terminates; a DRAFT assignee, an unacted later approver after
 *       a rejection, and an unrelated user all still get 404 (never 403 — the existence of
 *       another user's request is not disclosed).</li>
 *   <li><b>P2-38a real-selector contract</b> — the two LIST endpoints' repository queries
 *       agree with the shared predicates. rev-3 of the plan proposed a parity matrix against
 *       {@code findVisibleTo}; that query was ID-scoped, served only the single-GET, and is
 *       now DELETED, so a test bound to it would have protected an unused repository policy —
 *       the very defect class P2-38a closes. These tests bind to the selectors production
 *       actually calls: {@code findByRequesterUserIdOrderByCreatedAtDesc} (GET /approvals)
 *       ↔ {@code isRequester}, and {@code findInboxFor} (GET /approvals/inbox) ↔
 *       {@code isAssignedApprover ∧ isActionable}.</li>
 * </ol>
 *
 * <p>The {@code allowedActions} assertions are made against the LIVE response body, so the
 * wire really carries what {@link ApprovalActionEvaluator} computed (P2-38b).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Tag("WORKFLOW")
class ApprovalVisibilitySelectorContractTest {

    // P2-120: the field stays — com.ax.template.authblueprint.common.AxPort reads it by
    // reflection before every test and is the single writer of RestAssured.port. The manual
    // publish that used to live in a per-test setup method here is gone.
    @LocalServerPort int port;

    private record Actor(String token, String id) {}

    private Actor actor(String prefix) {
        String token = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail(prefix), "MEMBER");
        return new Actor(token, ApprovalWorkflowTestSupport.resolveUserId(token));
    }

    private record Created(String requestId, List<String> stepIds) {}

    private Created createDraft(Actor requester, List<String> approverIds, String title) {
        var res = given()
            .header("Authorization", "Bearer " + requester.token())
            .contentType(ContentType.JSON)
            .body(Map.of("type", "EXPENSE", "title", title,
                         "payload", Map.of("amount", 1000),
                         "approverUserIds", approverIds))
        .when().post("/api/approvals")
        .then().statusCode(201).extract();
        return new Created(res.path("id"), res.path("steps.id"));
    }

    private void submit(Actor requester, String requestId) {
        given().header("Authorization", "Bearer " + requester.token())
            .when().post("/api/approvals/" + requestId + "/submit")
            .then().statusCode(200);
    }

    private void act(Actor approver, String requestId, String stepId, boolean approve) {
        given().header("Authorization", "Bearer " + approver.token())
            .contentType(ContentType.JSON).body(Map.of("comment", "ok"))
        .when().post("/api/approvals/" + requestId + "/steps/" + stepId
                     + (approve ? "/approve" : "/reject"))
        .then().statusCode(200);
    }

    // ── P3-63 positive ──────────────────────────────────────────────────────

    @Test
    @Tag("WF-AUTHZ-002")
    @DisplayName("P3-63: an approver who ACTED can still GET the request after it goes terminal (view-only)")
    void actedApprover_canReadRequest_afterTerminal() {
        Actor requester = actor("p363-requester");
        Actor approver = actor("p363-approver");
        Created c = createDraft(requester, List.of(approver.id()), "p363 acted");
        submit(requester, c.requestId());
        act(approver, c.requestId(), c.stepIds().get(0), true);   // → APPROVED (terminal)

        given().header("Authorization", "Bearer " + approver.token())
            .when().get("/api/approvals/" + c.requestId())
            .then()
                .statusCode(200)
                .body("status", Matchers.equalTo("APPROVED"))
                // P2-38b — the wire carries the server's own answer, and for a terminal
                // request the acted approver may only look.
                .body("allowedActions", Matchers.contains("view"));
    }

    // ── P3-63 negatives (three) ─────────────────────────────────────────────

    @Test
    @Tag("WF-AUTHZ-002")
    @DisplayName("P3-63 negative 1: a DRAFT is 404 for its assigned approver — it has not been sent yet")
    void draftAssignee_gets404() {
        Actor requester = actor("p363-draft-requester");
        Actor approver = actor("p363-draft-approver");
        Created c = createDraft(requester, List.of(approver.id()), "p363 draft");

        given().header("Authorization", "Bearer " + approver.token())
            .when().get("/api/approvals/" + c.requestId())
            .then().statusCode(404).body("code", Matchers.equalTo("REQUEST_NOT_FOUND"));
    }

    @Test
    @Tag("WF-AUTHZ-002")
    @DisplayName("P3-63 negative 2: an UNACTED later approver gets 404 after an upstream rejection ends the chain")
    void unactedLaterApprover_gets404_afterRejection() {
        Actor requester = actor("p363-rej-requester");
        Actor first = actor("p363-rej-first");
        Actor second = actor("p363-rej-second");
        Created c = createDraft(requester, List.of(first.id(), second.id()), "p363 rejection");
        submit(requester, c.requestId());
        act(first, c.requestId(), c.stepIds().get(0), false);   // → REJECTED (terminal)

        // the approver who ACTED keeps view …
        given().header("Authorization", "Bearer " + first.token())
            .when().get("/api/approvals/" + c.requestId())
            .then().statusCode(200).body("allowedActions", Matchers.contains("view"));

        // … the one who was never reached does NOT
        given().header("Authorization", "Bearer " + second.token())
            .when().get("/api/approvals/" + c.requestId())
            .then().statusCode(404).body("code", Matchers.equalTo("REQUEST_NOT_FOUND"));
    }

    @Test
    @Tag("WF-AUTHZ-002")
    @DisplayName("P3-63 negative 3: an unrelated user gets 404 at every lifecycle stage")
    void unrelatedUser_gets404_everywhere() {
        Actor requester = actor("p363-unrel-requester");
        Actor approver = actor("p363-unrel-approver");
        Actor stranger = actor("p363-unrel-stranger");
        Created c = createDraft(requester, List.of(approver.id()), "p363 unrelated");

        given().header("Authorization", "Bearer " + stranger.token())
            .when().get("/api/approvals/" + c.requestId()).then().statusCode(404);

        submit(requester, c.requestId());
        given().header("Authorization", "Bearer " + stranger.token())
            .when().get("/api/approvals/" + c.requestId()).then().statusCode(404);

        act(approver, c.requestId(), c.stepIds().get(0), true);
        given().header("Authorization", "Bearer " + stranger.token())
            .when().get("/api/approvals/" + c.requestId()).then().statusCode(404);
    }

    // ── P3-76: the step-scoped action set, on the LIVE wire ─────────────────

    @Test
    @Tag("WF-AUTHZ-003")
    @Tag("WF-STEP-001")
    @DisplayName("P3-76: steps[].allowedActions identifies WHICH step is the caller's, and advances with the 결재선")
    void stepScopedAllowedActions_trackTheChain() {
        Actor requester = actor("p376-requester");
        Actor first = actor("p376-first");
        Actor second = actor("p376-second");
        Created c = createDraft(requester, List.of(first.id(), second.id()), "p376 chain");
        submit(requester, c.requestId());

        // The later approver is VISIBLE (SUBMITTED + assigned) but her step is not hers to
        // act on yet — the request-scoped array cannot express that, the step-scoped one can.
        given().header("Authorization", "Bearer " + second.token())
            .when().get("/api/approvals/" + c.requestId())
            .then()
                .statusCode(200)
                .body("allowedActions", Matchers.contains("view"))
                .body("steps[0].allowedActions", Matchers.empty())
                .body("steps[1].allowedActions", Matchers.empty());

        // … while for the FIRST approver, step 0 is open and step 1 is not hers at all.
        given().header("Authorization", "Bearer " + first.token())
            .when().get("/api/approvals/" + c.requestId())
            .then()
                .statusCode(200)
                .body("steps[0].allowedActions",
                      Matchers.containsInAnyOrder("approve", "reject"))
                .body("steps[1].allowedActions", Matchers.empty());

        act(first, c.requestId(), c.stepIds().get(0), true);

        // The chain advanced: now the SECOND step is the actionable one — the case a
        // client cannot get right from the request-scoped array alone.
        given().header("Authorization", "Bearer " + second.token())
            .when().get("/api/approvals/" + c.requestId())
            .then()
                .statusCode(200)
                .body("steps[0].allowedActions", Matchers.empty())
                .body("steps[1].allowedActions",
                      Matchers.containsInAnyOrder("approve", "reject"));
    }

    // ── P2-38a real-selector contract: GET /approvals ↔ isRequester ─────────

    @Test
    @Tag("WF-QUERY-001")
    @DisplayName("P2-38a: every row of GET /approvals satisfies isRequester — a request the caller only approves is absent")
    void requesterList_containsExactlyTheCallersOwnRequests() {
        Actor alice = actor("sel-alice");
        Actor bob = actor("sel-bob");

        Created mine = createDraft(alice, List.of(bob.id()), "sel alice own");
        // a request alice is an APPROVER on, raised by bob — visible to her via canView
        // once submitted, but it is NOT hers, so the requester list must not contain it.
        Created theirs = createDraft(bob, List.of(alice.id()), "sel bob own");
        submit(bob, theirs.requestId());

        var rows = given().header("Authorization", "Bearer " + alice.token())
            .when().get("/api/approvals")
            .then().statusCode(200).extract();

        List<String> ids = rows.path("items.id");
        List<String> requesters = rows.path("items.requesterUserId");

        assertThat(ids).contains(mine.requestId());
        assertThat(ids)
            .as("a request the caller merely approves must NOT appear in the requester list")
            .doesNotContain(theirs.requestId());
        assertThat(requesters)
            .as("isRequester must hold for EVERY row the selector returns")
            .allMatch(alice.id()::equals);

        // and the one she can see as an approver IS reachable via the single-GET —
        // proving the exclusion above is the selector's doing, not a visibility failure.
        given().header("Authorization", "Bearer " + alice.token())
            .when().get("/api/approvals/" + theirs.requestId())
            .then().statusCode(200);
    }

    // ── P2-38a real-selector contract: GET /approvals/inbox ────────────────

    @Test
    @Tag("WF-QUERY-002")
    @DisplayName("P2-38a: every inbox entry satisfies isAssignedApprover ∧ isActionable — acted and non-SUBMITTED are absent")
    void inbox_containsOnlyPendingStepsOnSubmittedRequests() {
        Actor requester = actor("inbox-requester");
        Actor approver = actor("inbox-approver");

        Created pending = createDraft(requester, List.of(approver.id()), "inbox pending");
        submit(requester, pending.requestId());

        Created draft = createDraft(requester, List.of(approver.id()), "inbox draft");   // never submitted

        Created acted = createDraft(requester, List.of(approver.id()), "inbox acted");
        submit(requester, acted.requestId());
        act(approver, acted.requestId(), acted.stepIds().get(0), true);   // step no longer PENDING

        var res = given().header("Authorization", "Bearer " + approver.token())
            .when().get("/api/approvals/inbox")
            .then().statusCode(200).extract();

        List<String> requestIds = res.path("items.requestId");
        List<String> stepStatuses = res.path("items.status");

        assertThat(requestIds)
            .as("a PENDING step on a SUBMITTED request IS in the inbox")
            .contains(pending.requestId());
        assertThat(requestIds)
            .as("isActionable fails for a DRAFT request — its step must not be in the inbox")
            .doesNotContain(draft.requestId());
        assertThat(requestIds)
            .as("an already-acted step must not be in the inbox")
            .doesNotContain(acted.requestId());
        assertThat(stepStatuses)
            .as("every inbox entry is a PENDING step")
            .allMatch("PENDING"::equals);

        // an unrelated approver's inbox never contains this requester's steps
        Actor stranger = actor("inbox-stranger");
        List<String> strangerIds = given().header("Authorization", "Bearer " + stranger.token())
            .when().get("/api/approvals/inbox")
            .then().statusCode(200).extract().path("items.requestId");
        assertThat(strangerIds)
            .as("isAssignedApprover must hold for EVERY row the inbox selector returns")
            .doesNotContain(pending.requestId());
    }

    // ── P2-38b: allowedActions on the wire matches the golden's semantics ───

    @Test
    @Tag("WF-AUTHZ-003")
    @DisplayName("P2-38b: the live response's allowedActions tracks the caller and the lifecycle stage")
    void allowedActions_onTheWire_trackCallerAndLifecycle() {
        Actor requester = actor("aa-requester");
        Actor first = actor("aa-first");
        Actor second = actor("aa-second");
        Created c = createDraft(requester, List.of(first.id(), second.id()), "aa chain");

        // DRAFT, requester: may submit or cancel, may not approve
        given().header("Authorization", "Bearer " + requester.token())
            .when().get("/api/approvals/" + c.requestId())
            .then().statusCode(200)
            .body("allowedActions", Matchers.containsInAnyOrder("cancel", "submit", "view"));

        submit(requester, c.requestId());

        // SUBMITTED, first approver: actionable
        given().header("Authorization", "Bearer " + first.token())
            .when().get("/api/approvals/" + c.requestId())
            .then().statusCode(200)
            .body("allowedActions", Matchers.containsInAnyOrder("approve", "reject", "view"));

        // SUBMITTED, second approver: visible but blocked by strict ordering —
        // the exact parity trap the golden's out-of-order row pins.
        given().header("Authorization", "Bearer " + second.token())
            .when().get("/api/approvals/" + c.requestId())
            .then().statusCode(200)
            .body("allowedActions", Matchers.contains("view"));

        // SUBMITTED, requester: may cancel but not approve their own chain
        given().header("Authorization", "Bearer " + requester.token())
            .when().get("/api/approvals/" + c.requestId())
            .then().statusCode(200)
            .body("allowedActions", Matchers.containsInAnyOrder("cancel", "view"));
    }
}
