package com.ax.template.authblueprint.auditlog;

import java.time.Instant;
import java.util.UUID;

/**
 * Public response shape for GET /api/audit-logs entries. Mirrors
 * {@code contracts/audit-log-openapi.yaml#components.schemas.AuditLogSummary}.
 */
public record AuditLogResponse(
    UUID id,
    String actorId,
    String actorIp,
    String action,
    String resourceType,
    String resourceId,
    AuditOutcome outcome,
    Instant timestamp,
    String correlationId,
    String userAgent
) {

    public static AuditLogResponse from(AuditLog e) {
        return new AuditLogResponse(
            e.getId(),
            e.getActorUserId(),
            e.getActorIp(),
            e.getAction(),
            e.getResourceType(),
            e.getResourceId(),
            e.getOutcome(),
            e.getTimestamp(),
            e.getCorrelationId(),
            e.getUserAgent()
        );
    }
}
