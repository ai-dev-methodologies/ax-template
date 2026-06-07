package com.ax.template.authblueprint.netting;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface NettingRunRepository extends JpaRepository<NettingRun, UUID> {

    Optional<NettingRun> findByRunKey(String runKey);

    boolean existsByRunKey(String runKey);

    /** NET-ONCE-001 — lock the run so concurrent reductions serialize: the reduction runs exactly once
     *  (a second sees NETTED and is rejected), and obligations cannot be added during a reduction. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM NettingRun r WHERE r.runKey = :runKey")
    Optional<NettingRun> findByRunKeyForUpdate(@Param("runKey") String runKey);
}
