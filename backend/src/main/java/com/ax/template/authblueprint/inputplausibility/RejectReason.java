package com.ax.template.authblueprint.inputplausibility;

/**
 * self-reported-input-plausibility-l0 rejection reason (PLAUSIBILITY-REJECT-001). The deterministic
 * verdict a rejected submission is recorded under: {@link #IMPLAUSIBLE_RANGE} (value outside the
 * channel's configured [min, max]) or {@link #IMPLAUSIBLE_RATE} (the jump from the prior accepted
 * value over elapsed time exceeded the channel's max delta-per-second — teleport / rollback /
 * impossible spike). The reason travels on the recorded rejected-attempt row so the rejection is
 * auditable, and it is the machine-readable {@code code} on the 422 ProblemDetail.
 */
public enum RejectReason {
    IMPLAUSIBLE_RANGE,
    IMPLAUSIBLE_RATE
}
