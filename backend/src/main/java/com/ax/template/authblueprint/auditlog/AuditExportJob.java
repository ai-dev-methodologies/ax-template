package com.ax.template.authblueprint.auditlog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Tracks asynchronous export jobs.
 * <p>
 * Trace: AUDIT-EXPORT-001 — async export with job ID.
 *
 * <p>Unlike {@link AuditLog} this entity is mutable (status transitions
 * PENDING → PROCESSING → COMPLETED/FAILED), but only the export worker writes
 * to it; never through user-facing controllers.
 */
@Entity
@Table(name = "audit_export_jobs")
public class AuditExportJob {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "requested_by", nullable = false, length = 255)
    private String requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false, length = 8)
    private AuditExportFormat format;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private AuditExportStatus status;

    @Lob
    @Column(name = "filter_json")
    private String filterJson;

    @Column(name = "download_url", length = 1024)
    private String downloadUrl;

    @Column(name = "error_message", length = 1024)
    private String errorMessage;

    @Column(name = "record_count")
    private Long recordCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected AuditExportJob() {}

    public AuditExportJob(UUID id, String requestedBy, AuditExportFormat format,
                          AuditExportStatus status, String filterJson) {
        this.id = id;
        this.requestedBy = requestedBy;
        this.format = format;
        this.status = status;
        this.filterJson = filterJson;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getRequestedBy() { return requestedBy; }
    public AuditExportFormat getFormat() { return format; }
    public AuditExportStatus getStatus() { return status; }
    public String getFilterJson() { return filterJson; }
    public String getDownloadUrl() { return downloadUrl; }
    public String getErrorMessage() { return errorMessage; }
    public Long getRecordCount() { return recordCount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void markProcessing() {
        this.status = AuditExportStatus.PROCESSING;
        this.updatedAt = Instant.now();
    }

    public void markCompleted(String downloadUrl, long recordCount) {
        this.status = AuditExportStatus.COMPLETED;
        this.downloadUrl = downloadUrl;
        this.recordCount = recordCount;
        this.updatedAt = Instant.now();
    }

    public void markFailed(String errorMessage) {
        this.status = AuditExportStatus.FAILED;
        this.errorMessage = errorMessage;
        this.updatedAt = Instant.now();
    }
}
