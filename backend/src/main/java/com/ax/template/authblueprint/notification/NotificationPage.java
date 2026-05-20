package com.ax.template.authblueprint.notification;

import java.util.List;

/**
 * Paginated response envelope for {@code GET /api/notifications}.
 * Mirrors the shape used by the audit-log domain (AuditLogPage) for
 * consistency across the template's catalog.
 */
public record NotificationPage(
    List<NotificationResponse> content,
    long totalElements,
    int totalPages,
    int page,
    int size
) {}
