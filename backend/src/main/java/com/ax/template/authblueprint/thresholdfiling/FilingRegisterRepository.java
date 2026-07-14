package com.ax.template.authblueprint.thresholdfiling;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface FilingRegisterRepository extends JpaRepository<FilingRegister, UUID> {

    Optional<FilingRegister> findBySubjectKey(String subjectKey);

    boolean existsBySubjectKey(String subjectKey);

    /** TFO-TRIGGER-001 — the register row is the single serialization point for the crossing
     *  accrual, a late accrual, and the ack path. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM FilingRegister r WHERE r.subjectKey = :subjectKey")
    Optional<FilingRegister> findBySubjectKeyForUpdate(@Param("subjectKey") String subjectKey);

    // ── through-root member reads (HG-AGG-REPO — FilingObligation owns no repository) ──

    @Query("SELECT f FROM FilingObligation f WHERE f.registerId = :registerId")
    Optional<FilingObligation> findFilingObligation(@Param("registerId") UUID registerId);

    /** TFO-DEADLINE-001 — fail-closed visibility: every still-OPEN, past-due filing, across ALL
     *  registers, oldest due date first. Nothing is ever silently filtered out for being late —
     *  lateness is exactly the condition this query surfaces. */
    @Query("SELECT f FROM FilingObligation f WHERE f.status = 'OPEN' AND f.dueAt < :now ORDER BY f.dueAt ASC")
    Page<FilingObligation> findOverdueOpen(@Param("now") Instant now, Pageable pageable);
}
