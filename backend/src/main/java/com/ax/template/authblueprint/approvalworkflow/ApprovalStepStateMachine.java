package com.ax.template.authblueprint.approvalworkflow;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Sole mutator of {@link ApprovalStep#getStatus()} + {@code actedByUserId} /
 * {@code actedAt} / {@code comment}.
 *
 * <p>Trace: WF-LIFECYCLE-004, WF-STEP-002, WF-STEP-003. Records actor + timestamp
 * atomically with the status change so no observable state exists where status is
 * terminal but actedBy is null.
 */
@Component
public class ApprovalStepStateMachine {

    private static final Map<ApprovalStepStatus, Set<ApprovalStepStatus>> ALLOWED;
    static {
        ALLOWED = new EnumMap<>(ApprovalStepStatus.class);
        ALLOWED.put(ApprovalStepStatus.PENDING,
            EnumSet.of(ApprovalStepStatus.APPROVED, ApprovalStepStatus.REJECTED));
        ALLOWED.put(ApprovalStepStatus.APPROVED, EnumSet.noneOf(ApprovalStepStatus.class));
        ALLOWED.put(ApprovalStepStatus.REJECTED, EnumSet.noneOf(ApprovalStepStatus.class));
    }

    private final Clock clock;

    public ApprovalStepStateMachine(Clock clock) {
        this.clock = clock;
    }

    /** PENDING → APPROVED — records actor + timestamp + optional comment atomically. */
    public void markApproved(ApprovalStep step, String actorUserId, String comment) {
        assertActor(actorUserId);
        assertTransition(step.getStatus(), ApprovalStepStatus.APPROVED);
        applyAction(step, ApprovalStepStatus.APPROVED, actorUserId, comment);
    }

    /** PENDING → REJECTED — records actor + timestamp + optional comment atomically. */
    public void markRejected(ApprovalStep step, String actorUserId, String comment) {
        assertActor(actorUserId);
        assertTransition(step.getStatus(), ApprovalStepStatus.REJECTED);
        applyAction(step, ApprovalStepStatus.REJECTED, actorUserId, comment);
    }

    private void applyAction(ApprovalStep step,
                             ApprovalStepStatus next,
                             String actorUserId,
                             String comment) {
        step.setStatus(next);
        step.setActedByUserId(actorUserId);
        step.setActedAt(Instant.now(clock));
        step.setComment(comment);
    }

    private static void assertActor(String actorUserId) {
        if (actorUserId == null || actorUserId.isBlank()) {
            throw new IllegalArgumentException("actor userId is required for a step action");
        }
    }

    private static void assertTransition(ApprovalStepStatus from, ApprovalStepStatus to) {
        Set<ApprovalStepStatus> allowed = ALLOWED.getOrDefault(from, EnumSet.noneOf(ApprovalStepStatus.class));
        if (!allowed.contains(to)) {
            throw new IllegalStateException(
                "illegal step transition: " + from + " → " + to + "; allowed = " + allowed);
        }
    }
}
