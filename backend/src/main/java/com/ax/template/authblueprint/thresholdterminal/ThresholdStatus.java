package com.ax.template.authblueprint.thresholdterminal;

/**
 * threshold-terminal-derivation-l0 lifecycle. ACTIVE = accruals and uses are admitted. EXPIRED = the
 * anchor reached/crossed the limit — IRREVERSIBLE terminal (TTD-TERMINAL-001): zero outgoing edges, no
 * un-expire/reset/reactivate. A replacement asset is a NEW register with its own identity.
 */
public enum ThresholdStatus {
    ACTIVE,
    EXPIRED;

    boolean isTerminal() {
        return this == EXPIRED;
    }
}
