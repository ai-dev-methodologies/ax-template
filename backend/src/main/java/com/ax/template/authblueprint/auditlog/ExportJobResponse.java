package com.ax.template.authblueprint.auditlog;

import java.util.UUID;

/** Returned by POST /api/audit-logs/export (HTTP 202). */
public record ExportJobResponse(
    UUID jobId,
    AuditExportStatus status
) {
}
