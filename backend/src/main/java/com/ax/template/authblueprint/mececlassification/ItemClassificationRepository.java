package com.ax.template.authblueprint.mececlassification;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** NO delete method — a classification identity and its moves are append-only (MECE-RECLASS-003). */
public interface ItemClassificationRepository extends JpaRepository<ItemClassification, UUID> {

    Optional<ItemClassification> findBySchemeKeyAndItemRef(String schemeKey, String itemRef);

    boolean existsBySchemeKeyAndItemRef(String schemeKey, String itemRef);

    // ── through-root member reads (HG-AGG-REPO — ClassificationMove owns no repository) ──

    @Query("SELECT m FROM ClassificationMove m WHERE m.classificationId = :classificationId ORDER BY m.movedAt ASC")
    List<ClassificationMove> findMoves(@Param("classificationId") UUID classificationId, Pageable pageable);

    /** MECE-RECLASS-003 — the latest move IS the current category; no separately-mutable field exists. */
    @Query("SELECT m FROM ClassificationMove m WHERE m.classificationId = :classificationId ORDER BY m.movedAt DESC")
    List<ClassificationMove> findMovesLatestFirst(@Param("classificationId") UUID classificationId, Pageable pageable);

    /** MECE-EXHAUSTIVE-002 — the residual (or any) category's CURRENT population, derived from each
     *  classification's latest move — a visible signal, not a silent sink. */
    @Query("SELECT COUNT(m) FROM ClassificationMove m, ItemClassification ic"
        + " WHERE ic.id = m.classificationId AND ic.schemeKey = :schemeKey AND m.toCategory = :category"
        + " AND m.movedAt = (SELECT MAX(m2.movedAt) FROM ClassificationMove m2 WHERE m2.classificationId = m.classificationId)")
    long countCurrentByCategory(@Param("schemeKey") String schemeKey, @Param("category") String category);
}
