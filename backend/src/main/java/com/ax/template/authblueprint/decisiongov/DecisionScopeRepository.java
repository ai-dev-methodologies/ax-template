package com.ax.template.authblueprint.decisiongov;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface DecisionScopeRepository extends JpaRepository<DecisionScope, UUID> {

    Optional<DecisionScope> findByScopeKey(String scopeKey);

    boolean existsByScopeKey(String scopeKey);

    /** DG-CONCURRENT-001 — the scope row is the single serialization point for the version counter. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM DecisionScope s WHERE s.scopeKey = :scopeKey")
    Optional<DecisionScope> findByScopeKeyForUpdate(@Param("scopeKey") String scopeKey);

    // ── through-root member reads (HG-AGG-REPO — DecisionVersion owns no repository) ──

    /** One determination of one scope (e.g. the latest, or the version an override deviates from). */
    @Query("SELECT v FROM DecisionVersion v WHERE v.scopeId = :scopeId AND v.versionNo = :versionNo")
    Optional<DecisionVersion> findVersion(@Param("scopeId") UUID scopeId, @Param("versionNo") int versionNo);

    /** The full chain in version order, paginated (DG-CHAIN-001). */
    @Query("SELECT v FROM DecisionVersion v WHERE v.scopeId = :scopeId ORDER BY v.versionNo ASC")
    Page<DecisionVersion> findVersionsPage(@Param("scopeId") UUID scopeId, Pageable pageable);
}
