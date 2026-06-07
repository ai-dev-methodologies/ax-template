package com.ax.template.authblueprint.copresence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ConflictRuleRepository extends JpaRepository<ConflictRule, UUID> {

    boolean existsByConceptAAndConceptB(String conceptA, String conceptB);

    /** GATE-SET-EVAL-001 — look up a conflict for an UNORDERED concept pair (either storage order). */
    @Query("SELECT r FROM ConflictRule r WHERE (r.conceptA = :x AND r.conceptB = :y) "
        + "OR (r.conceptA = :y AND r.conceptB = :x)")
    Optional<ConflictRule> findConflict(@Param("x") String x, @Param("y") String y);
}
