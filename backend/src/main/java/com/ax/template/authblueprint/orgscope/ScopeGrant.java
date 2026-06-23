package com.ax.template.authblueprint.orgscope;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateMember;

/**
 * containment-scope-authz grant: an IMMUTABLE record that {@link #principal} holds {@link #role}
 * AT the org-unit node {@link #orgUnitId} (ORGSCOPE-GRANT-001). The grant is the ONLY input to
 * authorization; the cascade to the node's subtree is DERIVED from the tree path at decision time
 * (ORGSCOPE-CASCADE-001), never stored. One grant per (org_unit, principal, role) — the
 * uq backstop makes a re-grant idempotent rather than a duplicate row. A ScopeGrant is an
 * {@code @AggregateMember} of {@link OrgUnit}: it has no repository of its own — root-JPQL reads,
 * {@code common/MemberWriter} writes (HG-AGG-REPO). All columns are {@code updatable=false}: a
 * grant is never edited in place; revocation (out of scope here) would be a separate tombstone.
 */
@AggregateMember(root = OrgUnit.class)
@Entity
@Table(name = "scope_grants", uniqueConstraints = {
    @UniqueConstraint(name = "uq_scope_grant", columnNames = {"org_unit_id", "principal", "role"})
})
public class ScopeGrant {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** The node this grant is rooted at — the cascade originates here and flows DOWN its subtree. */
    @Column(name = "org_unit_id", nullable = false, updatable = false)
    private UUID orgUnitId;

    @Column(name = "principal", nullable = false, updatable = false, length = 320)
    private String principal;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, updatable = false, length = 20)
    private ScopeRole role;

    @Column(name = "granted_by", nullable = false, updatable = false, length = 320)
    private String grantedBy;

    @Column(name = "granted_at", nullable = false, updatable = false)
    private Instant grantedAt;

    protected ScopeGrant() {}

    public ScopeGrant(UUID id, UUID orgUnitId, String principal, ScopeRole role,
                      String grantedBy, Instant grantedAt) {
        this.id = id;
        this.orgUnitId = orgUnitId;
        this.principal = principal;
        this.role = role;
        this.grantedBy = grantedBy;
        this.grantedAt = grantedAt;
    }

    public UUID getId() { return id; }
    public UUID getOrgUnitId() { return orgUnitId; }
    public String getPrincipal() { return principal; }
    public ScopeRole getRole() { return role; }
    public String getGrantedBy() { return grantedBy; }
    public Instant getGrantedAt() { return grantedAt; }
}
