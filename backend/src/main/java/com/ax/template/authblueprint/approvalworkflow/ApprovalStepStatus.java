package com.ax.template.authblueprint.approvalworkflow;

/**
 * Lifecycle states for an {@link ApprovalStep}.
 *
 * <p>Transitions (manifest {@code state_machines.step.transitions}):
 * <pre>
 *   PENDING  → APPROVED, REJECTED
 *   APPROVED / REJECTED → (terminal)
 * </pre>
 *
 * <p>Sole mutator: {@link ApprovalStepStateMachine}.
 */
public enum ApprovalStepStatus {
    PENDING,
    APPROVED,
    REJECTED;

    public boolean isTerminal() {
        return this == APPROVED || this == REJECTED;
    }
}
