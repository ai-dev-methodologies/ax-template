package com.ax.template.authblueprint.divisibility;

import java.math.BigDecimal;

/**
 * material-divisibility-constraint-l0 pure arithmetic (DIV-DETERMINISM-001). The deterministic,
 * format-independent integrality and decimal-scale tests the whole domain turns on — exact
 * {@link BigDecimal} arithmetic, never a {@code double} parse or a string-length heuristic
 * (CWE-682: an incorrect calculation; {@code 0.1 + 0.2 != 0.3} in binary floating point).
 *
 * <p>{@link BigDecimal#stripTrailingZeros()} normalizes away the literal's formatting: {@code 5},
 * {@code 5.0} and {@code 5.00} all strip to the same value with scale {@code <= 0}, so all three
 * are integral; {@code 1.250} strips to scale {@code 2}. Integrality is then {@code strippedScale
 * <= 0} (equivalently {@code remainder(ONE) compareTo ZERO == 0}); the effective decimal scale is
 * {@code max(0, strippedScale)}. A pure function of its single input — no clock, no state — so it
 * is deterministic regardless of how the client formatted the decimal.
 */
final class DivisibilityArithmetic {

    private DivisibilityArithmetic() {}

    /** True iff {@code q} has no non-zero fractional part — 5, 5.0, 5.00 are integral; 5.5 is not. */
    static boolean isIntegral(BigDecimal q) {
        // stripTrailingZeros makes the test format-independent; scale <= 0 means a whole number.
        return q.stripTrailingZeros().scale() <= 0;
    }

    /** The effective number of fractional digits AFTER stripping trailing zeros (1.250 -> 2; 5 -> 0). */
    static int effectiveScale(BigDecimal q) {
        return Math.max(0, q.stripTrailingZeros().scale());
    }
}
