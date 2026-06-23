package com.ax.template.authblueprint.mandate;

/**
 * mandate-fanout-l0 directive lifecycle. A mandate is ISSUED at fan-out and may be marked
 * SATISFIED once its check battery is fully PASSED (MANDATE-BATTERY-001). SATISFIED is terminal —
 * there is no un-satisfy edge. (Completion of the CHILD TASKS is a separate DERIVED recall, never
 * a stored status — see {@link MandateTaskState#isTerminal()} and the issuedCount conservation.)
 */
public enum MandateStatus {
    ISSUED,
    SATISFIED
}
