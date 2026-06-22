package com.ax.template.authblueprint.dunning;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** NO delete method is declared anywhere in this domain — a dunning case is closed, never removed. */
public interface DunningCaseRepository extends JpaRepository<DunningCase, UUID> {

    /** DUNNING-CONCURRENT-001 — the case row serializes the read-stage / write-next-stage advance. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM DunningCase c WHERE c.id = :id")
    Optional<DunningCase> findByIdForUpdate(@Param("id") UUID id);

    // ── through-root member reads (HG-AGG-REPO — DunningStageTransition owns no repository) ──

    @Query("SELECT t FROM DunningStageTransition t WHERE t.caseId = :caseId ORDER BY t.occurredAt ASC, t.stage ASC")
    List<DunningStageTransition> findTransitions(@Param("caseId") UUID caseId);
}
