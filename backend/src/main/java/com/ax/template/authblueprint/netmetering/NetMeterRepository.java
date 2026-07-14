package com.ax.template.authblueprint.netmetering;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NetMeterRepository extends JpaRepository<NetMeter, UUID> {

    Optional<NetMeter> findByMeterKey(String meterKey);

    boolean existsByMeterKey(String meterKey);

    /** NETM-CONCURRENT-001 — lock the METER row (not per-direction) so ALL concurrent appends serialize:
     *  each direction delta is computed against the freshly-committed cumulative, never a stale one, and
     *  the derived net is never computed from a half-applied pair (CWE-362). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM NetMeter m WHERE m.meterKey = :meterKey")
    Optional<NetMeter> findByMeterKeyForUpdate(@Param("meterKey") String meterKey);

    // ── through-root member reads (HG-AGG-REPO — NetMeterReading / NetMeterPeriod own no repository) ──

    /** Max sequence for one (meter, direction) (0 if none) — allocated under the meter's row lock. */
    @Query("SELECT COALESCE(MAX(r.sequenceNo), 0) FROM NetMeterReading r "
        + "WHERE r.meterId = :meterId AND r.direction = :direction")
    long maxSequence(@Param("meterId") UUID meterId, @Param("direction") MeterDirection direction);

    /** Σ import deltas — the independent recompute of cumulative import (NETM-NET-001 cross-check). */
    @Query("SELECT COALESCE(SUM(r.delta), 0) FROM NetMeterReading r "
        + "WHERE r.meterId = :meterId AND r.direction = com.ax.template.authblueprint.netmetering.MeterDirection.IMPORT")
    BigDecimal sumImportDelta(@Param("meterId") UUID meterId);

    /** Σ export deltas — the independent recompute of cumulative export (NETM-NET-001 cross-check). */
    @Query("SELECT COALESCE(SUM(r.delta), 0) FROM NetMeterReading r "
        + "WHERE r.meterId = :meterId AND r.direction = com.ax.template.authblueprint.netmetering.MeterDirection.EXPORT")
    BigDecimal sumExportDelta(@Param("meterId") UUID meterId);

    /** Max period sequence for one meter (0 if none) — period boundaries move strictly forward. */
    @Query("SELECT COALESCE(MAX(p.sequenceNo), 0) FROM NetMeterPeriod p WHERE p.meterId = :meterId")
    long maxPeriodSequence(@Param("meterId") UUID meterId);

    /** NETM-RATE-001 — independent recompute of THIS period's import delta from the immutable reading chain
     *  (never trusted by-construction): Σ deltas for readings strictly after the prior boundary, up to and
     *  including the closing boundary. */
    @Query("SELECT COALESCE(SUM(r.delta), 0) FROM NetMeterReading r WHERE r.meterId = :meterId "
        + "AND r.direction = com.ax.template.authblueprint.netmetering.MeterDirection.IMPORT "
        + "AND r.effectiveAt > :after AND r.effectiveAt <= :through")
    BigDecimal sumImportDeltaInRange(@Param("meterId") UUID meterId, @Param("after") Instant after,
                                     @Param("through") Instant through);

    /** NETM-RATE-001 — same independent recompute, EXPORT direction. */
    @Query("SELECT COALESCE(SUM(r.delta), 0) FROM NetMeterReading r WHERE r.meterId = :meterId "
        + "AND r.direction = com.ax.template.authblueprint.netmetering.MeterDirection.EXPORT "
        + "AND r.effectiveAt > :after AND r.effectiveAt <= :through")
    BigDecimal sumExportDeltaInRange(@Param("meterId") UUID meterId, @Param("after") Instant after,
                                     @Param("through") Instant through);

    /** Paginated reading chain in causal (recorded) order across both directions. */
    @Query("SELECT r FROM NetMeterReading r WHERE r.meterId = :meterId ORDER BY r.recordedAt ASC, r.sequenceNo ASC")
    Page<NetMeterReading> findReadingsPage(@Param("meterId") UUID meterId, Pageable pageable);

    /** Paginated closed-period snapshots in boundary order. */
    @Query("SELECT p FROM NetMeterPeriod p WHERE p.meterId = :meterId ORDER BY p.sequenceNo ASC")
    Page<NetMeterPeriod> findPeriodsPage(@Param("meterId") UUID meterId, Pageable pageable);
}
