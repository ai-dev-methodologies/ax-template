package com.ax.template.authblueprint.mececlassification;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** NO delete method — a scheme and its rules are append-only (MECE-EXHAUSTIVE-002). */
public interface ClassificationSchemeRepository extends JpaRepository<ClassificationScheme, UUID> {

    Optional<ClassificationScheme> findBySchemeKey(String schemeKey);

    boolean existsBySchemeKey(String schemeKey);

    // ── through-root member reads (HG-AGG-REPO — ClassificationRule owns no repository) ──

    @Query("SELECT r FROM ClassificationRule r WHERE r.schemeKey = :schemeKey ORDER BY r.createdAt ASC")
    List<ClassificationRule> findRules(@Param("schemeKey") String schemeKey, Pageable pageable);

    @Query("SELECT r FROM ClassificationRule r WHERE r.schemeKey = :schemeKey AND r.matchValue = :matchValue")
    Optional<ClassificationRule> findRuleByMatch(@Param("schemeKey") String schemeKey, @Param("matchValue") String matchValue);
}
