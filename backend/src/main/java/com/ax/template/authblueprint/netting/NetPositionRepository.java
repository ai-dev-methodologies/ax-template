package com.ax.template.authblueprint.netting;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.UUID;

public interface NetPositionRepository extends JpaRepository<NetPosition, UUID> {

    Page<NetPosition> findByRunIdOrderByMemberAsc(UUID runId, Pageable pageable);

    /** NET-SETWIDE-ZERO-001 reconciliation — the sum of a run's net positions is exactly 0. */
    @Query("SELECT COALESCE(SUM(p.netAmount), 0) FROM NetPosition p WHERE p.runId = :runId")
    BigDecimal sumNet(@Param("runId") UUID runId);
}
