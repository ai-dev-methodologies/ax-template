package com.ax.template.authblueprint.filestorage;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * StoredFile entity — keyed by owner.
 * <p>
 * Trace:
 * <ul>
 *   <li>FILE-AUTHZ-002 — every lookup filters on (id, owner_user_id, deleted=false)</li>
 *   <li>FILE-UPLOAD-003 — {@code fileName} is the sanitized display name; {@code storageKey} is an opaque UUID</li>
 *   <li>FILE-SEC-001 / FILE-SEC-002 — {@code storageKey} is {@link JsonIgnore @JsonIgnore}; never serialized</li>
 *   <li>FILE-SCAN-001 — {@code status} transitions PENDING → READY | QUARANTINED</li>
 *   <li>FILE-QUOTA-001 — {@code sizeBytes} feeds the per-user quota sum</li>
 * </ul>
 * Manifest: {@code blueprints/file-storage-manifest.yaml}.
 */
@Entity
@Table(
    name = "stored_files",
    indexes = {
        @Index(name = "ix_stored_files_owner_status",
               columnList = "owner_user_id,status,deleted"),
        @Index(name = "ix_stored_files_sha256_owner",
               columnList = "sha256,owner_user_id")
    }
)
public class StoredFile {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false, updatable = false, length = 255)
    private String ownerUserId;

    /** Sanitized display filename — never used as a filesystem/S3 key. */
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 255)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "sha256", nullable = false, length = 64)
    private String sha256;

    /**
     * FILE-SEC-001 / FILE-SEC-002 — opaque storage key (UUID for local FS, S3 key in S3).
     * Never serialized to API consumers.
     */
    @JsonIgnore
    @Column(name = "storage_key", nullable = false, updatable = false, length = 512)
    private String storageKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private FileStatus status;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    @Column(name = "scanned_at")
    private Instant scannedAt;

    /** Required by JPA. */
    protected StoredFile() {}

    private StoredFile(Builder b) {
        this.id = (b.id != null) ? b.id : UUID.randomUUID();
        this.ownerUserId = b.ownerUserId;
        this.fileName = b.fileName;
        this.contentType = b.contentType;
        this.sizeBytes = b.sizeBytes;
        this.sha256 = b.sha256;
        this.storageKey = b.storageKey;
        this.status = (b.status != null) ? b.status : FileStatus.PENDING;
        this.deleted = b.deleted;
        this.uploadedAt = (b.uploadedAt != null) ? b.uploadedAt : Instant.now();
        this.scannedAt = b.scannedAt;
    }

    public UUID getId() { return id; }
    public String getOwnerUserId() { return ownerUserId; }
    public String getFileName() { return fileName; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public String getSha256() { return sha256; }

    /** Internal only — never expose via DTO. */
    @JsonIgnore
    public String getStorageKey() { return storageKey; }

    public FileStatus getStatus() { return status; }
    public boolean isDeleted() { return deleted; }
    public Instant getUploadedAt() { return uploadedAt; }
    public Instant getScannedAt() { return scannedAt; }

    /** FILE-SCAN-001 — record scan outcome. */
    public void markScanResult(FileStatus result, Instant now) {
        if (result != FileStatus.READY && result != FileStatus.QUARANTINED) {
            throw new IllegalArgumentException(
                "scan result must be READY or QUARANTINED, got " + result);
        }
        this.status = result;
        this.scannedAt = now;
    }

    /** Soft delete — file row stays for audit + grace period. */
    public void softDelete(Instant now) {
        this.deleted = true;
        this.status = FileStatus.DELETED;
        this.scannedAt = (this.scannedAt != null) ? this.scannedAt : now;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private UUID id;
        private String ownerUserId;
        private String fileName;
        private String contentType;
        private long sizeBytes;
        private String sha256;
        private String storageKey;
        private FileStatus status;
        private boolean deleted = false;
        private Instant uploadedAt;
        private Instant scannedAt;

        public Builder id(UUID v) { this.id = v; return this; }
        public Builder ownerUserId(String v) { this.ownerUserId = v; return this; }
        public Builder fileName(String v) { this.fileName = v; return this; }
        public Builder contentType(String v) { this.contentType = v; return this; }
        public Builder sizeBytes(long v) { this.sizeBytes = v; return this; }
        public Builder sha256(String v) { this.sha256 = v; return this; }
        public Builder storageKey(String v) { this.storageKey = v; return this; }
        public Builder status(FileStatus v) { this.status = v; return this; }
        public Builder deleted(boolean v) { this.deleted = v; return this; }
        public Builder uploadedAt(Instant v) { this.uploadedAt = v; return this; }
        public Builder scannedAt(Instant v) { this.scannedAt = v; return this; }

        public StoredFile build() { return new StoredFile(this); }
    }
}
