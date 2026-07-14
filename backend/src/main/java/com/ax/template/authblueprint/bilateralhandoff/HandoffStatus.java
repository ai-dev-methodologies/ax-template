package com.ax.template.authblueprint.bilateralhandoff;

/**
 * bilateral-handoff-l0 lifecycle (BHO-FSM-001): PROPOSED → one of two terminal outcomes. PROPOSED
 * is the only non-terminal state. COMPLETED means both named parties independently confirmed (the
 * custody flip effect applied exactly once); VOIDED means either party declined — terminal
 * regardless of any prior partial confirmation (BHO-VOID-001).
 */
public enum HandoffStatus {
    PROPOSED,
    COMPLETED,
    VOIDED
}
