package com.ax.template.authblueprint.dsr;

/**
 * Lifecycle of a single DSR request (DSR-SLA-001 tracking contract).
 *
 * <pre>
 *   RECEIVED    → IN_PROGRESS, CLOSED
 *   IN_PROGRESS → CLOSED
 *   CLOSED      → ∅ (terminal)
 * </pre>
 *
 * The {@code status} field is mutated ONLY by {@link DsrRequestStateMachine}.
 */
public enum DsrRequestStatus {
    RECEIVED,
    IN_PROGRESS,
    CLOSED
}
