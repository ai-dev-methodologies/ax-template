package com.ax.template.authblueprint.auditlog;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/** Body for POST /api/audit-logs/export. Mirrors OpenAPI ExportRequest. */
public record ExportRequest(
    @NotNull AuditExportFormat format,
    String actorId,
    String resourceType,
    String action,
    AuditOutcome outcome,
    Instant from,
    Instant to
) {
}
