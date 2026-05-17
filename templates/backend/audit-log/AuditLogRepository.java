/**
 * @ax-template-meta
 * template_id: backend/audit-log/AuditLogRepository
 * layer: backend-domain
 * domain: audit-log
 * anchors_rule: specs/audit-log-l0.yaml#AUDIT-LIST-001
 *               specs/audit-log-l0.yaml#AUDIT-LIST-002
 *               specs/audit-log-l0.yaml#AUDIT-RETENTION-001
 *               specs/audit-log-l0.yaml#AUDIT-RECORD-002
 * provenance_class: external_canonical
 * evidence:
 *   - source_type: external
 *     citation: "Spring Data JPA Reference — JpaSpecificationExecutor for dynamic queries"
 *     url: "https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#specifications"
 *   - source_type: external
 *     citation: "Spring Data JPA Reference — Derived delete queries"
 *     url: "https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.query-methods.at-query"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   Exposes only save() + read + delete-by-timestamp. No deleteById or updateById
 *   to enforce immutability (AUDIT-RECORD-002).
 */
package com.example.app.auditlog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * AuditLogRepository — read/append-only repository for {@link AuditLog}.
 *
 * <p>Immutability (AUDIT-RECORD-002): Only {@code save()} and read methods are exposed.
 * {@code deleteById} is intentionally NOT overridden to prevent accidental deletion
 * of individual entries from application code; the only delete path is the time-based
 * bulk purge method below.
 *
 * <p>Filtering (AUDIT-LIST-002): Extends {@code JpaSpecificationExecutor} to support
 * dynamic multi-criteria queries via {@link AuditLogSpecifications}.
 *
 * <p>Retention (AUDIT-RETENTION-001): {@link #deleteByTimestampBefore(Instant)} is the
 * only delete path — called exclusively by the retention job.
 */
public interface AuditLogRepository
        extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {

    /**
     * Returns all audit logs matching the given pageable spec.
     *
     * <p>Use with {@link AuditLogSpecifications} for filtered queries (AUDIT-LIST-002).
     *
     * <pre>{@code
     * Specification<AuditLog> spec = AuditLogSpecifications.fromQuery(queryDto);
     * Page<AuditLog> page = repository.findAll(spec, pageable);
     * }</pre>
     */
    Page<AuditLog> findAll(org.springframework.data.jpa.domain.Specification<AuditLog> spec, Pageable pageable);

    /**
     * Bulk-deletes audit log entries older than the given cutoff.
     *
     * <p>Called by the retention job only. Returns the number of deleted rows
     * so the job can emit a WARN log if the count exceeds the warning threshold
     * (AUDIT-RETENTION-003).
     *
     * @param cutoff entries with timestamp strictly before this instant are deleted
     * @return number of rows deleted
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM AuditLog a WHERE a.timestamp < :cutoff")
    int deleteByTimestampBefore(Instant cutoff);
}
