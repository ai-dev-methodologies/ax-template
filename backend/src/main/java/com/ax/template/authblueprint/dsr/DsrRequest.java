package com.ax.template.authblueprint.dsr;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * DsrRequest — the tracking record for a single data-subject-rights request
 * (DSR-SLA-001: {@code {requestId, type, status, receivedAt, dueAt, closedAt}}).
 *
 * <p>Trace:
 * <ul>
 *   <li>DSR-SLA-001 — {@code dueAt = receivedAt + 30 days}; a single request MAY
 *       extend by ≤ 60 further days ({@code extensionDays} + {@code extensionReason}).</li>
 *   <li>DSR-SLA-001 — owner-scoped lookups filter on {@code (id, subjectId)};
 *       a cross-subject GET without ROLE_ADMIN collapses to a 404.</li>
 *   <li>{@code status} is mutated ONLY by {@link DsrRequestStateMachine}.</li>
 * </ul>
 *
 * <p>Immutable identity columns ({@code requestId} (the {@code id} primary key),
 * {@code subjectId}, {@code type}, {@code receivedAt}) carry
 * {@code @Column(updatable=false)} — re-pointing any of them mid-life would be a
 * stealth ownership / classification change. {@code @Version} guards concurrent
 * status / SLA mutation.
 *
 * <p>This entity holds only request METADATA (no PHI / sensitive personal data) —
 * the personal data itself lives in the owning modules and is reached through the
 * {@link PersonalDataProvider} SPI, never copied onto this row.
 */
@AggregateRoot
@Entity
@Table(
    name = "dsr_requests",
    indexes = {
        @Index(name = "ix_dsr_requests_subject", columnList = "subject_id"),
        @Index(name = "ix_dsr_requests_subject_type_status", columnList = "subject_id,type,status"),
        @Index(name = "ix_dsr_requests_status_due", columnList = "status,due_at")
    }
)
public class DsrRequest {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "subject_id", nullable = false, updatable = false, length = 255)
    private String subjectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, updatable = false, length = 16)
    private DsrRequestType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private DsrRequestStatus status;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    /** Cumulative extension applied to this single request; capped at 60 days (DSR-SLA-001). */
    @Column(name = "extension_days", nullable = false)
    private int extensionDays;

    @Column(name = "extension_reason", length = 512)
    private String extensionReason;

    @Column(name = "sla_breached", nullable = false)
    private boolean slaBreached;

    /**
     * DSR-ERASURE-001 idempotency: the completion manifest serialized once when this
     * ERASURE request closes. A re-request returns it verbatim (never re-runs provider
     * erase()), so the manifest is identical across re-requests. Null for non-erasure
     * requests and until the first erasure closes.
     */
    @Column(name = "erasure_manifest_json", columnDefinition = "TEXT")
    private String erasureManifestJson;

    @Version
    @Column(name = "version")
    private Long version;

    /** Required by JPA. */
    protected DsrRequest() {}

    private DsrRequest(Builder b) {
        this.id = (b.id != null) ? b.id : UUID.randomUUID();
        this.subjectId = b.subjectId;
        this.type = b.type;
        this.status = (b.status != null) ? b.status : DsrRequestStatus.RECEIVED;
        this.receivedAt = b.receivedAt;
        this.dueAt = b.dueAt;
        this.extensionDays = b.extensionDays;
        this.slaBreached = b.slaBreached;
    }

    public UUID getId() { return id; }
    public String getSubjectId() { return subjectId; }
    public DsrRequestType getType() { return type; }
    public DsrRequestStatus getStatus() { return status; }
    public Instant getReceivedAt() { return receivedAt; }
    public Instant getDueAt() { return dueAt; }
    public Instant getClosedAt() { return closedAt; }
    public int getExtensionDays() { return extensionDays; }
    public String getExtensionReason() { return extensionReason; }
    public boolean isSlaBreached() { return slaBreached; }
    public String getErasureManifestJson() { return erasureManifestJson; }
    public Long getVersion() { return version; }

    /**
     * Package-private mutators — used exclusively by {@link DsrRequestStateMachine}
     * (status / closedAt) and {@link DsrService} (SLA extension / breach flag).
     * Calling code outside the domain MUST NOT mutate these fields.
     */
    void setStatus(DsrRequestStatus next) { this.status = next; }
    void setClosedAt(Instant ts) { this.closedAt = ts; }
    void setDueAt(Instant ts) { this.dueAt = ts; }
    void setExtensionDays(int days) { this.extensionDays = days; }
    void setExtensionReason(String reason) { this.extensionReason = reason; }
    void setSlaBreached(boolean breached) { this.slaBreached = breached; }
    void setErasureManifestJson(String json) { this.erasureManifestJson = json; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private UUID id;
        private String subjectId;
        private DsrRequestType type;
        private DsrRequestStatus status;
        private Instant receivedAt;
        private Instant dueAt;
        private int extensionDays;
        private boolean slaBreached;

        public Builder id(UUID v) { this.id = v; return this; }
        public Builder subjectId(String v) { this.subjectId = v; return this; }
        public Builder type(DsrRequestType v) { this.type = v; return this; }
        public Builder status(DsrRequestStatus v) { this.status = v; return this; }
        public Builder receivedAt(Instant v) { this.receivedAt = v; return this; }
        public Builder dueAt(Instant v) { this.dueAt = v; return this; }
        public Builder extensionDays(int v) { this.extensionDays = v; return this; }
        public Builder slaBreached(boolean v) { this.slaBreached = v; return this; }

        public DsrRequest build() { return new DsrRequest(this); }
    }
}
