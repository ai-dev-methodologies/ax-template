package com.ax.template.authblueprint.register;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.UUID;

public interface RegisterReadingRepository extends JpaRepository<RegisterReading, UUID> {

    /** Append-only read history of one register, in causal (sequence) order. Paginated. */
    Page<RegisterReading> findByRegisterIdOrderBySequenceNoAsc(UUID registerId, Pageable pageable);

    /** Current max per-register sequence (0 if none) — next is this + 1, allocated under the register lock. */
    @Query("SELECT COALESCE(MAX(r.sequenceNo), 0) FROM RegisterReading r WHERE r.registerId = :registerId")
    long maxSequence(@Param("registerId") UUID registerId);

    /** REG-DELTA-001 reconciliation — total consumption is Σ deltas across all reads of the register. */
    @Query("SELECT COALESCE(SUM(r.delta), 0) FROM RegisterReading r WHERE r.registerId = :registerId")
    BigDecimal sumDelta(@Param("registerId") UUID registerId);
}
