package com.ax.template.authblueprint.approvalworkflow;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface RoutingRuleRepository extends JpaRepository<RoutingRule, java.util.UUID> {

    List<RoutingRule> findAllByOrderByCategoryOrDeptAscMinAmountAsc();

    /**
     * WF-ROUTE-001 — half-open band match: {@code minAmount <= amount < maxAmount}
     * ({@code maxAmount IS NULL} = open-ended). Ordered by {@code minAmount} so the
     * caller can deterministically take the first (narrowest-starting) match.
     */
    @Query("SELECT r FROM RoutingRule r WHERE r.categoryOrDept = :cat "
        + "AND :amount >= r.minAmount AND (r.maxAmount IS NULL OR :amount < r.maxAmount) "
        + "ORDER BY r.minAmount ASC")
    List<RoutingRule> findMatches(@Param("cat") String categoryOrDept, @Param("amount") BigDecimal amount);
}
