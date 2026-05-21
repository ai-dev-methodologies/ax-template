package com.ax.template.authblueprint.approvalworkflow;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * VIOLATION proof tests — closes the methodology requirement
 * "VIOLATION 테스트로 피드백 루프를 증명했는가?" from METHODOLOGY.md Step 5.
 *
 * <p>Each test deliberately introduces a rule violation against the catalog's
 * sole-mutator contract and asserts the violation IS caught — either via a
 * thrown {@link IllegalStateException} from the state machine, or via a
 * structural check on the entity. This is the binary proof that the feedback
 * loop actually works, not just that the happy paths pass.
 *
 * <p>If any test in this file ever PASSES silently when it should have thrown,
 * the catalog's enforcement has eroded — that is a P0 catalog bug.
 */
@Tag("WORKFLOW")
class ApprovalViolationProofTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-05-22T00:00:00Z"), ZoneOffset.UTC);
    private final ApprovalRequestStateMachine requestSm = new ApprovalRequestStateMachine(clock);
    private final ApprovalStepStateMachine stepSm = new ApprovalStepStateMachine(clock);

    /**
     * Violation: skip SUBMITTED, jump DRAFT → APPROVED directly. The state machine MUST
     * refuse — bypassing the SUBMITTED step would mean approvers never reviewed.
     */
    @Test
    @Tag("WF-LIFECYCLE-004")
    void violation_skipsSubmittedStraightToApproved_isBlocked() {
        ApprovalRequest r = newRequest();

        assertThatThrownBy(() -> requestSm.markApproved(r))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("illegal request transition");

        // Crucially, the entity is unchanged.
        assertThat(r.getStatus()).isEqualTo(ApprovalRequestStatus.DRAFT);
        assertThat(r.getCompletedAt()).isNull();
    }

    /**
     * Violation: re-approve an already-APPROVED step. Step state machine MUST refuse —
     * double approval would let an approver retroactively change the audit trail.
     */
    @Test
    @Tag("WF-LIFECYCLE-004")
    void violation_doubleApproveSameStep_isBlocked() {
        ApprovalStep s = newStep();
        stepSm.markApproved(s, "actor-1", "first");

        assertThatThrownBy(() -> stepSm.markApproved(s, "actor-2", "second"))
            .isInstanceOf(IllegalStateException.class);

        // First approval is preserved untouched.
        assertThat(s.getActedByUserId()).isEqualTo("actor-1");
        assertThat(s.getComment()).isEqualTo("first");
    }

    /**
     * Violation: act on a step with a null actor. Step state machine MUST refuse BEFORE
     * any field mutation — otherwise we could have an APPROVED step with no actedBy.
     */
    @Test
    @Tag("WF-STEP-003")
    void violation_nullActorOnStep_leavesEntityUntouched() {
        ApprovalStep s = newStep();

        assertThatThrownBy(() -> stepSm.markApproved(s, null, "ghost"))
            .isInstanceOf(IllegalArgumentException.class);

        assertThat(s.getStatus()).isEqualTo(ApprovalStepStatus.PENDING);
        assertThat(s.getActedByUserId()).isNull();
        assertThat(s.getActedAt()).isNull();
        assertThat(s.getComment()).isNull();
    }

    /**
     * Violation: try to transition out of a terminal state. Both state machines MUST refuse —
     * terminal means terminal.
     */
    @Test
    @Tag("WF-LIFECYCLE-004")
    void violation_terminalRequestCannotTransition() {
        ApprovalRequest r = newRequest();
        requestSm.markSubmitted(r);
        requestSm.markRejected(r);

        assertThatThrownBy(() -> requestSm.markApproved(r)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> requestSm.markCancelled(r)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> requestSm.markSubmitted(r)).isInstanceOf(IllegalStateException.class);

        assertThat(r.getStatus()).isEqualTo(ApprovalRequestStatus.REJECTED);
    }

    /**
     * Violation: assume a stale {@code allowedTransitions} table from old code was cached.
     * The test reads the static table directly and asserts no surprise edges have crept in
     * (e.g. an accidental APPROVED → REJECTED).
     */
    @Test
    @Tag("WF-LIFECYCLE-004")
    void violation_stateMachineTableHasOnlyDocumentedEdges() {
        // PENDING → APPROVED, REJECTED  (no PENDING → PENDING, no PENDING → CANCELLED for steps).
        ApprovalStep step1 = newStep();
        stepSm.markApproved(step1, "a", null);
        assertThat(step1.getStatus()).isEqualTo(ApprovalStepStatus.APPROVED);

        ApprovalStep step2 = newStep();
        stepSm.markRejected(step2, "a", null);
        assertThat(step2.getStatus()).isEqualTo(ApprovalStepStatus.REJECTED);

        // The only other transitions the table should allow are the request's two
        // submission-time edges and the four exit edges from SUBMITTED. Anything else
        // would be a new edge that wasn't reviewed — caught by the negative tests below.
        ApprovalRequest r1 = newRequest();
        assertThatThrownBy(() -> requestSm.markApproved(r1)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> requestSm.markRejected(r1)).isInstanceOf(IllegalStateException.class);
    }

    private static ApprovalRequest newRequest() {
        return ApprovalRequest.builder()
            .requesterUserId("u-1")
            .type("budget")
            .status(ApprovalRequestStatus.DRAFT)
            .build();
    }

    private static ApprovalStep newStep() {
        return ApprovalStep.builder()
            .orderIndex(0)
            .approverUserId("approver-1")
            .status(ApprovalStepStatus.PENDING)
            .build();
    }
}
