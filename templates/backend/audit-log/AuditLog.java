/**
 * @ax-template-meta
 * template_id: backend/audit-log/AuditLog
 * layer: backend-domain
 * domain: audit-log
 * anchors_rule: specs/audit-log-l0.yaml#AUDIT-RECORD-001
 *               specs/audit-log-l0.yaml#AUDIT-RECORD-002
 *               blueprints/audit-log-manifest.yaml#audit_policy
 * provenance_class: external_canonical
 * evidence:
 *   - source_type: external
 *     citation: "ISO 27001:2022 Annex A.8.15 — Logging: log records should include user ID, actions, and timestamps"
 *     url: "https://www.iso.org/standard/27001"
 *   - source_type: external
 *     citation: "OWASP ASVS V4 — V7.1.1 Ensure all security events are logged with sufficient context"
 *     url: "https://owasp.org/www-project-application-security-verification-standard/"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   All fields are @Column(updatable=false) to enforce immutability (AUDIT-RECORD-002).
 *   Do not add setters beyond the builder/constructor — use AuditLogBuilder instead.
 */
package com.example.app.auditlog;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * AuditLog — JPA entity representing an immutable audit log entry.
 *
 * <p>Immutability contract (AUDIT-RECORD-002):
 * <ul>
 *   <li>All columns are {@code updatable = false} — no field can change after INSERT.
 *   <li>No setters are provided. Construction via {@link AuditLogBuilder}.
 *   <li>{@code AuditLogRepository} exposes only save() and read operations.
 * </ul>
 *
 * <p>Non-blocking (AUDIT-RECORD-003):
 * <ul>
 *   <li>Persistence is wrapped in {@code REQUIRES_NEW} by {@code AuditLogService}.
 *   <li>AuditLoggingAspect swallows persistence failures — the originating operation is unaffected.
 * </ul>
 *
 * <p>Rule references: AUDIT-RECORD-001, AUDIT-RECORD-002, AUDIT-RECORD-003.
 */
@Entity
@SQLDelete(sql = "UPDATE audit_logs SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
@Table(
    name = "audit_logs",
    indexes = {
        @Index(name = "idx_audit_logs_actor_id", columnList = "actor_id"),
        @Index(name = "idx_audit_logs_resource", columnList = "resource_type, resource_id"),
        @Index(name = "idx_audit_logs_timestamp", columnList = "timestamp"),
        @Index(name = "idx_audit_logs_action", columnList = "action"),
    }
)
public class AuditLog {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** User ID of the actor (may be masked per PII policy). */
    @Column(name = "actor_id", updatable = false, nullable = false, length = 255)
    private String actorId;

    /** IP address of the actor (masked per PII redaction policy). */
    @Column(name = "actor_ip", updatable = false, length = 64)
    private String actorIp;

    /** Action verb (CREATE, UPDATE, DELETE, LOGIN, EXPORT, etc.). */
    @Column(name = "action", updatable = false, nullable = false, length = 64)
    private String action;

    /** Domain entity type (payment, user, item, etc.). */
    @Column(name = "resource_type", updatable = false, nullable = false, length = 64)
    private String resourceType;

    /** Entity primary key as string. */
    @Column(name = "resource_id", updatable = false, length = 255)
    private String resourceId;

    /** SUCCESS or FAILURE. */
    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", updatable = false, nullable = false, length = 16)
    private Outcome outcome;

    /** UTC timestamp of the operation. Auto-set on insert. */
    @CreationTimestamp
    @Column(name = "timestamp", updatable = false, nullable = false)
    private Instant timestamp;

    /** X-Correlation-Id from the originating request for trace linkage. */
    @Column(name = "correlation_id", updatable = false, length = 128)
    private String correlationId;

    /** HTTP User-Agent from the originating request. */
    @Column(name = "user_agent", updatable = false, length = 512)
    private String userAgent;

    /**
     * Optional structured metadata: changed fields, request params, diff snapshots.
     * Stored as JSONB in PostgreSQL or JSON in MySQL.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", updatable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    /**
     * Soft-delete timestamp. NULL = active; non-null = logically deleted.
     * Set by @SQLDelete — do not set directly.
     * Excluded from all standard queries by @Where(clause = "deleted_at IS NULL").
     */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    /** Required by JPA. Do not use directly — use {@link AuditLogBuilder}. */
    protected AuditLog() {}

    private AuditLog(AuditLogBuilder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID();
        this.actorId = builder.actorId;
        this.actorIp = builder.actorIp;
        this.action = builder.action;
        this.resourceType = builder.resourceType;
        this.resourceId = builder.resourceId;
        this.outcome = builder.outcome;
        this.correlationId = builder.correlationId;
        this.userAgent = builder.userAgent;
        this.metadata = builder.metadata;
    }

    // ─── Accessors (no setters — immutable after construction) ──────────────

    public UUID getId() { return id; }
    public String getActorId() { return actorId; }
    public String getActorIp() { return actorIp; }
    public String getAction() { return action; }
    public String getResourceType() { return resourceType; }
    public String getResourceId() { return resourceId; }
    public Outcome getOutcome() { return outcome; }
    public Instant getTimestamp() { return timestamp; }
    public String getCorrelationId() { return correlationId; }
    public String getUserAgent() { return userAgent; }
    public Map<String, Object> getMetadata() { return metadata; }
    public Instant getDeletedAt() { return deletedAt; }
    public boolean isDeleted()    { return deletedAt != null; }

    // ─── Nested types ────────────────────────────────────────────────────────

    public enum Outcome {
        SUCCESS, FAILURE
    }

    // ─── Builder ─────────────────────────────────────────────────────────────

    public static AuditLogBuilder builder() {
        return new AuditLogBuilder();
    }

    public static final class AuditLogBuilder {
        private UUID id;
        private String actorId;
        private String actorIp;
        private String action;
        private String resourceType;
        private String resourceId;
        private Outcome outcome = Outcome.SUCCESS;
        private String correlationId;
        private String userAgent;
        private Map<String, Object> metadata;

        private AuditLogBuilder() {}

        public AuditLogBuilder id(UUID id) { this.id = id; return this; }
        public AuditLogBuilder actorId(String actorId) { this.actorId = actorId; return this; }
        public AuditLogBuilder actorIp(String actorIp) { this.actorIp = actorIp; return this; }
        public AuditLogBuilder action(String action) { this.action = action; return this; }
        public AuditLogBuilder resourceType(String resourceType) { this.resourceType = resourceType; return this; }
        public AuditLogBuilder resourceId(String resourceId) { this.resourceId = resourceId; return this; }
        public AuditLogBuilder outcome(Outcome outcome) { this.outcome = outcome; return this; }
        public AuditLogBuilder correlationId(String correlationId) { this.correlationId = correlationId; return this; }
        public AuditLogBuilder userAgent(String userAgent) { this.userAgent = userAgent; return this; }
        public AuditLogBuilder metadata(Map<String, Object> metadata) { this.metadata = metadata; return this; }

        public AuditLog build() {
            if (actorId == null || actorId.isBlank()) throw new IllegalStateException("actorId is required");
            if (action == null || action.isBlank()) throw new IllegalStateException("action is required");
            if (resourceType == null || resourceType.isBlank()) throw new IllegalStateException("resourceType is required");
            return new AuditLog(this);
        }
    }
}
