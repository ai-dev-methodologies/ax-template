package com.ax.template.authblueprint.thresholdterminal;

import org.springframework.stereotype.Component;

/**
 * threshold-terminal-derivation-l0 sole lifecycle mutator. The ONLY transition is the one-way crossing
 * edge ACTIVE → EXPIRED (TTD-CROSS-001), invoked by {@link ThresholdRegisterService} inside the SAME
 * transaction as the crossing accrual. EXPIRED has ZERO outgoing edges (TTD-TERMINAL-001): this class
 * deliberately defines no un-expire / reset / reactivate — a replacement asset is a NEW register.
 */
@Component
public class ThresholdRegisterStateMachine {

    /** ACTIVE → EXPIRED. Idempotence is NOT offered — expiring an EXPIRED register is a caller bug. */
    void expire(ThresholdRegister register) {
        if (register.getStatus() != ThresholdStatus.ACTIVE) {
            throw new IllegalStateException(
                "EXPIRED is terminal (zero outgoing edges) — cannot transition from " + register.getStatus());
        }
        register.markExpired();
    }
}
