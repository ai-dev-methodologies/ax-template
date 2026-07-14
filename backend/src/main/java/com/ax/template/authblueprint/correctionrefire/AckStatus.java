package com.ax.template.authblueprint.correctionrefire;

/**
 * correction-refire-l0 — an acknowledgement's own 2-state lifecycle: PENDING (awaiting the
 * recipient's ack) -> CLOSED (acknowledged). A correction re-opens the loop for the CORRECTED
 * version by creating a NEW PENDING row — it never flips an existing CLOSED row back to PENDING
 * (CRF-REFIRE-002).
 */
public enum AckStatus {
    PENDING,
    CLOSED
}
