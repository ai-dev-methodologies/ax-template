package com.ax.template.authblueprint.transformation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransformationRunRepository extends JpaRepository<TransformationRun, UUID> {

    /** Legs of one run, ordered by role — through-root member read (HG-AGG-REPO); bounded (Pageable). */
    @Query(
        "SELECT l FROM TransformationLeg l WHERE l.runId = :runId ORDER BY l.role ASC")
    List<TransformationLeg> findLegsByRunId(
        @Param("runId") UUID runId,
        Pageable pageable);
}
