package com.ax.template.authblueprint.thresholdterminal;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ThresholdRegisterRepository extends JpaRepository<ThresholdRegister, UUID> {

    Optional<ThresholdRegister> findByScopeKey(String scopeKey);

    boolean existsByScopeKey(String scopeKey);

    /** TTD-CONCURRENT-001 — the register row is the single serialization point for BOTH write-paths
     *  (accrue and use): exactly one accrual is the crossing, and no use is admitted concurrently
     *  with it (CWE-362). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ThresholdRegister r WHERE r.scopeKey = :scopeKey")
    Optional<ThresholdRegister> findByScopeKeyForUpdate(@Param("scopeKey") String scopeKey);
}
