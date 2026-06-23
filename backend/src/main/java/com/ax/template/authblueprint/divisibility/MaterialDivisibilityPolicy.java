package com.ax.template.authblueprint.divisibility;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import java.time.Instant;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * material-divisibility-constraint-l0 root: one immutable, append-only VERSION of a material's
 * divisibility policy (DIV-POLICY-001). A material declares whether it is INTEGER_ONLY (whole units
 * only) or FRACTIONAL (any positive amount, bounded by {@link #maxScale} decimal places). Re-
 * declaring a material's policy APPENDS a new row with the next {@code policy_version}; the prior
 * version is retained, never overwritten (uq(material_ref, policy_version)), so the policy in force
 * at any past check is reconstructible. Every column is {@code updatable = false} — there are NO
 * mutator hooks at all; a re-declaration is a new row, never an edit.
 *
 * <p>The {@link Check} binds {@code policy_version >= 1 AND max_scale >= 0 AND (policy_kind =
 * 'FRACTIONAL' OR max_scale = 0)} so an INTEGER_ONLY row cannot carry a meaningful scale and no row
 * can carry a negative scale. {@code @Version} is present only to satisfy the catalog's optimistic-
 * lock posture (the row is otherwise immutable). The column is {@code max_scale} (never a column
 * named {@code value} / {@code limit} / {@code order} / {@code scale}).
 */
@AggregateRoot
@Entity
@Table(name = "material_divisibility_policies", uniqueConstraints = {
    @UniqueConstraint(name = "uq_divisibility_material_version", columnNames = {"material_ref", "policy_version"})
})
@Check(constraints =
    "policy_version >= 1 AND max_scale >= 0"
    + " AND (policy_kind = 'FRACTIONAL' OR max_scale = 0)")
public class MaterialDivisibilityPolicy {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** The material this policy version governs — opaque, recorded verbatim. */
    @Column(name = "material_ref", nullable = false, updatable = false, length = 200)
    private String materialRef;

    /** Monotonically increasing per-material version (1, 2, 3, …); a re-declaration appends. */
    @Column(name = "policy_version", nullable = false, updatable = false)
    private long policyVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "policy_kind", nullable = false, updatable = false, length = 20)
    private DivisibilityPolicyKind policyKind;

    /** Maximum decimal places for a FRACTIONAL material; 0 for INTEGER_ONLY (whole units only). */
    @Column(name = "max_scale", nullable = false, updatable = false)
    private int maxScale;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "declared_at", nullable = false, updatable = false)
    private Instant declaredAt;

    protected MaterialDivisibilityPolicy() {}

    public MaterialDivisibilityPolicy(UUID id, String materialRef, long policyVersion,
                                      DivisibilityPolicyKind policyKind, int maxScale, Instant declaredAt) {
        this.id = id;
        this.materialRef = materialRef;
        this.policyVersion = policyVersion;
        this.policyKind = policyKind;
        // INTEGER_ONLY pins max_scale to 0 so the @Check stays satisfied and the basis is unambiguous.
        this.maxScale = policyKind == DivisibilityPolicyKind.INTEGER_ONLY ? 0 : maxScale;
        this.declaredAt = declaredAt;
    }

    public UUID getId() { return id; }
    public String getMaterialRef() { return materialRef; }
    public long getPolicyVersion() { return policyVersion; }
    public DivisibilityPolicyKind getPolicyKind() { return policyKind; }
    public int getMaxScale() { return maxScale; }
    public Long getVersion() { return version; }
    public Instant getDeclaredAt() { return declaredAt; }
}
