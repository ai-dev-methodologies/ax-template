package com.ax.template.authblueprint.reproducibility;

/**
 * reproducible-procedure-l0 deterministic field masking (PROC-BLIND-001). The masked projection a
 * MEMBER sees is derived deterministically from the raw value (same raw → same mask) so the
 * blinding is itself reproducible. The raw value is reachable only by a privileged role (ADMIN) —
 * NIST SP 800-53 least-privilege: each caller is granted the minimum it needs.
 */
final class FieldBlinder {

    private FieldBlinder() {}

    /**
     * Mask {@code raw} to first + last visible char with the middle replaced by a fixed number of
     * stars (deterministic — never reveals length). {@code null} masks to {@code null}; a short
     * value (≤ 2 chars) is fully starred so it is never disclosed.
     */
    static String mask(String raw) {
        if (raw == null) {
            return null;
        }
        if (raw.length() <= 2) {
            return "***";
        }
        return raw.charAt(0) + "***" + raw.charAt(raw.length() - 1);
    }
}
