package com.ax.template.authblueprint.orgscope;

import com.ax.template.authblueprint.common.MemberWriter;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * containment-scope-authz sole orchestrator. The authorization model is HIERARCHICAL containment
 * (NIST RBAC role hierarchy — "all users receive permissions only through the roles to which they
 * are assigned, or through roles they inherit through the role hierarchy"; NIST SP 800-162 ABAC —
 * authorization by the relationship of subject to object):
 *
 * <ul>
 *   <li>ORGSCOPE-TREE-001 — OrgUnits form a tree; each node carries a materialized ancestor path
 *       so subtree containment is a deterministic prefix test at arbitrary depth.</li>
 *   <li>ORGSCOPE-GRANT-001 — a ScopeGrant gives a principal a role AT one node; one row per
 *       (node, principal, role), so a re-grant is idempotent.</li>
 *   <li>ORGSCOPE-CONTAINMENT-001 — a caller may act on a target node with a required role IFF they
 *       hold a satisfying grant at that node OR ANY ANCESTOR of it (the grant's node CONTAINS the
 *       target); otherwise 403 OUT_OF_SCOPE. A grant at a leaf does NOT cascade upward.</li>
 *   <li>ORGSCOPE-CASCADE-001 — the cascade is DERIVED from the tree at decision time, never a
 *       denormalized per-node ACL that drifts.</li>
 *   <li>ORGSCOPE-CONCURRENT-001 — a concurrent same-key grant serializes on the node's
 *       PESSIMISTIC_WRITE row lock + uq(node, principal, role) backstop → exactly one row.</li>
 * </ul>
 *
 * ScopeGrant rows are members: {@link MemberWriter} writes, root-JPQL reads. No delete path exists.
 */
@Service
public class OrgScopeService {

    private final OrgUnitRepository units;
    private final MemberWriter members;
    private final OrgScopeMetrics metrics;
    private final Clock clock;

    public OrgScopeService(OrgUnitRepository units, MemberWriter members,
                           OrgScopeMetrics metrics, Clock clock) {
        this.units = units;
        this.members = members;
        this.metrics = metrics;
        this.clock = clock;
    }

    /** ORGSCOPE-TREE-001 — create a node; {@code parentId} null ⇒ a tree root. The materialized
     *  path is derived from the parent's path so containment is a prefix test at any depth. */
    @Transactional
    public OrgUnit createNode(UUID parentId, String name) {
        UUID id = UUID.randomUUID();
        String path;
        if (parentId == null) {
            path = OrgUnit.rootPath(id);
        } else {
            OrgUnit parent = units.findById(parentId).orElseThrow(OrgScopeException::nodeNotFound);
            path = OrgUnit.childPath(parent.getPath(), id);
        }
        OrgUnit saved = units.save(new OrgUnit(id, parentId, name, path, Instant.now(clock)));
        metrics.record("create_node", "created");
        return saved;
    }

    /** ORGSCOPE-GRANT/CONCURRENT-001 — grant {@code role} to {@code principal} AT {@code nodeId},
     *  idempotently. The node-row lock + uq(node, principal, role) make a concurrent same-key grant
     *  resolve to exactly one row (CWE-362); a re-grant returns the existing row unchanged. */
    @Transactional
    public ScopeGrant grant(UUID nodeId, String principal, ScopeRole role, String grantedBy) {
        OrgUnit node = units.findByIdForUpdate(nodeId).orElseThrow(OrgScopeException::nodeNotFound);
        Optional<ScopeGrant> existing = units.findGrant(node.getId(), principal, role);
        if (existing.isPresent()) {
            metrics.record("grant", "idempotent");
            return existing.get();
        }
        ScopeGrant g = new ScopeGrant(UUID.randomUUID(), node.getId(), principal, role,
            grantedBy, Instant.now(clock));
        try {
            members.persistAndFlush(g);
        } catch (DataIntegrityViolationException dup) {
            // a concurrent same-key grant beat us to the uq — return the row that won, idempotently
            metrics.record("grant", "idempotent");
            return units.findGrant(node.getId(), principal, role).orElseThrow(() -> dup);
        }
        metrics.record("grant", "granted");
        return g;
    }

    /**
     * ORGSCOPE-CONTAINMENT/CASCADE-001 (keystone) — may {@code principal} act on {@code targetId}
     * with {@code required} role? The decision is DERIVED purely from the tree: load the caller's
     * grants, and allow iff ANY grant is (a) at-least-as-strong as {@code required} AND (b) rooted
     * at a node that CONTAINS the target (the target's path is prefixed by the granted node's path
     * — true for the node itself and every descendant, false for siblings and strict ancestors).
     * Otherwise 403 OUT_OF_SCOPE. Never consults a stored per-node ACL.
     */
    @Transactional(readOnly = true)
    public ScopeDecision check(String principal, UUID targetId, ScopeRole required) {
        OrgUnit target = units.findById(targetId).orElseThrow(OrgScopeException::nodeNotFound);
        List<ScopeGrant> grants = units.findGrantsByPrincipal(principal);

        // index the granted nodes once so containment is a path-prefix test against each (TREE-derived)
        Map<UUID, OrgUnit> grantedNodes = grants.stream()
            .map(ScopeGrant::getOrgUnitId)
            .distinct()
            .map(units::findById)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(java.util.stream.Collectors.toMap(OrgUnit::getId, u -> u));

        for (ScopeGrant g : grants) {
            if (!g.getRole().satisfies(required)) {
                continue;
            }
            OrgUnit grantedNode = grantedNodes.get(g.getOrgUnitId());
            if (grantedNode != null && target.isContainedBy(grantedNode)) {
                metrics.record("check", "allowed");
                return new ScopeDecision(true, g.getOrgUnitId(), g.getRole());
            }
        }
        metrics.record("check", "out_of_scope");
        throw OrgScopeException.outOfScope();
    }

    @Transactional(readOnly = true)
    public OrgUnit getNode(UUID nodeId) {
        return units.findById(nodeId).orElseThrow(OrgScopeException::nodeNotFound);
    }

    @Transactional(readOnly = true)
    public List<ScopeGrant> grantsAtNode(UUID nodeId) {
        getNode(nodeId);                                    // 404 before an empty list
        return units.findGrantsAtNode(nodeId, PageRequest.of(0, 200));
    }

    /** The result of a containment check: allowed + the granting node/role that satisfied it. */
    public record ScopeDecision(boolean allowed, UUID viaNodeId, ScopeRole viaRole) {}
}
