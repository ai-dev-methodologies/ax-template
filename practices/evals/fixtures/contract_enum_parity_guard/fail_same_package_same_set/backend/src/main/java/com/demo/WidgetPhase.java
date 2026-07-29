package com.demo;

/**
 * FIXTURE (P3-87) — the ONLY delta vs pass_clean.
 *
 * A second enum in the SAME package com.demo carrying the SAME constant set as
 * WidgetStatus {OPEN, CLOSED}. Nothing above the P3-87 check can tell the two
 * apart: the FQCN resolves, the package leaf still spells the contract domain,
 * and the constant sets compare equal — so repointing a WidgetStatus binding at
 * WidgetPhase would keep the guard green while WidgetStatus drifted unobserved.
 *
 * Expected: exit 1, AMBIGUOUS BINDING on both com.demo.WidgetStatus entries.
 */
public enum WidgetPhase {
    OPEN,
    CLOSED
}
