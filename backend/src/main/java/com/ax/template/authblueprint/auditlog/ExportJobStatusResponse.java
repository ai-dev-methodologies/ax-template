package com.ax.template.authblueprint.auditlog;

import java.util.UUID;

/** Returned by GET /api/audit-logs/export/{jobId}. */
public record ExportJobStatusResponse(
    UUID jobId,
    AuditExportStatus status,
    String downloadUrl,
    String errorMessage,
    Long recordCount
) {
}
