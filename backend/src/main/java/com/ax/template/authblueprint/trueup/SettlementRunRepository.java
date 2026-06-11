package com.ax.template.authblueprint.trueup;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SettlementRunRepository extends JpaRepository<SettlementRun, UUID> {

    Optional<SettlementRun> findTopByPeriodIdOrderByRunVersionDesc(UUID periodId);

    List<SettlementRun> findByPeriodIdOrderByRunVersionAsc(UUID periodId);

    // ── through-root member reads (HG-AGG-REPO — TrueUpPosting owns no repository) ──

    @Query("SELECT t FROM TrueUpPosting t WHERE t.sourcePeriodId = :periodId ORDER BY t.toRunVersion ASC")
    List<TrueUpPosting> findPostingsBySource(@Param("periodId") UUID periodId);

    /** Independent conservation derivation — repo SUM, a different code path than the postings walk. */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM TrueUpPosting t WHERE t.sourcePeriodId = :periodId")
    BigDecimal sumPostingsForSource(@Param("periodId") UUID periodId);
}
