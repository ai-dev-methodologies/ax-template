package com.ax.template.authblueprint.netting;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface GrossObligationRepository extends JpaRepository<GrossObligation, UUID> {

    /** All gross obligations of a run — read once by the reduction (bounded by the run's input set). */
    List<GrossObligation> findByRunId(UUID runId);

    /** Paginated view for the API (never an unbounded list over the wire). */
    Page<GrossObligation> findByRunIdOrderByCreatedAtAsc(UUID runId, Pageable pageable);

    /** NET-PER-NODE-001 INDEPENDENT cross-check — Σ amounts owed TO a member (a different code path
     *  than the in-memory reduction, so a from/to swap or dropped row in the loop is detectable). */
    @Query("SELECT COALESCE(SUM(o.amount), 0) FROM GrossObligation o WHERE o.runId = :runId AND o.toMember = :member")
    BigDecimal sumOwedTo(@Param("runId") UUID runId, @Param("member") String member);

    /** NET-PER-NODE-001 INDEPENDENT cross-check — Σ amounts owed BY a member. */
    @Query("SELECT COALESCE(SUM(o.amount), 0) FROM GrossObligation o WHERE o.runId = :runId AND o.fromMember = :member")
    BigDecimal sumOwedBy(@Param("runId") UUID runId, @Param("member") String member);
}
