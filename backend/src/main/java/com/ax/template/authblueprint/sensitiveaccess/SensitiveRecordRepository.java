package com.ax.template.authblueprint.sensitiveaccess;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/** NO delete method is declared anywhere in this domain — a sensitive record is closed, never removed. */
public interface SensitiveRecordRepository extends JpaRepository<SensitiveRecord, UUID> {

    // ── through-root member reads (HG-AGG-REPO — SensitiveAccessLog owns no repository) ──

    /** SENSITIVE-QUERY-001 — the append-only access trail for one record, in occurredAt order.
     *  Bounded by Pageable (ArchitectureUnboundedRepositoryListTest — no raw unbounded List). */
    @Query("SELECT a FROM SensitiveAccessLog a WHERE a.recordId = :recordId"
        + " ORDER BY a.occurredAt ASC, a.id ASC")
    List<SensitiveAccessLog> findAccessLog(@Param("recordId") UUID recordId, Pageable pageable);
}
