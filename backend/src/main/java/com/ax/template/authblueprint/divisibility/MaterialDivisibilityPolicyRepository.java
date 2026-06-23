package com.ax.template.authblueprint.divisibility;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** NO delete method is declared anywhere in this domain — a policy version is appended, never removed. */
public interface MaterialDivisibilityPolicyRepository
        extends JpaRepository<MaterialDivisibilityPolicy, UUID> {

    /**
     * DIV-POLICY-001 — the current (highest-version) policy for a material. Read on every quantity
     * check; the version it returns is recorded on the check record as the policy in force.
     */
    @Query("SELECT p FROM MaterialDivisibilityPolicy p WHERE p.materialRef = :ref"
        + " ORDER BY p.policyVersion DESC LIMIT 1")
    Optional<MaterialDivisibilityPolicy> findCurrent(@Param("ref") String ref);

    /**
     * DIV-POLICY-001 — the current policy under a PESSIMISTIC_WRITE row lock. A re-declaration takes
     * this lock so the read-max-version / write-next-version sequence cannot interleave (two
     * concurrent re-declarations of the same material can never mint the same version — the
     * uq(material_ref, policy_version) backstop makes the residual-race loser deterministic).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM MaterialDivisibilityPolicy p WHERE p.materialRef = :ref"
        + " ORDER BY p.policyVersion DESC LIMIT 1")
    Optional<MaterialDivisibilityPolicy> findCurrentForUpdate(@Param("ref") String ref);

    /** DIV-POLICY-001 — the full append-only version history for a material, oldest first. */
    @Query("SELECT p FROM MaterialDivisibilityPolicy p WHERE p.materialRef = :ref"
        + " ORDER BY p.policyVersion ASC")
    List<MaterialDivisibilityPolicy> findHistory(@Param("ref") String ref, Pageable pageable);

    // ── through-root member reads (HG-AGG-REPO — DivisibilityCheck owns no repository) ──

    @Query("SELECT c FROM DivisibilityCheck c WHERE c.materialRef = :ref"
        + " ORDER BY c.checkedAt ASC, c.id ASC")
    List<DivisibilityCheck> findChecks(@Param("ref") String ref, Pageable pageable);
}
