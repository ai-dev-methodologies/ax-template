package com.ax.template.authblueprint.approvalworkflow;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Sole mutator of {@link ApprovalRequest#getStatus()} +
 * {@code submittedAt} / {@code completedAt} timestamps.
 *
 * <p>Trace: WF-LIFECYCLE-001..004. Mirrors the {@code SubscriptionStateMachine} /
 * {@code OrderStateMachine} pattern adopted across the catalog.
 */
@Component
public class ApprovalRequestStateMachine {

    private static final Map<ApprovalRequestStatus, Set<ApprovalRequestStatus>> ALLOWED;
    static {
        ALLOWED = new EnumMap<>(ApprovalRequestStatus.class);
        ALLOWED.put(ApprovalRequestStatus.DRAFT,
            EnumSet.of(ApprovalRequestStatus.SUBMITTED, ApprovalRequestStatus.CANCELLED));
        ALLOWED.put(ApprovalRequestStatus.SUBMITTED,
            EnumSet.of(ApprovalRequestStatus.APPROVED, ApprovalRequestStatus.REJECTED, ApprovalRequestStatus.CANCELLED));
        ALLOWED.put(ApprovalRequestStatus.APPROVED, EnumSet.noneOf(ApprovalRequestStatus.class));
        ALLOWED.put(ApprovalRequestStatus.REJECTED, EnumSet.noneOf(ApprovalRequestStatus.class));
        ALLOWED.put(ApprovalRequestStatus.CANCELLED, EnumSet.noneOf(ApprovalRequestStatus.class));
    }

    private final Clock clock;

    public ApprovalRequestStateMachine(Clock clock) {
        this.clock = clock;
    }

    /** DRAFT → SUBMITTED. */
    public void markSubmitted(ApprovalRequest request) {
        assertTransition(request.getStatus(), ApprovalRequestStatus.SUBMITTED);
        request.setStatus(ApprovalRequestStatus.SUBMITTED);
        request.setSubmittedAt(Instant.now(clock));
    }

    /** SUBMITTED → APPROVED. */
    public void markApproved(ApprovalRequest request) {
        assertTransition(request.getStatus(), ApprovalRequestStatus.APPROVED);
        request.setStatus(ApprovalRequestStatus.APPROVED);
        request.setCompletedAt(Instant.now(clock));
    }

    /** SUBMITTED → REJECTED. */
    public void markRejected(ApprovalRequest request) {
        assertTransition(request.getStatus(), ApprovalRequestStatus.REJECTED);
        request.setStatus(ApprovalRequestStatus.REJECTED);
        request.setCompletedAt(Instant.now(clock));
    }

    /** DRAFT or SUBMITTED → CANCELLED. */
    public void markCancelled(ApprovalRequest request) {
        assertTransition(request.getStatus(), ApprovalRequestStatus.CANCELLED);
        request.setStatus(ApprovalRequestStatus.CANCELLED);
        request.setCompletedAt(Instant.now(clock));
    }

    private static void assertTransition(ApprovalRequestStatus from, ApprovalRequestStatus to) {
        Set<ApprovalRequestStatus> allowed = ALLOWED.getOrDefault(from, EnumSet.noneOf(ApprovalRequestStatus.class));
        if (!allowed.contains(to)) {
            throw new IllegalStateException(
                "illegal request transition: " + from + " → " + to + "; allowed = " + allowed);
        }
    }
}
