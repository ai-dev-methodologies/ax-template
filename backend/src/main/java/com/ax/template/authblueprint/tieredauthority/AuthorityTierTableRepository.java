package com.ax.template.authblueprint.tieredauthority;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthorityTierTableRepository extends JpaRepository<AuthorityTierTable, UUID> {

    Optional<AuthorityTierTable> findTopByOrderByTableVersionDesc();

    // ── through-root member access (HG-AGG-REPO — bands/decisions own no repository) ──

    @Query("SELECT b FROM AuthorityTierBand b WHERE b.tableId = :tableId ORDER BY b.orderIndex ASC")
    List<AuthorityTierBand> findBands(@Param("tableId") UUID tableId);

    @Query("SELECT d FROM TieredDecisionRecord d WHERE d.id = :id")
    Optional<TieredDecisionRecord> findDecision(@Param("id") UUID id);
}
