package com.ax.template.authblueprint.inputplausibility;

/**
 * self-reported-input-plausibility-l0 provenance status (PLAUSIBILITY-PROVENANCE-001). A value
 * the server cannot independently verify is admitted ONLY as {@link #SELF_REPORTED_UNVERIFIED} —
 * there is deliberately NO {@code CONFIRMED} / server-verified constant, because the server has
 * no authoritative source to confirm a self-report against. The single-value enum makes the
 * unverified provenance unrepresentable any other way: a downstream consumer always sees that
 * the value's origin is a self-report that passed plausibility, never a server confirmation.
 */
public enum VerificationStatus {
    SELF_REPORTED_UNVERIFIED
}
