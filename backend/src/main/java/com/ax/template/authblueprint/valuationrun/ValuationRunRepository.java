package com.ax.template.authblueprint.valuationrun;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** NO delete method is declared anywhere in this domain — a run is immutable, never removed. */
public interface ValuationRunRepository extends JpaRepository<ValuationRun, UUID> {

    Optional<ValuationRun> findTopBySubjectIdOrderByRunVersionDesc(UUID subjectId);

    Optional<ValuationRun> findBySubjectIdAndRunVersion(UUID subjectId, int runVersion);

    /** VALRUN-ASOF-001 — the run with the GREATEST as-of ≤ T (point-in-time read). */
    @Query("SELECT r FROM ValuationRun r WHERE r.subjectId = :subjectId AND r.asOf <= :asOf "
        + "ORDER BY r.asOf DESC, r.runVersion DESC")
    Optional<ValuationRun> findAsOf(@Param("subjectId") UUID subjectId, @Param("asOf") Instant asOf,
                                    Limit limit);

    /** VALRUN-FALLBACK-001 — the run with the GREATEST as-of ≤ T, scoped to ONE named source. */
    @Query("SELECT r FROM ValuationRun r WHERE r.subjectId = :subjectId AND r.sourceRef = :sourceRef "
        + "AND r.asOf <= :asOf ORDER BY r.asOf DESC, r.runVersion DESC")
    Optional<ValuationRun> findAsOfBySource(@Param("subjectId") UUID subjectId,
                                            @Param("sourceRef") String sourceRef,
                                            @Param("asOf") Instant asOf, Limit limit);

    /** Full version history, bounded by the subject predicate (a small per-subject set). */
    List<ValuationRun> findBySubjectIdOrderByRunVersionAsc(UUID subjectId);

    // ── through-root member reads (HG-AGG-REPO — ValuationOutput owns no repository) ──

    @Query("SELECT o FROM ValuationOutput o WHERE o.runId = :runId ORDER BY o.positionRef ASC")
    List<ValuationOutput> findOutputs(@Param("runId") UUID runId);

    /** VALRUN-FANOUT-001 — independent conservation derivation: a repo SUM over the persisted
     *  output rows, a DIFFERENT code path than the in-memory fold that produced total_value. */
    @Query("SELECT COALESCE(SUM(o.positionValue), 0) FROM ValuationOutput o WHERE o.runId = :runId")
    BigDecimal sumOutputValues(@Param("runId") UUID runId);
}
