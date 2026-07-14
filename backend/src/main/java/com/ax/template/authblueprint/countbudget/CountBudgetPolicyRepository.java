package com.ax.template.authblueprint.countbudget;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CountBudgetPolicyRepository extends JpaRepository<CountBudgetPolicy, UUID> {

    Optional<CountBudgetPolicy> findBySubjectKey(String subjectKey);

    boolean existsBySubjectKey(String subjectKey);

    /** PCB-CONSUME-001 — the policy row is the single serialization point for BOTH lazy period-creation
     *  AND consumption: two concurrent consumes for the same subject can never both accept past the cap
     *  (CWE-362). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM CountBudgetPolicy p WHERE p.subjectKey = :subjectKey")
    Optional<CountBudgetPolicy> findBySubjectKeyForUpdate(@Param("subjectKey") String subjectKey);

    // ── through-root member reads (HG-AGG-REPO — CountBudgetPeriod / CountBudgetConsumption own no repository) ──

    @Query("SELECT p FROM CountBudgetPeriod p WHERE p.policyId = :policyId AND p.periodKey = :periodKey")
    Optional<CountBudgetPeriod> findPeriod(@Param("policyId") UUID policyId, @Param("periodKey") String periodKey);

    /** PCB-AUDIT-001 — the consumed count is DERIVED, never a separately-stored total. */
    @Query("SELECT COUNT(c) FROM CountBudgetConsumption c WHERE c.periodId = :periodId")
    long countConsumptions(@Param("periodId") UUID periodId);

    @Query("SELECT COALESCE(MAX(c.sequenceNo), 0) FROM CountBudgetConsumption c WHERE c.periodId = :periodId")
    long maxConsumptionSequence(@Param("periodId") UUID periodId);

    @Query("SELECT p FROM CountBudgetPeriod p WHERE p.policyId = :policyId ORDER BY p.firstTouchedAt ASC")
    Page<CountBudgetPeriod> findPeriodsPage(@Param("policyId") UUID policyId, Pageable pageable);

    @Query("SELECT c FROM CountBudgetConsumption c WHERE c.periodId = :periodId ORDER BY c.sequenceNo ASC")
    Page<CountBudgetConsumption> findConsumptionsPage(@Param("periodId") UUID periodId, Pageable pageable);
}
