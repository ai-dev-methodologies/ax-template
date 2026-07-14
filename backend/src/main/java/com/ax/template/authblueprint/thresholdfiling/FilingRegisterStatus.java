package com.ax.template.authblueprint.thresholdfiling;

/**
 * threshold-filing-obligation-l0 register lifecycle (TFO-TRIGGER-001). ACTIVE = accruals are
 * admitted. TRIGGERED = the accrued value reached/crossed the threshold — IRREVERSIBLE: zero
 * outgoing edges, no un-trigger/reset. A new reporting period is a NEW register with its own
 * identity (mirrors ThresholdStatus in thresholdterminal).
 */
public enum FilingRegisterStatus {
    ACTIVE,
    TRIGGERED;

    boolean isTerminal() {
        return this == TRIGGERED;
    }
}
