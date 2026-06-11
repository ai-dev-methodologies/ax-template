package com.ax.template.authblueprint.obligation;

/**
 * deadline-obligation-l0 lifecycle. OPEN = the loop is not closed; the sweep escalates but NEVER
 * terminates. ACKNOWLEDGED = the ONLY terminal — reached exclusively through an explicit
 * acknowledgment recording who/when (OBL-ACK-001). There is deliberately NO EXPIRED state.
 */
public enum ObligationStatus {
    OPEN,
    ACKNOWLEDGED
}
