package com.ax.template.authblueprint.inputplausibility;

/**
 * PLAUSIBILITY-DATE-RANGE/FUTURE-001 rejection reason. A single deterministic verdict — the
 * asserted date fell outside the channel's configured [reference - maxLookback,
 * reference + maxLookahead] window, whether by lying too far in the past or too far in the
 * future (the future edge is PLAUSIBILITY-DATE-FUTURE-001's boundary-precision concern, not a
 * distinct violation type).
 */
public enum DateRejectReason {
    IMPLAUSIBLE_DATE_RANGE
}
