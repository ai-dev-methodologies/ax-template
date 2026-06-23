package com.ax.template.authblueprint.divisibility;

/**
 * material-divisibility-constraint-l0 quantity-check verdict (DIV-RECORD-001). Recorded on every
 * immutable DivisibilityCheck against the policy version in force: ACCEPTED (the quantity is
 * representable for the material), NON_INTEGRAL (a fractional quantity against an INTEGER_ONLY
 * material — rejected, never rounded), or EXCESS_PRECISION (a quantity whose decimal scale exceeds
 * a FRACTIONAL material's maximum — rejected, never truncated).
 */
public enum CheckVerdict {
    ACCEPTED,
    NON_INTEGRAL,
    EXCESS_PRECISION
}
