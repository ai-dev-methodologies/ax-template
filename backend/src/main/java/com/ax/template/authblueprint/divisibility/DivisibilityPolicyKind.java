package com.ax.template.authblueprint.divisibility;

/**
 * material-divisibility-constraint-l0 policy kind (DIV-INTEGRAL-001 / DIV-PRECISION-001). A material
 * is either INTEGER_ONLY — it may be transacted only in whole units (a discrete item, a motor, a
 * license; the discrete-variable case) — or FRACTIONAL — any positive amount is allowed, bounded by
 * a maximum decimal scale (bulk liquid/powder; the continuous-variable case). This is the prior,
 * orthogonal question every item master answers before quantization: MAY this material carry a
 * fractional quantity at all, and to what precision.
 */
public enum DivisibilityPolicyKind {
    INTEGER_ONLY,
    FRACTIONAL
}
