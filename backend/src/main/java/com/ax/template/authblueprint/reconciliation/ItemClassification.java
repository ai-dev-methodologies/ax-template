package com.ax.template.authblueprint.reconciliation;

import java.math.BigDecimal;

/**
 * external-reconciliation-l0 classification verdict for one internal/external pair
 * (RECON-CLASSIFY-001). Computed DETERMINISTICALLY from key membership and amount comparison:
 * MATCHED (key both sides + amounts equal), BREAK (key both sides + amounts differ — the delta
 * is recorded), INTERNAL_ONLY (key internal, absent externally), EXTERNAL_ONLY (key external,
 * absent internally). Only a BREAK requires a human disposition (RECON-DISPOSE-001).
 */
public enum ItemClassification {
    MATCHED,
    BREAK,
    INTERNAL_ONLY,
    EXTERNAL_ONLY;

    /**
     * Deterministic classification from the presence of the key on each side and the amounts.
     * A null amount means the key is absent on that side.
     */
    public static ItemClassification of(BigDecimal internalValue, BigDecimal externalValue) {
        if (internalValue != null && externalValue != null) {
            return internalValue.compareTo(externalValue) == 0 ? MATCHED : BREAK;
        }
        if (internalValue != null) {
            return INTERNAL_ONLY;
        }
        return EXTERNAL_ONLY;
    }
}
