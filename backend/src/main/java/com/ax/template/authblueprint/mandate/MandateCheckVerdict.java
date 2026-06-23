package com.ax.template.authblueprint.mandate;

/**
 * mandate-fanout-l0 per-check verdict (MANDATE-BATTERY-001). A declared check starts PENDING
 * (no recorded verdict) and is recorded PASSED or FAILED. The mandate is SATISFIED only when
 * EVERY declared check is recorded PASSED — a single FAILED or still-PENDING check blocks (422).
 * The gate reads these per-check verdicts, never a bare aggregate.
 */
public enum MandateCheckVerdict {
    PENDING,
    PASSED,
    FAILED
}
