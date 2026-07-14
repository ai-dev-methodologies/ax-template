package com.ax.template.authblueprint.piecewisedeadband;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeadbandConfigRepository extends JpaRepository<DeadbandConfig, UUID> {

    Optional<DeadbandConfig> findByConfigKey(String configKey);

    boolean existsByConfigKey(String configKey);

    // ── through-root member reads (HG-AGG-REPO — DeadbandSegment / DeadbandEvaluation own no repository) ──

    /** PWDB-EVAL-001 — the tiling, in covering order. */
    @Query("SELECT s FROM DeadbandSegment s WHERE s.configId = :configId ORDER BY s.ordinal ASC")
    List<DeadbandSegment> findSegments(@Param("configId") UUID configId);

    /** PWDB-IMMUTABLE-001 — idempotent replay lookup. */
    @Query("SELECT e FROM DeadbandEvaluation e WHERE e.configId = :configId AND e.idempotencyKey = :key")
    Optional<DeadbandEvaluation> findEvaluationByIdempotencyKey(@Param("configId") UUID configId,
                                                                @Param("key") String idempotencyKey);

    @Query("SELECT COALESCE(MAX(e.sequenceNo), 0) FROM DeadbandEvaluation e WHERE e.configId = :configId")
    long maxEvaluationSequence(@Param("configId") UUID configId);

    @Query("SELECT e FROM DeadbandEvaluation e WHERE e.configId = :configId ORDER BY e.sequenceNo ASC")
    Page<DeadbandEvaluation> findEvaluationsPage(@Param("configId") UUID configId, Pageable pageable);
}
