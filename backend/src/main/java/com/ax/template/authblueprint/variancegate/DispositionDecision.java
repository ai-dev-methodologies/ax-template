package com.ax.template.authblueprint.variancegate;

/**
 * variance-tolerance-band-l0 disposition decision (VG-DISPOSE-001). The accountable decision
 * attached to a breach: OVERRIDE proceeds past the OUT_OF_TOLERANCE appraisal (recording the
 * actor / when / reason), REJECT records a refusal to proceed. A disposition is DISTINCT from
 * silent acceptance — there is no path past a breach without a recorded who/when/reason.
 */
public enum DispositionDecision {
    OVERRIDE,
    REJECT
}
