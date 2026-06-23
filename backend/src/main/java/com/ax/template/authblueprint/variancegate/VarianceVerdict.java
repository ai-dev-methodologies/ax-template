package com.ax.template.authblueprint.variancegate;

/**
 * variance-tolerance-band-l0 verdict (VG-GATE-001). Rendered by the ASYMMETRIC two-sided gate:
 * WITHIN_TOLERANCE iff the derived variance is ≥ −lowerTolerance AND ≤ +upperTolerance
 * (inclusive on both bounds), otherwise OUT_OF_TOLERANCE. The verdict is recorded on the
 * appraisal row so a downstream read sees a breach without re-deriving — and is NEVER rewritten
 * to WITHIN_TOLERANCE by a disposition (the breach stays visible WITH an override on record).
 */
public enum VarianceVerdict {
    WITHIN_TOLERANCE,
    OUT_OF_TOLERANCE
}
