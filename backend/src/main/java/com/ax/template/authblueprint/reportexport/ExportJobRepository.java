package com.ax.template.authblueprint.reportexport;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence for {@link ExportJob}. Every owner-scoped finder filters by
 * {@code ownerUserId} to enforce EXPORT-AUTHZ-002 / 003.
 */
@Repository
public interface ExportJobRepository extends JpaRepository<ExportJob, UUID> {

    Optional<ExportJob> findByIdAndOwnerUserId(UUID id, String ownerUserId);

    Page<ExportJob> findByOwnerUserIdOrderByCreatedAtDesc(String ownerUserId, Pageable pageable);

    /** Worker pickup query — oldest PENDING first. Caller adds a {@link Pageable} for limiting. */
    List<ExportJob> findByStatusOrderByCreatedAtAsc(ExportJobStatus status, Pageable pageable);
}
