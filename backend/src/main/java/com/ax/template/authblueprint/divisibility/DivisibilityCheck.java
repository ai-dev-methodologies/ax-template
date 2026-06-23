package com.ax.template.authblueprint.divisibility;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateMember;

/**
 * One immutable quantity-check record (DIV-RECORD-001): the material, the submitted quantity
 * recorded VERBATIM (a rejected over-precise or fractional quantity is stored exactly as sent,
 * never normalized), the {@link CheckVerdict}, the policy VERSION in force at the time, and the
 * as-of instant. Append-only — every column is {@code updatable = false}, there is no public
 * setter. The verdict against the recorded policy version is the reconstructible basis of any
 * acceptance or rejection.
 *
 * <p>A {@code @AggregateMember} of {@link MaterialDivisibilityPolicy}: written through
 * {@code common/MemberWriter}, read via root-JPQL on {@link MaterialDivisibilityPolicyRepository}
 * (it owns no repository — HG-AGG-REPO). The cross-aggregate link to the policy version is a
 * recorded {@code long policy_version} value, not an object pointer.
 */
@AggregateMember(root = MaterialDivisibilityPolicy.class)
@Entity
@Table(name = "divisibility_checks")
public class DivisibilityCheck {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "material_ref", nullable = false, updatable = false, length = 200)
    private String materialRef;

    /** The submitted quantity, recorded EXACTLY as sent — never rounded or truncated. */
    @Column(name = "submitted_quantity", nullable = false, updatable = false, precision = 38, scale = 18)
    private BigDecimal submittedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "verdict", nullable = false, updatable = false, length = 20)
    private CheckVerdict verdict;

    /** The policy version in force at the time of the check — the reconstructible basis. */
    @Column(name = "policy_version", nullable = false, updatable = false)
    private long policyVersion;

    @Column(name = "checked_at", nullable = false, updatable = false)
    private Instant checkedAt;

    protected DivisibilityCheck() {}

    public DivisibilityCheck(UUID id, String materialRef, BigDecimal submittedQuantity,
                             CheckVerdict verdict, long policyVersion, Instant checkedAt) {
        this.id = id;
        this.materialRef = materialRef;
        this.submittedQuantity = submittedQuantity;
        this.verdict = verdict;
        this.policyVersion = policyVersion;
        this.checkedAt = checkedAt;
    }

    public UUID getId() { return id; }
    public String getMaterialRef() { return materialRef; }
    public BigDecimal getSubmittedQuantity() { return submittedQuantity; }
    public CheckVerdict getVerdict() { return verdict; }
    public long getPolicyVersion() { return policyVersion; }
    public Instant getCheckedAt() { return checkedAt; }
}
