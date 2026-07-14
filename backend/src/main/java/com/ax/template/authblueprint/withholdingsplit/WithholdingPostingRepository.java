package com.ax.template.authblueprint.withholdingsplit;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** NO delete method anywhere — postings and legs are append-only (WHT-IMMUTABLE-004). */
public interface WithholdingPostingRepository extends JpaRepository<WithholdingPosting, UUID> {

    /** WHT-REMIT-003 — how many postings fall in a period (used to freeze postingCount on collection). */
    long countByPeriod(String period);

    /** WHT-REMIT-003 — Σ(WITHHOLDING legs) for every posting in a period, at collection time. */
    @Query("SELECT COALESCE(SUM(l.amount), 0) FROM WithholdingLeg l, WithholdingPosting p"
        + " WHERE l.postingId = p.id AND p.period = :period AND l.legType = com.ax.template.authblueprint.withholdingsplit.LegType.WITHHOLDING")
    BigDecimal sumWithholdingForPeriod(@Param("period") String period);

    // ── through-root member reads (HG-AGG-REPO — WithholdingLeg owns no repository) ──

    @Query("SELECT l FROM WithholdingLeg l WHERE l.postingId = :postingId ORDER BY l.legType ASC")
    List<WithholdingLeg> findLegs(@Param("postingId") UUID postingId, Pageable pageable);
}
