package com.ax.template.authblueprint.auditlog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository contract for {@link AuditLog}.
 * <p>
 * Trace: AUDIT-RECORD-002 — immutability. This repository extends only the
 * narrow {@link Repository} marker (not {@code CrudRepository} or
 * {@code JpaRepository}) so {@code deleteById} / {@code save(updated)} are NOT
 * inherited. Only the explicit methods declared below are exposed.
 *
 * <p>The single bulk delete method exists for the scheduled retention job
 * (AUDIT-RETENTION-001..003); regular business operations cannot reach an
 * {@code AuditLog} delete path. The retention job is the sole privileged caller.
 */
public interface AuditLogRepository
        extends Repository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {

    /** Persist a new entry. Update paths are blocked by {@code @Column(updatable=false)}. */
    AuditLog save(AuditLog entry);

    Optional<AuditLog> findById(UUID id);

    long count();

    Page<AuditLog> findAll(Pageable pageable);

    /**
     * Scheduled retention purge — the only delete path in the catalog.
     * Returns the number of rows deleted.
     *
     * <p>Trace: AUDIT-RETENTION-001.
     */
    @Modifying
    @Transactional
    @Query("delete from AuditLog a where a.timestamp < :cutoff")
    int deleteByTimestampBefore(@Param("cutoff") Instant cutoff);
}
