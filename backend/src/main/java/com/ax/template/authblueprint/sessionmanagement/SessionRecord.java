package com.ax.template.authblueprint.sessionmanagement;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * SessionRecord — explicit per-login record bound to a JWT jti claim.
 *
 * <p>Trace:
 * <ul>
 *   <li>SESS-LIFECYCLE-001 — UNIQUE(user_id, jti) backs idempotent register</li>
 *   <li>SESS-LIFECYCLE-003 — hard delete forbidden; status flips to REVOKED</li>
 *   <li>SESS-INTROSPECT-002 — ipAddress + userAgent are @JsonIgnore; only the
 *       masked / summarized forms reach DTOs</li>
 *   <li>SESS-AUTHZ-002 — every lookup uses (id, userId) filter</li>
 * </ul>
 */
@AggregateRoot
@Entity
@Table(
    name = "session_records",
    uniqueConstraints = @UniqueConstraint(name = "uq_session_records_user_jti", columnNames = {"user_id", "jti"}),
    indexes = {
        @Index(name = "ix_session_records_user_created", columnList = "user_id,created_at"),
        @Index(name = "ix_session_records_jti", columnList = "jti"),
        @Index(name = "ix_session_records_status", columnList = "status")
    }
)
public class SessionRecord {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false, length = 255)
    private String userId;

    @Column(name = "jti", nullable = false, updatable = false, length = 128)
    private String jti;

    @Column(name = "device_label", updatable = false, length = 64)
    private String deviceLabel;

    /** Raw IP — never serialized. Masked at the DTO boundary. */
    @JsonIgnore
    @Column(name = "ip_address", updatable = false, length = 64)
    private String ipAddress;

    /** Raw User-Agent — never serialized. Summarized at the DTO boundary. */
    @JsonIgnore
    @Column(name = "user_agent", updatable = false, length = 512)
    private String userAgent;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private SessionStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_by_user_id", length = 255)
    private String revokedByUserId;

    protected SessionRecord() {}

    private SessionRecord(Builder b) {
        this.id = (b.id != null) ? b.id : UUID.randomUUID();
        this.userId = b.userId;
        this.jti = b.jti;
        this.deviceLabel = b.deviceLabel;
        this.ipAddress = b.ipAddress;
        this.userAgent = b.userAgent;
        this.status = (b.status != null) ? b.status : SessionStatus.ACTIVE;
        this.createdAt = (b.createdAt != null) ? b.createdAt : Instant.now();
        this.expiresAt = b.expiresAt;
    }

    public UUID getId() { return id; }
    public String getUserId() { return userId; }
    public String getJti() { return jti; }
    public String getDeviceLabel() { return deviceLabel; }

    @JsonIgnore public String getIpAddress() { return ipAddress; }
    @JsonIgnore public String getUserAgent() { return userAgent; }

    public SessionStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public String getRevokedByUserId() { return revokedByUserId; }

    public boolean isExpired(Instant now) {
        return expiresAt != null && now.isAfter(expiresAt);
    }

    // Package-private — only the state machine + service mutates these.
    void markRevoked(Instant when, String actorUserId) {
        if (this.status == SessionStatus.REVOKED) {
            return;  // idempotent — preserve original revokedAt + revokedByUserId
        }
        this.status = SessionStatus.REVOKED;
        this.revokedAt = when;
        this.revokedByUserId = actorUserId;
    }

    void touchLastSeen(Instant when) { this.lastSeenAt = when; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private UUID id;
        private String userId;
        private String jti;
        private String deviceLabel;
        private String ipAddress;
        private String userAgent;
        private SessionStatus status;
        private Instant createdAt;
        private Instant expiresAt;

        public Builder id(UUID v) { this.id = v; return this; }
        public Builder userId(String v) { this.userId = v; return this; }
        public Builder jti(String v) { this.jti = v; return this; }
        public Builder deviceLabel(String v) { this.deviceLabel = v; return this; }
        public Builder ipAddress(String v) { this.ipAddress = v; return this; }
        public Builder userAgent(String v) { this.userAgent = v; return this; }
        public Builder status(SessionStatus v) { this.status = v; return this; }
        public Builder createdAt(Instant v) { this.createdAt = v; return this; }
        public Builder expiresAt(Instant v) { this.expiresAt = v; return this; }

        public SessionRecord build() { return new SessionRecord(this); }
    }
}
