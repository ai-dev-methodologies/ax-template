package com.ax.template.authblueprint.timedoffer;

/**
 * timed-offer-exclusive-assignment-l0 offer lifecycle (TIMEDOFFER-LIFECYCLE-001):
 * OPEN → one of three terminal outcomes. OPEN is the only non-terminal state.
 * ACCEPTED means this offer won the subject (exactly one per subject — TIMEDOFFER-EXCLUSIVE-001);
 * DECLINED is a human refusal; EXPIRED is the deadline-sweep outcome (recorded SYSTEM/when).
 */
public enum OfferStatus {
    OPEN,
    ACCEPTED,
    DECLINED,
    EXPIRED
}
