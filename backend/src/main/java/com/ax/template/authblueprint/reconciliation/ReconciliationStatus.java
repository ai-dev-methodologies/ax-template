package com.ax.template.authblueprint.reconciliation;

/**
 * external-reconciliation-l0 run lifecycle (RECON-RESOLVE-001). OPEN → RESOLVED only — a run is
 * RESOLVED once every break carries a disposition, and is never reopened or deleted. The
 * forward step is gated by {@link ReconciliationService}, which refuses to resolve while any
 * break is undisposed (RECON-RESOLVE-001).
 */
public enum ReconciliationStatus {
    OPEN,
    RESOLVED
}
