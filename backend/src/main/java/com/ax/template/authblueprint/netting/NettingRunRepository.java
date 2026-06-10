package com.ax.template.authblueprint.netting;

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

public interface NettingRunRepository extends JpaRepository<NettingRun, UUID> {

    Optional<NettingRun> findByRunKey(String runKey);

    boolean existsByRunKey(String runKey);

    /** NET-ONCE-001 — lock the run so concurrent reductions serialize: the reduction runs exactly once
     *  (a second sees NETTED and is rejected), and obligations cannot be added during a reduction. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM NettingRun r WHERE r.runKey = :runKey")
    Optional<NettingRun> findByRunKeyForUpdate(@Param("runKey") String runKey);

    // ── through-root member reads (HG-AGG-REPO — GrossObligation/NetPosition own no repository) ──

    /** Every gross row of one run — each row read exactly once by the netting reduction. */
    @Query("SELECT o FROM GrossObligation o WHERE o.runId = :runId")
    List<GrossObligation> findObligations(@Param("runId") UUID runId);

    /** Paginated gross listing for the API. */
    @Query("SELECT o FROM GrossObligation o WHERE o.runId = :runId ORDER BY o.createdAt ASC")
    Page<GrossObligation> findObligationsPage(
        @Param("runId") UUID runId, Pageable pageable);

    /** INDEPENDENT per-node cross-check (repo SUM — a different code path than the in-memory
     *  reduction; a conservation check that holds by construction is a FALSE backstop). */
    @Query("SELECT COALESCE(SUM(o.amount), 0) FROM GrossObligation o WHERE o.runId = :runId AND o.toMember = :member")
    BigDecimal sumOwedTo(@Param("runId") UUID runId, @Param("member") String member);

    @Query("SELECT COALESCE(SUM(o.amount), 0) FROM GrossObligation o WHERE o.runId = :runId AND o.fromMember = :member")
    BigDecimal sumOwedBy(@Param("runId") UUID runId, @Param("member") String member);

    /** Set-wide conservation probe (SUM over computed net positions). */
    @Query("SELECT COALESCE(SUM(p.netAmount), 0) FROM NetPosition p WHERE p.runId = :runId")
    BigDecimal sumNet(@Param("runId") UUID runId);

    /** Paginated position listing for the API. */
    @Query("SELECT p FROM NetPosition p WHERE p.runId = :runId ORDER BY p.member ASC")
    Page<NetPosition> findPositionsPage(
        @Param("runId") UUID runId, Pageable pageable);
}