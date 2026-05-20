package com.ax.template.authblueprint.auditlog;

/**
 * Audit-log retention tiers.
 * <p>
 * Trace: AUDIT-RETENTION-002 — three standard tiers.
 * Manifest: {@code blueprints/audit-log-manifest.yaml#retention.tiers}.
 */
public enum RetentionTier {
    SHORT(30),
    STANDARD(90),
    LONG(365);

    private final int daysToKeep;

    RetentionTier(int daysToKeep) {
        this.daysToKeep = daysToKeep;
    }

    public int daysToKeep() { return daysToKeep; }
}
