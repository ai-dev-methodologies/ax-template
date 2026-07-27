package com.ax.template.authblueprint.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

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
 * <h2>Honest scope — there is no BE-emitted action-set to assert against</h2>
 * {@link com.ax.template.authblueprint.approvalworkflow.ApprovalRequestResponse} and
 * {@link com.ax.template.authblueprint.approvalworkflow.ApprovalStepResponse} carry NO computed
 * permissions/action-set field. The catalog's real BE never tells a caller which of
 * {@code submit}/{@code cancel}/{@code approve}/{@code reject}/{@code view} it may invoke — that is a
 * genuine gap (recommend a BACKLOG entry; see the golden fixture's {@code _provenance} and this lane's
 * final report). Absent a BE-exposed decision function, {@link #computeBeAuthorizedActions} is a
 * REIMPLEMENTATION of the enforced guards, evidence-anchored by file:line citation on every branch
 * below and in the golden's per-row {@code _note}. It is NOT a call into
 * {@code ApprovalController}/{@code ApprovalService} (out of this lane's file set, and those guards are
 * side-effecting, not a queryable predicate) — EXCEPT for the two lifecycle transition checks, which DO
 * call the real {@link ApprovalRequestStateMachine} / {@link ApprovalStepStateMachine} instances
 * (production classes, not reimplemented ALLOWED maps): a change to either machine's transition table
 * flips this test RED without any edit here. The visibility / ownership / step-ordering conditions are
 * NOT wired to the real guard in the same way — a change to {@code ApprovalService}'s ordering or
 * ownership check would NOT automatically flip this test. That residual is the reason this lane
 * recommends the coverage-map cell stay {@code partial} rather than {@code covered}.
 *
 * <p>Mutation lock: hand-flip any one row's {@code expectedActions} in the golden (e.g. drop
 * {@code "approve"} from {@code approver_first_step_pending_submitted}) and this test goes RED because
 * {@link #computeBeAuthorizedActions} still independently derives the un-flipped set from the real
 * state machines + the cited guard conditions — it does not echo the golden back at itself.
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

    /**
     * REAL production call — {@link ApprovalRequestStateMachine#markSubmitted} /
     * {@link ApprovalRequestStateMachine#markCancelled} on a freshly-built probe request whose ONLY
     * populated field that matters is {@code status}. Mirrors {@code ApprovalRequestStateMachine.java:41-45}
     * / {@code :62-66}; the ALLOWED-transition map itself (lines 24-32) is private, so this calls the
     * public mutator and observes whether it throws — the actual runtime check, not a copy of it.
     */
    private static boolean canTransitionRequest(ApprovalRequestStatus from, ApprovalRequestStatus to) {
        ApprovalRequest probe = ApprovalRequest.builder()
                .requesterUserId("probe-requester")
                .type("PROBE")
                .status(from)
                .build();
        ApprovalRequestStateMachine machine = new ApprovalRequestStateMachine(FIXED_CLOCK);
        try {
            switch (to) {
                case SUBMITTED -> machine.markSubmitted(probe);
                case CANCELLED -> machine.markCancelled(probe);
                default -> throw new IllegalArgumentException("unsupported probe target " + to);
            }
            return true;
        } catch (IllegalStateException ex) {
            return false;
        }
    }

    /**
     * REAL production call — {@link ApprovalStepStateMachine#markApproved} /
     * {@link ApprovalStepStateMachine#markRejected} on a freshly-built probe step. Mirrors
     * {@code ApprovalStepStateMachine.java:38-43} / {@code :45-50}.
     */
    private static boolean canTransitionStep(ApprovalStepStatus from, ApprovalStepStatus to, String actorUserId) {
        ApprovalStep probe = ApprovalStep.builder()
                .orderIndex(0)
                .approverUserId(actorUserId)
                .status(from)
                .build();
        ApprovalStepStateMachine machine = new ApprovalStepStateMachine(FIXED_CLOCK);
        try {
            switch (to) {
                case APPROVED -> machine.markApproved(probe, actorUserId, null);
                case REJECTED -> machine.markRejected(probe, actorUserId, null);
                default -> throw new IllegalArgumentException("unsupported probe target " + to);
            }
            return true;
        } catch (IllegalStateException ex) {
            return false;
        }
    }

    /**
     * The reference derivation of "which actions is {@code callerId} authorized to invoke on this
     * request" — see class javadoc for the honest scoping of what is and is not a call into real
     * production guard code.
     */
    private static List<String> computeBeAuthorizedActions(JsonNode requestNode, String callerId) {
        String requesterUserId = requestNode.get("requesterUserId").asText();
        ApprovalRequestStatus status = ApprovalRequestStatus.valueOf(requestNode.get("status").asText());
        List<StepFixture> steps = new ArrayList<>();
        for (JsonNode s : requestNode.get("steps")) {
            steps.add(new StepFixture(
                    s.get("orderIndex").asInt(),
                    s.get("approverUserId").asText(),
                    ApprovalStepStatus.valueOf(s.get("status").asText())));
        }

        TreeSet<String> actions = new TreeSet<>();

        // ApprovalRequestRepository.java:39-42 — requester OR-arm is unconditional on status; the
        // approver OR-arm additionally requires status == SUBMITTED.
        boolean isRequester = callerId.equals(requesterUserId);
        boolean isApproverOnSomeStep = steps.stream().anyMatch(s -> callerId.equals(s.approverUserId()));
        boolean visible = isRequester || (isApproverOnSomeStep && status == ApprovalRequestStatus.SUBMITTED);
        if (visible) {
            actions.add("view");
        }

        // ApprovalService.java:245-251 (loadOwn) — submit/cancel are requester-only, then gated by the
        // REAL request state machine's allowed-transition set.
        if (isRequester) {
            if (canTransitionRequest(status, ApprovalRequestStatus.SUBMITTED)) {
                actions.add("submit");
            }
            if (canTransitionRequest(status, ApprovalRequestStatus.CANCELLED)) {
                actions.add("cancel");
            }
        }

        // ApprovalService.java:195-202 — request must be exactly SUBMITTED (isTerminal() OR
        // status != SUBMITTED both throw). ApprovalService.java:209-217 — caller must be the target
        // step's assigned approver. ApprovalService.java:219-227 — every earlier-orderIndex step must
        // already be APPROVED. Only then is the REAL step state machine consulted.
        if (status == ApprovalRequestStatus.SUBMITTED) {
            for (StepFixture target : steps) {
                if (!callerId.equals(target.approverUserId())) {
                    continue;
                }
                boolean earlierStepsClear = steps.stream()
                        .filter(s -> s.orderIndex() < target.orderIndex())
                        .allMatch(s -> s.status() == ApprovalStepStatus.APPROVED);
                if (!earlierStepsClear) {
                    continue;
                }
                if (canTransitionStep(target.status(), ApprovalStepStatus.APPROVED, callerId)) {
                    actions.add("approve");
                }
                if (canTransitionStep(target.status(), ApprovalStepStatus.REJECTED, callerId)) {
                    actions.add("reject");
                }
            }
        }

        return new ArrayList<>(actions);
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
            List<String> actual = computeBeAuthorizedActions(row.get("request"), row.get("callerId").asText());
            assertThat(actual)
                    .as("row '%s' — BE-authorized action set must match the golden's expectedActions", label)
                    .isEqualTo(expected);
        }
    }

    @Test
    void requesterOnDraftRequest_mayViewSubmitAndCancel_butNotApproveOrReject() throws IOException {
        JsonNode row = findRow(goldenRows(), "requester_draft_own_request");
        List<String> actual = computeBeAuthorizedActions(row.get("request"), row.get("callerId").asText());
        assertThat(actual).containsExactly("cancel", "submit", "view");
    }

    @Test
    void assignedFirstStepApprover_onSubmittedRequest_mayApproveOrReject_butNotSubmitOrCancel() throws IOException {
        JsonNode row = findRow(goldenRows(), "approver_first_step_pending_submitted");
        List<String> actual = computeBeAuthorizedActions(row.get("request"), row.get("callerId").asText());
        assertThat(actual).containsExactly("approve", "reject", "view");
    }

    @Test
    void nonParticipant_getsNoActionsAtAll_notEvenView() throws IOException {
        JsonNode row = findRow(goldenRows(), "non_participant_submitted");
        List<String> actual = computeBeAuthorizedActions(row.get("request"), row.get("callerId").asText());
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
        List<String> actual = computeBeAuthorizedActions(row.get("request"), row.get("callerId").asText());
        assertThat(actual).containsExactly("view");
    }

    /**
     * The second non-obvious finding this fixture pins: an approver who already acted loses even
     * VIEW visibility once the request leaves SUBMITTED, because findVisibleTo's approver OR-arm is
     * conditioned on status == SUBMITTED (ApprovalRequestRepository.java:39-42).
     */
    @Test
    void approverWhoActed_losesVisibility_onceRequestGoesTerminal() throws IOException {
        JsonNode row = findRow(goldenRows(), "approver_loses_visibility_after_request_terminal");
        List<String> actual = computeBeAuthorizedActions(row.get("request"), row.get("callerId").asText());
        assertThat(actual).isEmpty();
    }

    @Test
    void requester_keepsViewOnly_afterRequestGoesTerminal() throws IOException {
        JsonNode row = findRow(goldenRows(), "requester_own_request_after_terminal_approval");
        List<String> actual = computeBeAuthorizedActions(row.get("request"), row.get("callerId").asText());
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
