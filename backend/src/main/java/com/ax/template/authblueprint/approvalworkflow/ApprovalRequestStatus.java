package com.ax.template.authblueprint.approvalworkflow;

/**
 * Lifecycle states for an {@link ApprovalRequest}.
 *
 * <p>Transitions (manifest {@code state_machines.request.transitions}):
 * <pre>
 *   DRAFT     → SUBMITTED, CANCELLED
 *   SUBMITTED → APPROVED, REJECTED, CANCELLED
 *   APPROVED / REJECTED / CANCELLED → (terminal)
 * </pre>
 *
 * <p>Sole mutator: {@link ApprovalRequestStateMachine}.
 */
public enum ApprovalRequestStatus {
    DRAFT,
    SUBMITTED,
    APPROVED,
    REJECTED,
    CANCELLED;

    public boolean isTerminal() {
        return this == APPROVED || this == REJECTED || this == CANCELLED;
    }
}
