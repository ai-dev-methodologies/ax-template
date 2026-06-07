package com.ax.template.authblueprint.dispatch;

/**
 * Offer lifecycle (timed-offer-l0 OFFER-FSM-001): PENDING → one of three terminal outcomes.
 * At most one PENDING offer may exist per request (partial unique index, enforced at the DB).
 */
public enum OfferStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    EXPIRED
}
