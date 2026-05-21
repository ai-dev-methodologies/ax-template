package com.ax.template.authblueprint.reportexport;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * ExportJob — the persistence record for an asynchronous report-export request.
 *
 * <p>Trace:
 * <ul>
 *   <li>EXPORT-AUTHZ-002 — every lookup filters on {@code (id, ownerUserId)}</li>
 *   <li>EXPORT-LIFECYCLE-001 — created in {@link ExportJobStatus#PENDING}</li>
 *   <li>EXPORT-LIFECYCLE-002 — {@code status} mirrored on responses</li>
 *   <li>EXPORT-LIFECYCLE-004 — {@code status} mutated only via {@link ExportJobStateMachine}</li>
 * </ul>
 *
 * <p>The rendered file bytes are stored in {@code payload} (a small LOB) for MVP
 * simplicity. A fork-receiver scaling beyond the 100k row advisory limit should
 * delegate persistence to the file-storage domain instead.
 */
@Entity
@Table(
    name = "export_jobs",
    indexes = {
        @Index(name = "ix_export_jobs_owner_status", columnList = "owner_user_id,status"),
        @Index(name = "ix_export_jobs_status_created", columnList = "status,created_at")
    }
)
public class ExportJob {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false, updatable = false, length = 255)
    private String ownerUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false, updatable = false, length = 16)
    private ExportFormat format;

    @Column(name = "name", length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ExportJobStatus status;

    @Column(name = "row_count")
    private Long rowCount;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "error_message", length = 1024)
    private String errorMessage;

    /**
     * Serialized JSON of the {@code query} object supplied at creation time. The worker
     * deserializes this back into a {@code Map<String,Object>} before invoking the
     * {@link ReportRowSource} so the original query parameters survive the async hop.
     */
    @Column(name = "query_json", length = 4096)
    private String queryJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    /**
     * Generated file bytes. Stored as a regular binary column (Hibernate maps {@code byte[]}
     * to VARBINARY on H2 and BYTEA on PostgreSQL — neither requires {@code @Lob}, which would
     * generate H2's unsupported BLOB type in PostgreSQL compatibility mode).
     */
    @JsonIgnore
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "payload", columnDefinition = "bytea")
    private byte[] payload;

    /** Required by JPA. */
    protected ExportJob() {}

    private ExportJob(Builder b) {
        this.id = (b.id != null) ? b.id : UUID.randomUUID();
        this.ownerUserId = b.ownerUserId;
        this.format = b.format;
        this.name = b.name;
        this.status = (b.status != null) ? b.status : ExportJobStatus.PENDING;
        this.createdAt = (b.createdAt != null) ? b.createdAt : Instant.now();
        this.queryJson = b.queryJson;
    }

    public UUID getId() { return id; }
    public String getOwnerUserId() { return ownerUserId; }
    public ExportFormat getFormat() { return format; }
    public String getName() { return name; }
    public ExportJobStatus getStatus() { return status; }
    public Long getRowCount() { return rowCount; }
    public Long getSizeBytes() { return sizeBytes; }
    public String getErrorMessage() { return errorMessage; }
    public String getQueryJson() { return queryJson; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }

    @JsonIgnore
    public byte[] getPayload() { return payload; }

    /**
     * Package-private setters used exclusively by {@link ExportJobStateMachine}.
     * Calling code outside the state machine MUST NOT mutate these fields.
     */
    void setStatus(ExportJobStatus next) { this.status = next; }
    void setStartedAt(Instant ts) { this.startedAt = ts; }
    void setCompletedAt(Instant ts) { this.completedAt = ts; }
    void setRowCount(long v) { this.rowCount = v; }
    void setSizeBytes(long v) { this.sizeBytes = v; }
    void setErrorMessage(String msg) { this.errorMessage = msg; }
    void setPayload(byte[] bytes) { this.payload = bytes; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private UUID id;
        private String ownerUserId;
        private ExportFormat format;
        private String name;
        private ExportJobStatus status;
        private Instant createdAt;
        private String queryJson;

        public Builder id(UUID v) { this.id = v; return this; }
        public Builder ownerUserId(String v) { this.ownerUserId = v; return this; }
        public Builder format(ExportFormat v) { this.format = v; return this; }
        public Builder name(String v) { this.name = v; return this; }
        public Builder status(ExportJobStatus v) { this.status = v; return this; }
        public Builder createdAt(Instant v) { this.createdAt = v; return this; }
        public Builder queryJson(String v) { this.queryJson = v; return this; }

        public ExportJob build() { return new ExportJob(this); }
    }
}
