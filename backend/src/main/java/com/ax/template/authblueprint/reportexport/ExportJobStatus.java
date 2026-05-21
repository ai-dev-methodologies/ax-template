package com.ax.template.authblueprint.reportexport;

/**
 * State machine values for an {@link ExportJob}.
 *
 * <p>Transitions (manifest {@code lifecycle.state_machine}):
 * <pre>
 *   PENDING   → RUNNING, CANCELLED
 *   RUNNING   → COMPLETED, FAILED
 *   COMPLETED → (terminal)
 *   FAILED    → (terminal)
 *   CANCELLED → (terminal)
 * </pre>
 *
 * <p>Sole mutator: {@link ExportJobStateMachine}. Trace: EXPORT-LIFECYCLE-004.
 */
public enum ExportJobStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }
}
