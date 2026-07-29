/**
 * @ax-template-meta
 * template_id: backend/file-storage/FileStorageRepository
 * layer: backend
 * domain: file-storage
 * anchors_rule: testing-archunit-repository-shape.md
 * provenance_class: internal_design
 * evidence:
 *   - source_type: internal
 *     rationale: "Spring Data JPA repository for StoredFile extending JpaRepository — the shape the anchored ArchUnit rule enforces. Realises specs/file-storage-l0.yaml#FILE-AUTHZ-002 (owner-scoped finders for IDOR safety) and #FILE-QUOTA-001 (sumSizeByOwner)."
 */
package com.ax.template.authblueprint.filestorage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * FileStorageRepository — Spring Data JPA repository for StoredFile.
 *
 * <p>All query methods that return file data filter by ownerUserId to enforce
 * per-user isolation (FILE-AUTHZ-002). Queries that do not filter by owner
 * are restricted to admin/cleanup operations.
 */
@Repository
public interface FileStorageRepository extends JpaRepository<StoredFile, UUID> {

    /**
     * Find a file by ID for a specific owner (FILE-AUTHZ-002 — IDOR protection).
     * Returns Optional.empty() if file belongs to a different user → 404 in service layer.
     */
    Optional<StoredFile> findByIdAndOwnerUserId(UUID id, String ownerUserId);

    /**
     * List files for a specific owner, excluding DELETED status by default.
     * Used by the list endpoint.
     */
    List<StoredFile> findByOwnerUserIdAndStatusNotOrderByUploadedAtDesc(
            String ownerUserId, FileStatus excludedStatus);

    /**
     * List files for a specific owner filtered by status.
     */
    List<StoredFile> findByOwnerUserIdAndStatusOrderByUploadedAtDesc(
            String ownerUserId, FileStatus status);

    /**
     * Sum of sizeBytes for active files owned by a user (FILE-QUOTA-001).
     * Counts PENDING and READY files; excludes QUARANTINED and DELETED.
     */
    @Query("""
            SELECT COALESCE(SUM(f.sizeBytes), 0)
            FROM StoredFile f
            WHERE f.ownerUserId = :ownerUserId
              AND f.status IN ('PENDING', 'READY')
            """)
    long sumActiveSizeBytesByOwner(@Param("ownerUserId") String ownerUserId);

    /**
     * Find files past their expiry date for retention cleanup (admin/scheduler only).
     */
    List<StoredFile> findByExpiresAtBeforeAndStatusNot(Instant cutoff, FileStatus excludedStatus);

    /**
     * Find all PENDING files older than a threshold (for stuck-scan recovery).
     */
    List<StoredFile> findByStatusAndUploadedAtBefore(FileStatus status, Instant threshold);
}
