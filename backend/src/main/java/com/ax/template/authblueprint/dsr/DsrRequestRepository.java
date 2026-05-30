package com.ax.template.authblueprint.dsr;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence for {@link DsrRequest}.
 *
 * <p>Every collection-returning finder takes a {@link Pageable} (no raw unbounded
 * {@code List} return — {@code ArchitectureUnboundedRepositoryListTest}).
 */
public interface DsrRequestRepository extends JpaRepository<DsrRequest, UUID> {

    /** Owner-scoped single-row lookup — backs the IDOR-safe 404 (DSR-SLA-001). */
    Optional<DsrRequest> findByIdAndSubjectId(UUID id, String subjectId);

    /** Is there a non-terminal request of this type already in flight for the subject? (DSR-ACCESS-001) */
    Optional<DsrRequest> findFirstBySubjectIdAndTypeAndStatusNot(
        String subjectId, DsrRequestType type, DsrRequestStatus status);

    /** All requests for one subject, newest first. */
    Page<DsrRequest> findBySubjectIdOrderByReceivedAtDesc(String subjectId, Pageable pageable);

    /** Open requests at/over their due date — driven by the SLA sweep (DSR-SLA-001). */
    Page<DsrRequest> findByStatusNotAndDueAtLessThanEqual(
        DsrRequestStatus status, Instant cutoff, Pageable pageable);
}
