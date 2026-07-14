package com.ax.template.authblueprint.approvalworkflow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * RoutingRule — a (categoryOrDept, amount-band) → ordered approver-role chain config row
 * (WF-ROUTE-001). Fully immutable once created: an admin who needs a different band deletes
 * the rule and creates a new one (no in-place edit), which is exactly what keeps an
 * already-resolved {@link ApprovalRequest#getResolvedChainJson()} snapshot safe from a later
 * rule change — resolution reads the rule ONCE, at submission, and the request never re-joins
 * against the live rule set afterward.
 *
 * <p>Half-open band semantics: {@code amount} matches iff
 * {@code minAmount <= amount < maxAmount} ({@code maxAmount == null} means open-ended).
 * Standalone aggregate root — not owned by {@link ApprovalRequest}; a request references a
 * resolved rule's OUTPUT (the role chain), never the rule row itself.
 */
@AggregateRoot
@Entity
@Table(name = "routing_rules")
public class RoutingRule {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "category_or_dept", nullable = false, updatable = false, length = 64)
    private String categoryOrDept;

    @Column(name = "min_amount", nullable = false, updatable = false, precision = 15, scale = 2)
    private BigDecimal minAmount;

    /** {@code null} = open-ended (no upper bound). */
    @Column(name = "max_amount", updatable = false, precision = 15, scale = 2)
    private BigDecimal maxAmount;

    /** Ordered JSON array of approver-role labels, e.g. {@code ["MANAGER","DIRECTOR"]}. */
    @Column(name = "approver_role_chain_json", nullable = false, updatable = false, length = 2000)
    private String approverRoleChainJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RoutingRule() {}

    public RoutingRule(UUID id, String categoryOrDept, BigDecimal minAmount, BigDecimal maxAmount,
                       String approverRoleChainJson, Instant createdAt) {
        this.id = id;
        this.categoryOrDept = categoryOrDept;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.approverRoleChainJson = approverRoleChainJson;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getCategoryOrDept() { return categoryOrDept; }
    public BigDecimal getMinAmount() { return minAmount; }
    public BigDecimal getMaxAmount() { return maxAmount; }
    public String getApproverRoleChainJson() { return approverRoleChainJson; }
    public Instant getCreatedAt() { return createdAt; }
}
