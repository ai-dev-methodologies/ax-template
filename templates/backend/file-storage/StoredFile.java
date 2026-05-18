// @ax-template-meta: template_id=backend/file-storage/StoredFile layer=backend domain=file-storage
// evidence: FILE-AUTHZ-002 (ownerUserId isolation), FILE-UPLOAD-003 (sanitized name),
//           FILE-SEC-001 (storageKey excluded from DTO), FILE-SCAN-001 (FileStatus lifecycle)
package com.ax.template.authblueprint.filestorage;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import java.time.Instant;
import java.util.UUID;

/**
 * StoredFile — JPA entity representing an uploaded file.
 *
 * <p>Design notes:
 * <ul>
 *   <li>storageKey is the internal storage identifier (S3 key / filesystem path).
 *       It is NEVER included in API responses (FILE-SEC-001, FILE-SEC-002).
 *       Controlled by StoredFileMapper which maps to StoredFileDto (no storageKey field).</li>
 *   <li>fileName is the sanitized display name (original filename, traversal-stripped).
 *       See FilenameSanitizer for sanitization logic (FILE-UPLOAD-003).</li>
 *   <li>ownerUserId is set from the authenticated principal at upload time.
 *       All service queries filter by ownerUserId = callerUserId (FILE-AUTHZ-002).</li>
 *   <li>status lifecycle: PENDING → READY | QUARANTINED. Deletion sets status → DELETED
 *       (soft delete). Hard deletion is performed by a scheduled cleanup job.</li>
 * </ul>
 */
@Entity
@SQLDelete(sql = "UPDATE stored_files SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
@Table(name = "stored_files", indexes = {
    @Index(name = "idx_stored_files_owner", columnList = "ownerUserId"),
    @Index(name = "idx_stored_files_status", columnList = "status"),
    @Index(name = "idx_stored_files_expires", columnList = "expiresAt"),
})
public class StoredFile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /** Authenticated user who uploaded the file. Never null. */
    @Column(nullable = false)
    private String ownerUserId;

    /**
     * Sanitized display filename — path traversal stripped, max 255 chars.
     * See FilenameSanitizer (FILE-UPLOAD-003).
     */
    @Column(nullable = false, length = 255)
    private String fileName;

    /** MIME content type as declared by the uploader (validated against allowlist). */
    @Column(nullable = false, length = 128)
    private String contentType;

    /** File size in bytes. Used for quota enforcement (FILE-QUOTA-001). */
    @Column(nullable = false)
    private long sizeBytes;

    /**
     * Internal storage identifier (S3 key / filesystem path).
     * MUST NOT be exposed in any API response (FILE-SEC-001, FILE-SEC-002).
     */
    @Column(nullable = false)
    private String storageKey;

    /**
     * Lifecycle status of the file.
     * PENDING → (scan) → READY | QUARANTINED → (cleanup) → DELETED
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FileStatus status;

    /** Optional human-readable description provided at upload time. */
    @Column(length = 500)
    private String description;

    /** Timestamp when the file was uploaded. Set at creation, never updated. */
    @Column(nullable = false, updatable = false)
    private Instant uploadedAt;

    /** Retention expiry. Null = keep indefinitely (until manual delete). */
    @Column
    private Instant expiresAt;

    /** Optimistic locking for concurrent status updates (e.g., scan callback races). */
    @Version
    private Long version;

    // ─── Lifecycle ──────────────────────────────────────────────────────────

    @PrePersist
    void onPrePersist() {
        if (uploadedAt == null) {
            uploadedAt = Instant.now();
        }
        if (status == null) {
            status = FileStatus.PENDING;
        }
    }

    // ─── Getters / setters ──────────────────────────────────────────────────

    public UUID getId() { return id; }

    public String getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(String ownerUserId) { this.ownerUserId = ownerUserId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }

    /** Returns the internal storage key. Do NOT expose via API. */
    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }

    public FileStatus getStatus() { return status; }
    public void setStatus(FileStatus status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Instant getUploadedAt() { return uploadedAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Long getVersion() { return version; }

    /** Soft-delete timestamp. NULL = active. Set by @SQLDelete — do not set directly. */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    public Instant getDeletedAt() { return deletedAt; }
    public boolean isDeleted()    { return deletedAt != null; }
}
