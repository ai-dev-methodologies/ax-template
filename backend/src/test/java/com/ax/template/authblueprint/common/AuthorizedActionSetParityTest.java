package com.ax.template.authblueprint.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import com.ax.template.authblueprint.approvalworkflow.ApprovalActionEvaluator;
import com.ax.template.authblueprint.approvalworkflow.ApprovalActionGuards;
import com.ax.template.authblueprint.approvalworkflow.ApprovalAggregateFixtures;
import com.ax.template.authblueprint.approvalworkflow.ApprovalRequest;
import com.ax.template.authblueprint.approvalworkflow.ApprovalRequestStateMachine;
import com.ax.template.authblueprint.approvalworkflow.ApprovalRequestStatus;
import com.ax.template.authblueprint.approvalworkflow.ApprovalStep;
import com.ax.template.authblueprint.approvalworkflow.ApprovalStepStateMachine;
import com.ax.template.authblueprint.approvalworkflow.ApprovalStepStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * S2.AUTHZ.XB — cross-boundary parity: the FE must not offer an approval-workflow
 * (전자결재) action the BE would reject, and must not hide one the BE allows.
 *
 * <p>Sibling of {@code common.PageEnvelopeContractParityTest} / {@code payment.MoneyContractParityTest}
 * (same pattern: one committed golden, two independent consumers — this Jackson test +
 * {@code frontend/tests/authz-action-parity.vitest.ts} both read
 * {@code frontend/tests/_fixtures/authorized-actions.golden.json}). Plain Jackson unit test — NO
 * {@code @SpringBootTest}, zero ContextCache pressure.
 *
 * <h2>Scope — this now CALLS the production decision function</h2>
 * When this test was written the BE emitted no action-set at all, so
 * {@code computeBeAuthorizedActions} was an honest-but-fragile REIMPLEMENTATION of the
 * enforced guards, anchored only by file:line comments. P2-38a/b closed that gap:
 * {@link ApprovalActionEvaluator} computes the set server-side, delegating every branch to
 * {@link ApprovalActionGuards} — the SAME predicates {@code ApprovalService} enforces on
 * the action path — plus the two real state machines' transition tables.
 *
 * <p>This test therefore no longer re-derives anything. It builds a probe aggregate from
 * each golden row and asks the REAL evaluator. Consequences, and the reason the residual
 * that kept the coverage cell {@code partial} is gone:
 * <ul>
 *   <li>neutering {@code isNextActionableStep} flips this test AND the action-path
 *       STEP_OUT_OF_ORDER test — one edit, two independent surfaces;</li>
 *   <li>same for {@code isRequester} (here + the ownership 404 + the requester-list
 *       contract test) and {@code canView} (here + single-GET visibility);</li>
 *   <li>a change to either state machine's ALLOWED map still flips this test, as before.</li>
 * </ul>
 *
 * <p>Mutation lock: hand-flip any one row's {@code expectedActions} in the golden and this
 * test goes RED, because the evaluator derives the set from production code and does not
 * echo the golden back at itself.
 */
// WORKFLOW is the approval-workflow domain's per-domain-task tag (matches every other
// class in backend/src/test/java/.../approvalworkflow/*, e.g. ApprovalComplianceTest) —
// binding this cross-boundary test to `./gradlew testApprovalWorkflow` even though the
// file itself lives in `common` alongside its PageEnvelope/Money contract-parity siblings.
@Tag("WORKFLOW")
@Tag("AUTHZ-XB")
@Tag("WF-AUTHZ-001")
@Tag("WF-AUTHZ-002")
@Tag("WF-AUTHZ-003")
@Tag("WF-STEP-001")
class AuthorizedActionSetParityTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private record StepFixture(int orderIndex, String approverUserId, ApprovalStepStatus status) {}

    private static JsonNode goldenRows() throws IOException {
        Path golden = Path.of(System.getProperty("user.dir"), "..", "frontend", "tests",
                "_fixtures", "authorized-actions.golden.json");
        return MAPPER.readTree(Files.readString(golden)).get("rows");
    }

    private static final ApprovalActionGuards GUARDS = new ApprovalActionGuards();
    private static final ApprovalActionEvaluator EVALUATOR = new ApprovalActionEvaluator(
            GUARDS,
            new ApprovalRequestStateMachine(FIXED_CLOCK),
            new ApprovalStepStateMachine(FIXED_CLOCK));

    /**
     * Builds the golden row's aggregate and asks the PRODUCTION
     * {@link ApprovalActionEvaluator}. No rule is expressed in this file.
     */
    private static List<String> beAuthorizedActions(JsonNode requestNode, String callerId) {
        List<ApprovalAggregateFixtures.StepSpec> steps = new ArrayList<>();
        for (JsonNode s : requestNode.get("steps")) {
            JsonNode acted = s.get("actedByUserId");
            steps.add(new ApprovalAggregateFixtures.StepSpec(
                    UUID.fromString(s.get("id").asText()),
                    s.get("orderIndex").asInt(),
                    s.get("approverUserId").asText(),
                    ApprovalStepStatus.valueOf(s.get("status").asText()),
                    acted == null || acted.isNull() ? null : acted.asText()));
        }
        ApprovalRequest request = ApprovalAggregateFixtures.build(
                requestNode.get("requesterUserId").asText(),
                ApprovalRequestStatus.valueOf(requestNode.get("status").asText()),
                steps);
        return EVALUATOR.allowedActions(request, callerId);
    }

    private static List<String> expectedActionsOf(JsonNode row) {
        List<String> expected = new ArrayList<>();
        row.get("expectedActions").forEach(n -> expected.add(n.asText()));
        return expected;
    }

    @Test
    void authorizedActionSet_matchesGoldenFixtureForEveryRow() throws IOException {
        JsonNode rows = goldenRows();
        assertThat(rows.isArray()).as("golden fixture must have a rows array").isTrue();
        assertThat(rows.size()).as("golden fixture must not be empty").isPositive();

        for (JsonNode row : rows) {
            String label = row.get("label").asText();
            List<String> expected = expectedActionsOf(row);
            List<String> actual = beAuthorizedActions(row.get("request"), row.get("callerId").asText());
            assertThat(actual)
                    .as("row '%s' — BE-authorized action set must match the golden's expectedActions", label)
                    .isEqualTo(expected);
        }
    }

    @Test
    void requesterOnDraftRequest_mayViewSubmitAndCancel_butNotApproveOrReject() throws IOException {
        JsonNode row = findRow(goldenRows(), "requester_draft_own_request");
        List<String> actual = beAuthorizedActions(row.get("request"), row.get("callerId").asText());
        assertThat(actual).containsExactly("cancel", "submit", "view");
    }

    @Test
    void assignedFirstStepApprover_onSubmittedRequest_mayApproveOrReject_butNotSubmitOrCancel() throws IOException {
        JsonNode row = findRow(goldenRows(), "approver_first_step_pending_submitted");
        List<String> actual = beAuthorizedActions(row.get("request"), row.get("callerId").asText());
        assertThat(actual).containsExactly("approve", "reject", "view");
    }

    @Test
    void nonParticipant_getsNoActionsAtAll_notEvenView() throws IOException {
        JsonNode row = findRow(goldenRows(), "non_participant_submitted");
        List<String> actual = beAuthorizedActions(row.get("request"), row.get("callerId").asText());
        assertThat(actual).isEmpty();
    }

    /**
     * The key parity trap: an approver assigned to a LATER step is visible on a SUBMITTED request
     * (findVisibleTo does not check ordering) but is NOT actionable until every earlier step is
     * APPROVED (ApprovalService's strict-ordering guard). A FE that renders "approve" merely because
     * the caller's id appears in {@code steps[].approverUserId} — without also checking ordering —
     * would offer an action the BE rejects with 409 STEP_OUT_OF_ORDER.
     */
    @Test
    void secondStepApprover_isVisibleButNotActionable_whileEarlierStepIsStillPending() throws IOException {
        JsonNode row = findRow(goldenRows(), "second_step_approver_visible_but_out_of_order");
        List<String> actual = beAuthorizedActions(row.get("request"), row.get("callerId").asText());
        assertThat(actual).containsExactly("view");
    }

    /**
     * P3-63 positive — an approver who ACTED keeps view once the request goes terminal, so they
     * can see the outcome of their own decision. Still view-only (terminal ⇒ no transitions, and
     * they are not the requester).
     */
    @Test
    void actedApprover_keepsViewOnly_afterRequestGoesTerminal() throws IOException {
        JsonNode row = findRow(goldenRows(), "acted_approver_keeps_view_after_terminal");
        List<String> actual = beAuthorizedActions(row.get("request"), row.get("callerId").asText());
        assertThat(actual).containsExactly("view");
    }

    /**
     * P3-63 negative #1 — the qualifier that keeps the widening narrow. An assigned approver who
     * was never reached (an upstream rejection ended the chain first) gets NOTHING: a rejection
     * must not retroactively disclose the request to approvers who never decided.
     */
    @Test
    void unactedApprover_getsNothing_afterRequestGoesTerminal() throws IOException {
        JsonNode row = findRow(goldenRows(), "unacted_approver_no_view_after_terminal");
        List<String> actual = beAuthorizedActions(row.get("request"), row.get("callerId").asText());
        assertThat(actual).isEmpty();
    }

    /** P3-63 negative #2 — a DRAFT is not visible to its named approvers; it has not been sent. */
    @Test
    void assignedApprover_getsNothing_whileRequestIsDraft() throws IOException {
        JsonNode row = findRow(goldenRows(), "assigned_approver_no_view_on_draft");
        List<String> actual = beAuthorizedActions(row.get("request"), row.get("callerId").asText());
        assertThat(actual).isEmpty();
    }

    @Test
    void requester_keepsViewOnly_afterRequestGoesTerminal() throws IOException {
        JsonNode row = findRow(goldenRows(), "requester_own_request_after_terminal_approval");
        List<String> actual = beAuthorizedActions(row.get("request"), row.get("callerId").asText());
        assertThat(actual).containsExactly("view");
    }

    private static JsonNode findRow(JsonNode rows, String label) {
        for (JsonNode row : rows) {
            if (label.equals(row.get("label").asText())) {
                return row;
            }
        }
        throw new AssertionError("golden fixture row not found: " + label);
    }
}
