package com.ax.template.authblueprint.thresholdfiling;

/**
 * threshold-filing-obligation-l0 filing lifecycle (TFO-DEADLINE-001). OPEN = not yet closed; an
 * overdue OPEN filing stays visible in the overdue listing until acknowledged — it NEVER
 * silently expires. ACKNOWLEDGED = the ONLY terminal, reached exclusively through an explicit
 * who/when acknowledgment (mirrors ObligationStatus in the obligation domain).
 */
public enum FilingObligationStatus {
    OPEN,
    ACKNOWLEDGED
}
