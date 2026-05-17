/**
 * @ax-template-meta
 * template_id: backend/email-outbox/EmailOutboxRepository
 * layer: backend-domain
 * domain: email-outbox
 * anchors_rule: testing-archunit-repository-shape.md (PRACTICES-TEST-004)
 * provenance_class: external_canonical
 * evidence:
 *   - source_type: external
 *     citation: "Spring Data JPA Reference — Defining Repository Interfaces"
 *     url: "https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html"
 *   - source_type: external
 *     citation: "Transactional Outbox Pattern — microservices.io"
 *     url: "https://microservices.io/patterns/data/transactional-outbox.html"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   EmailOutboxRepository extends BaseRepository for soft-delete support.
 *   Key query: findAllPendingAndRetry filters by status and nextAttemptAt for processQueue().
 */
package com.example.app.emailoutbox;

import com.example.app.repositories.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for EmailOutbox entities.
 *
 * <p>Extends {@link BaseRepository} for soft-delete support.
 *
 * <p>Core query: {@link #findAllPendingAndRetry} — used by processQueue() to
 * retrieve entries ready for a send attempt. The query respects:
 * <ul>
 *   <li>status IN (PENDING, RETRY)
 *   <li>nextAttemptAt IS NULL OR nextAttemptAt &lt;= now
 *   <li>deleted = false
 * </ul>
 */
public interface EmailOutboxRepository extends BaseRepository<EmailOutbox, UUID> {

    /**
     * Returns all outbox entries eligible for a send attempt in the current cycle.
     *
     * <p>An entry is eligible if:
     * <ul>
     *   <li>status is PENDING (first attempt) or RETRY (retry after backoff)
     *   <li>nextAttemptAt is null (PENDING) or <= now (backoff period elapsed)
     * </ul>
     *
     * @param now current instant — used to filter RETRY entries whose backoff has elapsed
     */
    @Query("""
        SELECT e FROM EmailOutbox e
        WHERE e.deleted = false
          AND e.status IN ('PENDING', 'RETRY')
          AND (e.nextAttemptAt IS NULL OR e.nextAttemptAt <= :now)
        ORDER BY e.createdAt ASC
        """)
    List<EmailOutbox> findAllPendingAndRetry(@Param("now") Instant now);

    /**
     * Returns paginated outbox entries for the admin list endpoint.
     * Supports optional status filter; ALL = no status filter.
     *
     * @param status null = ALL statuses; non-null = filter by specific status
     */
    @Query("""
        SELECT e FROM EmailOutbox e
        WHERE e.deleted = false
          AND (:status IS NULL OR e.status = :status)
        ORDER BY e.createdAt DESC
        """)
    Page<EmailOutbox> findAllForAdmin(
            @Param("status") EmailOutbox.EmailOutboxStatus status,
            Pageable pageable);

    /**
     * Counts entries in DLQ — useful for admin dashboard metrics.
     */
    @Query("""
        SELECT COUNT(e) FROM EmailOutbox e
        WHERE e.deleted = false
          AND e.status = 'DLQ'
        """)
    long countDlq();
}
