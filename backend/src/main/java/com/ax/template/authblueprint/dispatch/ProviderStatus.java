package com.ax.template.authblueprint.dispatch;

/**
 * Supply-side duty state (timed-offer-l0 AVAIL-FSM-001). AVAILABLE and ASSIGNED are
 * mutually exclusive — a provider is never simultaneously offerable and busy (the supply-side
 * root of double-dispatch). The contended AVAILABLE→ASSIGNED edge is NOT a state-machine call;
 * it is the atomic status-guarded conditional UPDATE (exclusive-assignment-l0 EXCL-CLAIM-001).
 */
public enum ProviderStatus {
    OFFLINE,
    AVAILABLE,
    ASSIGNED
}
