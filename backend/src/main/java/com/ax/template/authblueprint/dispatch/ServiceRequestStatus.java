package com.ax.template.authblueprint.dispatch;

/**
 * Demand-side lifecycle. PENDING→OFFERED (a candidate has a live offer) →ASSIGNED (a provider
 * accepted) →FULFILLED; OFFERED→UNFULFILLED when the offer cascade exhausts (OFFER-CASCADE-004);
 * PENDING/OFFERED→CANCELLED. The contended OFFERED→ASSIGNED edge is the atomic status-guarded
 * conditional UPDATE (exclusive-assignment-l0 EXCL-CLAIM-001), not a state-machine call.
 */
public enum ServiceRequestStatus {
    PENDING,
    OFFERED,
    ASSIGNED,
    FULFILLED,
    UNFULFILLED,
    CANCELLED
}
