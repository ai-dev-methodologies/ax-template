package com.ax.template.authblueprint.auditlog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * Immutable audit log entry.
 * <p>
 * Trace:
 * <ul>
 *   <li>AUDIT-RECORD-001 — mandatory fields (actor, action, resource, outcome, timestamp)</li>
 *   <li>AUDIT-RECORD-002 — immutability: {@code @Column(updatable=false)} on every field</li>
 * </ul>
 * Manifest: {@code blueprints/audit-log-manifest.yaml#audit_policy}.
 */
@AggregateRoot
@Entity
@Table(
    name = "audit_logs",
    indexes = {
        @Index(name = "ix_audit_logs_timestamp", columnList = "timestamp"),
        @Index(name = "ix_audit_logs_actor", columnList = "actor_user_id"),
        @Index(name = "ix_audit_logs_resource", columnList = "resource_type,resource_id"),
        @Index(name = "ix_audit_logs_action", columnList = "action")
    }
)
public class AuditLog {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "actor_user_id", updatable = false, length = 255)
    private String actorUserId;

    @Column(name = "actor_ip", updatable = false, length = 64)
    private String actorIp;

    @Column(name = "action", updatable = false, nullable = false, length = 64)
    private String action;

    @Column(name = "resource_type", updatable = false, nullable = false, length = 128)
    private String resourceType;

    @Column(name = "resource_id", updatable = false, nullable = false, length = 255)
    private String resourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", updatable = false, nullable = false, length = 16)
    private AuditOutcome outcome;

    @Column(name = "timestamp", updatable = false, nullable = false)
    private Instant timestamp;

    @Column(name = "correlation_id", updatable = false, length = 128)
    private String correlationId;

    @Column(name = "user_agent", updatable = false, length = 512)
    private String userAgent;

    @Lob
    @Column(name = "metadata_json", updatable = false)
    private String metadataJson;

    /** Required by JPA. Do not call directly — use the builder. */
    protected AuditLog() {}

    private AuditLog(Builder b) {
        this.id = Objects.requireNonNull(b.id, "id");
        this.actorUserId = b.actorUserId;
        this.actorIp = b.actorIp;
        this.action = Objects.requireNonNull(b.action, "action");
        this.resourceType = Objects.requireNonNull(b.resourceType, "resourceType");
        this.resourceId = Objects.requireNonNull(b.resourceId, "resourceId");
        this.outcome = Objects.requireNonNull(b.outcome, "outcome");
        this.timestamp = Objects.requireNonNull(b.timestamp, "timestamp");
        this.correlationId = b.correlationId;
        this.userAgent = b.userAgent;
        this.metadataJson = b.metadataJson;
    }

    public UUID getId() { return id; }
    public String getActorUserId() { return actorUserId; }
    public String getActorIp() { return actorIp; }
    public String getAction() { return action; }
    public String getResourceType() { return resourceType; }
    public String getResourceId() { return resourceId; }
    public AuditOutcome getOutcome() { return outcome; }
    public Instant getTimestamp() { return timestamp; }
    public String getCorrelationId() { return correlationId; }
    public String getUserAgent() { return userAgent; }
    public String getMetadataJson() { return metadataJson; }

    public static Builder builder() { return new Builder(); }

    /** Builder. Constructs immutable entries. */
    public static final class Builder {
        private UUID id = UUID.randomUUID();
        private String actorUserId;
        private String actorIp;
        private String action;
        private String resourceType;
        private String resourceId;
        private AuditOutcome outcome = AuditOutcome.SUCCESS;
        private Instant timestamp = Instant.now();
        private String correlationId;
        private String userAgent;
        private String metadataJson;

        public Builder id(UUID id) { this.id = id; return this; }
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

        public AuditLog build() { return new AuditLog(this); }
    }
}
