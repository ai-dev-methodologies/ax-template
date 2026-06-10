package com.ax.template.authblueprint.register;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RegisterRepository extends JpaRepository<Register, UUID> {

    Optional<Register> findByScopeKey(String scopeKey);

    boolean existsByScopeKey(String scopeKey);

    /** REG-CONCURRENT-001 — lock the register so concurrent appends serialize: each delta is computed
     *  against the freshly-committed anchor, never a stale one (no double-count / lost read). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Register r WHERE r.scopeKey = :scopeKey")
    Optional<Register> findByScopeKeyForUpdate(@Param("scopeKey") String scopeKey);

    // ── through-root member reads (HG-AGG-REPO — RegisterReading owns no repository) ──

    /** Current max sequence for one register (0 if none) — allocated under the register's row lock. */
    @Query("SELECT COALESCE(MAX(r.sequenceNo), 0) FROM RegisterReading r WHERE r.registerId = :registerId")
    long maxSequence(@Param("registerId") UUID registerId);

    /** Σ deltas — the billed quantity (REG-DELTA-001), robust across rollover/exchange. */
    @Query("SELECT COALESCE(SUM(r.delta), 0) FROM RegisterReading r WHERE r.registerId = :registerId")
    BigDecimal sumDelta(@Param("registerId") UUID registerId);

    /** Paginated reading chain in causal (sequence) order. */
    @Query("SELECT r FROM RegisterReading r WHERE r.registerId = :registerId ORDER BY r.sequenceNo ASC")
    Page<RegisterReading> findReadingsPage(
        @Param("registerId") UUID registerId, Pageable pageable);
}