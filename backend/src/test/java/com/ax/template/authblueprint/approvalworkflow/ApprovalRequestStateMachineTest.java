package com.ax.template.authblueprint.approvalworkflow;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("WORKFLOW")
class ApprovalRequestStateMachineTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-05-22T00:00:00Z"), ZoneOffset.UTC);
    private final ApprovalRequestStateMachine sm = new ApprovalRequestStateMachine(clock);

    @Test
    @Tag("WF-LIFECYCLE-004")
    void draftToSubmitted_allowed() {
        ApprovalRequest r = newRequest();

        sm.markSubmitted(r);

        assertThat(r.getStatus()).isEqualTo(ApprovalRequestStatus.SUBMITTED);
        assertThat(r.getSubmittedAt()).isNotNull();
    }

    @Test
    @Tag("WF-LIFECYCLE-004")
    void submittedToApproved_allowed() {
        ApprovalRequest r = newRequest();
        sm.markSubmitted(r);

        sm.markApproved(r);

        assertThat(r.getStatus()).isEqualTo(ApprovalRequestStatus.APPROVED);
        assertThat(r.getCompletedAt()).isNotNull();
    }

    @Test
    @Tag("WF-LIFECYCLE-004")
    void submittedToRejected_allowed() {
        ApprovalRequest r = newRequest();
        sm.markSubmitted(r);

        sm.markRejected(r);

        assertThat(r.getStatus()).isEqualTo(ApprovalRequestStatus.REJECTED);
        assertThat(r.getCompletedAt()).isNotNull();
    }

    @Test
    @Tag("WF-LIFECYCLE-004")
    void draftToApproved_rejected() {
        ApprovalRequest r = newRequest();
        assertThatThrownBy(() -> sm.markApproved(r))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @Tag("WF-LIFECYCLE-004")
    void terminalToAnything_rejected() {
        ApprovalRequest r = newRequest();
        sm.markSubmitted(r);
        sm.markApproved(r);

        assertThatThrownBy(() -> sm.markSubmitted(r)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> sm.markRejected(r)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> sm.markCancelled(r)).isInstanceOf(IllegalStateException.class);
    }

    private static ApprovalRequest newRequest() {
        return ApprovalRequest.builder()
            .requesterUserId("u-1")
            .type("budget")
            .status(ApprovalRequestStatus.DRAFT)
            .build();
    }
}
