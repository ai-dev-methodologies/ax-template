package com.ax.template.authblueprint.settlement;

/**
 * settlement-finality-l0 instruction lifecycle.
 *
 * <p>Happy path: {@code PENDING → SETTLED}. SETTLED is the IRREVOCABLE FINAL state — after
 * finality, novation/cancel/amend are all 409 (BIS CPMI "final settlement" = irrevocable and
 * unconditional). Fail ladder: {@code PENDING → FAILED → RETRY → BUYIN}, each edge taken at
 * most once (exactly-once transitions). FAILED/RETRY may still recover to SETTLED on a fresh
 * settle attempt; BUYIN is terminal-failed (the obligation is resolved off-ladder by a buy-in).
 */
public enum SettlementStatus {
    PENDING,
    SETTLED,
    FAILED,
    RETRY,
    BUYIN
}
