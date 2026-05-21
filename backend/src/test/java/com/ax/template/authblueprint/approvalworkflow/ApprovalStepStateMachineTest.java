package com.ax.template.authblueprint.approvalworkflow;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("WORKFLOW")
class ApprovalStepStateMachineTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-05-22T00:00:00Z"), ZoneOffset.UTC);
    private final ApprovalStepStateMachine sm = new ApprovalStepStateMachine(clock);

    @Test
    @Tag("WF-STEP-002")
    void pendingToApproved_recordsActorAndComment() {
        ApprovalStep step = newStep();

        sm.markApproved(step, "approver-1", "ok by me");

        assertThat(step.getStatus()).isEqualTo(ApprovalStepStatus.APPROVED);
        assertThat(step.getActedByUserId()).isEqualTo("approver-1");
        assertThat(step.getActedAt()).isNotNull();
        assertThat(step.getComment()).isEqualTo("ok by me");
    }

    @Test
    @Tag("WF-STEP-003")
    void nullActor_isRejectedBeforeAnyMutation() {
        ApprovalStep step = newStep();

        assertThatThrownBy(() -> sm.markApproved(step, null, "anything"))
            .isInstanceOf(IllegalArgumentException.class);

        assertThat(step.getStatus()).isEqualTo(ApprovalStepStatus.PENDING);
        assertThat(step.getActedByUserId()).isNull();
        assertThat(step.getActedAt()).isNull();
    }

    @Test
    @Tag("WF-LIFECYCLE-004")
    void approvedToApproved_rejected() {
        ApprovalStep step = newStep();
        sm.markApproved(step, "approver-1", null);

        assertThatThrownBy(() -> sm.markApproved(step, "approver-1", null))
            .isInstanceOf(IllegalStateException.class);
    }

    private static ApprovalStep newStep() {
        return ApprovalStep.builder()
            .orderIndex(0)
            .approverUserId("approver-1")
            .status(ApprovalStepStatus.PENDING)
            .build();
    }
}
