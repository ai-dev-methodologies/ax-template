package com.ax.template.authblueprint.scheduledtask;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TaskLockRepository extends JpaRepository<TaskLock, String> {

    /**
     * SCHED-IDEMPOTENT-001 (BACKLOG P2-48) — the ONLY read of a lock row on the acquire/release
     * paths, and it is deliberately a pessimistic {@code SELECT ... FOR UPDATE}: the row lock
     * serializes the read-staleness / write-takeover sequence in
     * {@link DatabaseAdvisoryLock#tryAcquire} so concurrent acquirers converge on exactly one
     * winner (CWE-362). A plain non-locking derived finder used to live here; it was DELETED
     * rather than kept alongside this one so the read-then-write race cannot be reintroduced by
     * calling the cheaper method. Precedent: {@code ReconciliationRunRepository#findItemByIdForUpdate}.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM TaskLock l WHERE l.taskName = :taskName")
    Optional<TaskLock> findByTaskNameForUpdate(@Param("taskName") String taskName);
}
