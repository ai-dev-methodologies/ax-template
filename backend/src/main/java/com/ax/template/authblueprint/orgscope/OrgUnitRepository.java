package com.ax.template.authblueprint.orgscope;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Root repository for the OrgUnit aggregate. ScopeGrant rows are members (HG-AGG-REPO — no grant
 * repository): grant READS are root-JPQL methods here; grant WRITES go through common/MemberWriter.
 * NO delete method is declared anywhere in this domain — the tree is append-structured.
 */
public interface OrgUnitRepository extends JpaRepository<OrgUnit, UUID> {

    /** ORGSCOPE-CONCURRENT-001 — the node row serializes a concurrent same-key grant (one winner). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM OrgUnit u WHERE u.id = :id")
    Optional<OrgUnit> findByIdForUpdate(@Param("id") UUID id);

    // ── through-root member reads (ScopeGrant owns no repository) ──────────────────────────

    /** The grants a {@code principal} holds, ordered — the candidate set for a containment check. */
    @Query("SELECT g FROM ScopeGrant g WHERE g.principal = :principal ORDER BY g.grantedAt ASC, g.id ASC")
    List<ScopeGrant> findGrantsByPrincipal(@Param("principal") String principal);

    /** Idempotency probe for ORGSCOPE-GRANT-001 — the existing grant for (node, principal, role). */
    @Query("SELECT g FROM ScopeGrant g WHERE g.orgUnitId = :orgUnitId AND g.principal = :principal AND g.role = :role")
    Optional<ScopeGrant> findGrant(@Param("orgUnitId") UUID orgUnitId,
                                   @Param("principal") String principal,
                                   @Param("role") ScopeRole role);

    /** The grants AT a single node (introspection) — Pageable per the unbounded-list guard. */
    @Query("SELECT g FROM ScopeGrant g WHERE g.orgUnitId = :orgUnitId ORDER BY g.grantedAt ASC, g.id ASC")
    List<ScopeGrant> findGrantsAtNode(@Param("orgUnitId") UUID orgUnitId, Pageable pageable);
}
