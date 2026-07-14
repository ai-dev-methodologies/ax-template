package com.ax.template.authblueprint.thresholdfiling;

import org.springframework.stereotype.Component;

/**
 * threshold-filing-obligation-l0 sole lifecycle mutator for the register. The ONLY transition is
 * the one-way crossing edge ACTIVE → TRIGGERED (TFO-TRIGGER-001), invoked by {@link FilingService}
 * inside the SAME transaction as the crossing accrual and the {@link FilingObligation} bind.
 * TRIGGERED has ZERO outgoing edges: no un-trigger/reset — a new reporting period is a NEW register.
 */
@Component
public class FilingRegisterStateMachine {

    /** ACTIVE → TRIGGERED. Idempotence is NOT offered — triggering a TRIGGERED register is a caller bug. */
    void trigger(FilingRegister register) {
        if (register.getStatus() != FilingRegisterStatus.ACTIVE) {
            throw new IllegalStateException(
                "TRIGGERED is terminal (zero outgoing edges) — cannot transition from " + register.getStatus());
        }
        register.markTriggered();
    }
}
