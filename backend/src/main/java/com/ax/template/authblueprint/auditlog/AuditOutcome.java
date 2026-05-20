package com.ax.template.authblueprint.auditlog;

/**
 * Outcome of an audited operation.
 * <p>
 * Trace: blueprints/audit-log-manifest.yaml#audit_policy.mandatory_fields[outcome]
 */
public enum AuditOutcome {
    SUCCESS,
    FAILURE
}
