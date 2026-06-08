package com.ax.template.authblueprint.auditlog;

import com.ax.template.authblueprint.common.PublishedApi;

import java.time.Instant;
import java.util.UUID;

/**
 * Published audit-write payload — the {@code auditlog} feature's cross-feature port
 * (DDD decomposition spec: docs/superpowers/specs/2026-06-08-ddd-decomposition-rules-design.md §3/§6).
 *
 * <p>Other features record an audit entry by building this DTO and handing it to
 * {@link AuditLogService#record(AuditLogDto)}. They never construct the {@link AuditLog}
 * JPA entity directly — constructing another aggregate's root is a boundary leak that
 * {@code HG-FEAT-ISOLATION} forbids (formerly grandfathered via the
 * {@code AX-DDD-AUDITLOG-ENTITY} allowlist exceptions, now retired).
 *
 * <p>Fields mirror {@link AuditLog}'s builder. Unset {@code id}/{@code outcome}/
 * {@code timestamp} stay {@code null}; the service lets the entity builder supply its
 * defaults (random id / {@code SUCCESS} / {@code Instant.now()}), preserving the prior
 * behaviour exactly.
 *
 * <p>{@code @PublishedApi} marks this as the feature's deliberate published surface
 * (see {@link PublishedApi}).
 */
@PublishedApi
public record AuditLogDto(
    UUID id,
    String actorUserId,
    String actorIp,
    String action,
    String resourceType,
    String resourceId,
    AuditOutcome outcome,
    Instant timestamp,
    String correlationId,
    String userAgent,
    String metadataJson
) {

    public static Builder builder() {
        return new Builder();
    }

    /** Fluent builder mirroring {@link AuditLog.Builder} so call sites swap only the type. */
    public static final class Builder {
        private UUID id;
        private String actorUserId;
        private String actorIp;
        private String action;
        private String resourceType;
        private String resourceId;
        private AuditOutcome outcome;
        private Instant timestamp;
        private String correlationId;
        private String userAgent;
        private String metadataJson;

        public Builder id(UUID v) { this.id = v; return this; }
        public Builder actorUserId(String v) { this.actorUserId = v; return this; }
        public Builder actorIp(String v) { this.actorIp = v; return this; }
        public Builder action(String v) { this.action = v; return this; }
        public Builder resourceType(String v) { this.resourceType = v; return this; }
        public Builder resourceId(String v) { this.resourceId = v; return this; }
        public Builder outcome(AuditOutcome v) { this.outcome = v; return this; }
        public Builder timestamp(Instant v) { this.timestamp = v; return this; }
        public Builder correlationId(String v) { this.correlationId = v; return this; }
        public Builder userAgent(String v) { this.userAgent = v; return this; }
        public Builder metadataJson(String v) { this.metadataJson = v; return this; }

        public AuditLogDto build() {
            return new AuditLogDto(id, actorUserId, actorIp, action, resourceType,
                resourceId, outcome, timestamp, correlationId, userAgent, metadataJson);
        }
    }
}
