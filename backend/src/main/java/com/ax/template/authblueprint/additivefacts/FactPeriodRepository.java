package com.ax.template.authblueprint.additivefacts;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FactPeriodRepository extends JpaRepository<FactPeriod, UUID> {

    /** The period row is the serialization point for every fact-add / close. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM FactPeriod p WHERE p.id = :id")
    Optional<FactPeriod> findByIdForUpdate(@Param("id") UUID id);

    // ── through-root member reads (HG-AGG-REPO — Fact / LateDeltaPosting own no repository) ──

    @Query("SELECT f FROM Fact f WHERE f.source = :source AND f.externalFactId = :externalFactId")
    Optional<Fact> findFactBySourceAndExternalId(@Param("source") String source,
                                                 @Param("externalFactId") String externalFactId);

    @Query("SELECT f FROM Fact f WHERE f.periodId = :periodId ORDER BY f.createdAt ASC")
    List<Fact> findFactsByPeriodId(@Param("periodId") UUID periodId);

    @Query("SELECT COALESCE(SUM(f.amount), 0) FROM Fact f WHERE f.periodId = :periodId")
    BigDecimal sumFactsForPeriod(@Param("periodId") UUID periodId);

    @Query("SELECT p FROM LateDeltaPosting p WHERE p.originPeriodId = :originPeriodId ORDER BY p.postedAt ASC")
    List<LateDeltaPosting> findPostingsByOrigin(@Param("originPeriodId") UUID originPeriodId);
}
