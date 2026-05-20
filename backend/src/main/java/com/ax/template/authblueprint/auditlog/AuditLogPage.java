package com.ax.template.authblueprint.auditlog;

import java.util.List;

/**
 * Mirrors {@code contracts/audit-log-openapi.yaml#AuditLogPage}.
 * Trace: AUDIT-LIST-001.
 */
public record AuditLogPage(
    List<AuditLogResponse> content,
    long totalElements,
    int totalPages,
    int page,
    int size
) {
}
