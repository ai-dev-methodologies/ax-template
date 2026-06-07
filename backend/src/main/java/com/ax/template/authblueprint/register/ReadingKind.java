package com.ax.template.authblueprint.register;

/**
 * monotone-register-l0 read kinds. NORMAL = a monotone read (read ≥ anchor, delta = read − anchor).
 * ROLLOVER = a governed odometer wrap (read &lt; anchor, delta = (modulus − anchor) + read). EXCHANGE =
 * a governed device swap (baseline reset to the new opening read, delta = 0). ROLLOVER and EXCHANGE
 * are the ONLY paths that may set the anchor below its prior value, and both require a reason.
 */
public enum ReadingKind {
    NORMAL,
    ROLLOVER,
    EXCHANGE;

    boolean isGovernedException() {
        return this != NORMAL;
    }
}
