package com.ax.template.authblueprint.mandate;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** NO delete method is declared anywhere in this domain — a mandate and its children are
 *  recorded forever (audit posture); they are resolved, never removed. */
public interface MandateRepository extends JpaRepository<Mandate, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Mandate m WHERE m.id = :id")
    Optional<Mandate> findByIdForUpdate(@Param("id") UUID id);

    // ── through-root member reads (HG-AGG-REPO — MandateTask / MandateCheck own no repository) ──

    /** MANDATE-CONCURRENT-001 — the task row serializes the explicit-complete / deemed-sweep race. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM MandateTask t WHERE t.id = :taskId")
    Optional<MandateTask> findTaskByIdForUpdate(@Param("taskId") UUID taskId);

    @Query("SELECT t FROM MandateTask t WHERE t.mandateId = :mandateId ORDER BY t.taskSeq ASC")
    List<MandateTask> findTasks(@Param("mandateId") UUID mandateId);

    /** MANDATE-FANOUT-001 — the DERIVED completion recall: count terminal children (state <> PENDING). */
    @Query("SELECT COUNT(t) FROM MandateTask t WHERE t.mandateId = :mandateId AND t.state <> com.ax.template.authblueprint.mandate.MandateTaskState.PENDING")
    long countTerminalTasks(@Param("mandateId") UUID mandateId);

    @Query("SELECT c FROM MandateCheck c WHERE c.mandateId = :mandateId ORDER BY c.checkKey ASC")
    List<MandateCheck> findChecks(@Param("mandateId") UUID mandateId);

    @Query("SELECT c FROM MandateCheck c WHERE c.mandateId = :mandateId AND c.checkKey = :checkKey")
    Optional<MandateCheck> findCheck(@Param("mandateId") UUID mandateId, @Param("checkKey") String checkKey);

    /** Deemed-sweep worklist (MANDATE-DEEMED-001) — PENDING tasks whose deadline has passed,
     *  oldest first, bounded. */
    @Query("SELECT t.id FROM MandateTask t WHERE t.state = com.ax.template.authblueprint.mandate.MandateTaskState.PENDING AND t.deemedDeadline <= :now ORDER BY t.deemedDeadline ASC")
    Page<UUID> findOverduePendingTaskIds(@Param("now") Instant now, Pageable pageable);
}
