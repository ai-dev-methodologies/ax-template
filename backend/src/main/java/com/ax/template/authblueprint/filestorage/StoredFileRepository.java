package com.ax.template.authblueprint.filestorage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link StoredFile}.
 * <p>
 * All read paths must filter {@code ownerUserId} so callers cannot leak cross-user
 * rows (FILE-AUTHZ-002). Cross-user lookups return {@code Optional.empty()}
 * which the service translates to 404.
 */
public interface StoredFileRepository extends JpaRepository<StoredFile, UUID> {

    /** FILE-AUTHZ-002 — strict owner lookup. */
    Optional<StoredFile> findByIdAndOwnerUserIdAndDeletedFalse(UUID id, String ownerUserId);

    /**
     * FILE-QUOTA-001 — sum of bytes for the user across statuses that count
     * toward the quota (PENDING + READY per manifest#quota.count_statuses).
     */
    @Query("select coalesce(sum(s.sizeBytes), 0) from StoredFile s " +
        "where s.ownerUserId = :ownerUserId " +
        "and s.deleted = false " +
        "and s.status in (com.ax.template.authblueprint.filestorage.FileStatus.PENDING, " +
        "                 com.ax.template.authblueprint.filestorage.FileStatus.READY)")
    long sumQuotaBytesForOwner(@Param("ownerUserId") String ownerUserId);
}
