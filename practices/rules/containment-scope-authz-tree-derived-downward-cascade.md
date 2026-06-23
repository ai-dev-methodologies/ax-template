---
title: Hierarchical containment-scope authorization must model org units as a TREE with a materialized ancestor path, derive the DOWNWARD-ONLY containment cascade from that path at decision time (a grant at a node authorizes that node and its whole subtree — never its siblings or ancestors, never a leaf grant cascading upward), return 403 OUT_OF_SCOPE when no satisfying grant is held at the target node or any ancestor, and keep grants immutable + idempotent with concurrent same-key grants serialized on the node row
impact: HIGH
impactDescription: "A containment-scope authz that materializes a per-node ACL (instead of deriving the cascade from the tree) DRIFTS the moment the tree grows — a new descendant silently escapes a grant made at its ancestor, or a stale ACL row authorizes a node that has been restructured away; an upward-leaking cascade (a leaf grant treated as authorizing the parent) is a privilege escalation that breaks least-privilege; and an unsynchronized grant lets two threads write two rows for one (node, principal, role) (CWE-362). Authorization MUST be derived from the tree, downward-only, and fail-closed at the containment boundary"
tags:
  - authorization
  - access-control
  - governance
  - concurrency
spec_ref: "specs/containment-scope-authz-l0.yaml#ORGSCOPE-CONTAINMENT-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/orgscope/OrgScopeService.java + backend/src/main/java/com/ax/template/authblueprint/orgscope/OrgUnit.java + backend/src/main/java/com/ax/template/authblueprint/orgscope/ScopeGrant.java"
  pattern: "OrgUnit is a tree node carrying a materialized ancestor path (/ancestor/.../self/) set at creation so subtree containment is a deterministic prefix test at arbitrary depth (OrgUnit.isContainedBy = target.path startsWith ancestor.path); a ScopeGrant gives a principal a ScopeRole AT one node, immutable + idempotent (uq(node,principal,role)); OrgScopeService.check derives the decision from the caller's grants and the tree — allowed iff a grant's role satisfies the required role AND the grant's node CONTAINS the target (downward-only: the node itself + descendants, never siblings or ancestors), else 403 OUT_OF_SCOPE; no per-node ACL table exists; the grant write path takes the node's PESSIMISTIC_WRITE lock; ScopeGrant is an @AggregateMember of OrgUnit (root-JPQL reads + common/MemberWriter writes); no delete path exists"
upstream:
  - "https://csrc.nist.gov/projects/role-based-access-control/faqs"
  - "https://csrc.nist.gov/pubs/sp/800/162/upd2/final"
  - "https://cwe.mitre.org/data/definitions/362.html"
evidence:
  - source_type: external
    citation: "NIST Role Based Access Control FAQ (CSRC) — permissions are acquired through assigned roles or roles inherited through the role hierarchy; the org-unit tree is the structural hierarchy the grant-cascade generalizes"
    url: "https://csrc.nist.gov/projects/role-based-access-control/faqs"
    quote: "A role is essentially a collection of permissions, and all users receive permissions only through the roles to which they are assigned, or through roles they inherit through the role hierarchy."
    quoted_at: "2026-06-23"
  - source_type: external
    citation: "NIST SP 800-162, Guide to Attribute Based Access Control (ABAC) Definition and Considerations (CSRC) — authorization determined by evaluating the attributes/relationship of subject to object, which the containment check realizes"
    url: "https://csrc.nist.gov/pubs/sp/800/162/upd2/final"
    quote: "ABAC is a logical access control methodology where authorization to perform a set of operations is determined by evaluating attributes associated with the subject, object, requested operations, and, in some cases, environment conditions against policy, rules, or relationships that describe the allowable operations for a given set of attributes."
    quoted_at: "2026-06-23"
  - source_type: external
    citation: "CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization ('Race Condition') — MITRE (concurrent same-key grants racing one node)"
    url: "https://cwe.mitre.org/data/definitions/362.html"
    quote: "The product contains a concurrent code sequence that requires temporary, exclusive access to a shared resource, but a timing window exists in which the shared resource can be modified by another code sequence operating concurrently."
    quoted_at: "2026-06-01"
---

## Hierarchical authorization is a TREE-DERIVED downward containment cascade — not a flat per-resource ACL

**Impact: HIGH — a materialized per-node ACL drifts as the tree grows; an upward-leaking cascade is a privilege escalation; an unsynchronized grant double-writes one (node, principal, role) (CWE-362).**

The catalog's existing relationship-authz is FLAT: a direct edge between a subject and ONE resource (owner / participant, via `common/CallerScope`). Many real authorization models are HIERARCHICAL instead — an org chart, a location/site tree, a product-category tree, a cloud resource hierarchy (folder → project). There, a role granted at a *node* must cascade to the *entire subtree* under it. NIST frames this as the role hierarchy: *"all users receive permissions only through the roles to which they are assigned, or through roles they inherit through the role hierarchy"* — and NIST SP 800-162 frames the decision itself as evaluating the *"relationships that describe the allowable operations"* of subject to object. The containment cascade is exactly that relationship, derived from the tree:

```text
OrgUnit:        a tree node; parentId (root has none); a MATERIALIZED PATH /ancestor/.../self/
                so subtree containment is a prefix test at ARBITRARY depth (no recursive query)
ScopeGrant:     a principal holds a ScopeRole AT one node; immutable; uq(node,principal,role) idempotent
containment:    allowed iff a held grant's role satisfies the required role AND the grant's node
                CONTAINS the target (target.path startsWith grantedNode.path) — DOWNWARD ONLY
out-of-scope:   no satisfying grant at the target node or any ancestor → 403 OUT_OF_SCOPE (fail-closed)
locks:          the node row, PESSIMISTIC_WRITE — concurrent same-key grants → exactly one row
```

**1. The cascade is DERIVED from the tree, never stored (ORGSCOPE-CASCADE-001).** A grant authorizes a node and its descendants because the target's materialized path is prefixed by the granted node's path — computed at decision time. A denormalized per-node ACL would have to be rewritten every time a descendant is added, and silently drifts when it is not.

**2. The cascade is DOWNWARD ONLY (ORGSCOPE-CONTAINMENT-001).** A grant at a mid-tree node authorizes that node and everything under it, and is 403 on the node's siblings and on its ancestors. A grant at a *leaf* authorizes only that leaf — it never cascades upward. Treating a leaf grant as authorizing the parent is a privilege escalation that breaks least privilege.

**3. Grants are immutable + idempotent, same-key grants serialize (ORGSCOPE-GRANT/CONCURRENT-001).** One row per `(node, principal, role)`; the node-row `PESSIMISTIC_WRITE` lock + the `uq` index make a concurrent same-key grant a deterministic idempotent no-op (CWE-362).

**Incorrect — a flat per-target ACL, an upward-leaking cascade, an unsynchronized grant:**

```java
// ❌ a denormalized per-node ACL row, written once — drifts the moment the tree grows
public boolean canAccess(String principal, UUID targetNodeId, String role) {
    AclRow acl = aclRepo.findByPrincipalAndNode(principal, targetNodeId);   // ❌ no cascade at all,
    if (acl != null && acl.getRole().equals(role)) return true;             //    or a stored cascade
    UUID p = nodeRepo.findById(targetNodeId).get().getParentId();           // ❌ walks UPWARD —
    while (p != null) {                                                     //    a leaf grant now
        if (aclRepo.findByPrincipalAndNode(principal, p) != null) return true; // authorizes ancestors
        p = nodeRepo.findById(p).get().getParentId();                      // ❌ escalation; N queries
    }
    return false;                                                          // ❌ direction inverted
}
```

**Correct — containment derived from the materialized path, downward-only, fail-closed at the boundary:**

```java
@Transactional(readOnly = true)
public ScopeDecision check(String principal, UUID targetId, ScopeRole required) {
    OrgUnit target = units.findById(targetId).orElseThrow(OrgScopeException::nodeNotFound);
    List<ScopeGrant> grants = units.findGrantsByPrincipal(principal);      // ✅ the caller's grants
    Map<UUID, OrgUnit> grantedNodes = grants.stream().map(ScopeGrant::getOrgUnitId).distinct()
        .map(units::findById).filter(Optional::isPresent).map(Optional::get)
        .collect(Collectors.toMap(OrgUnit::getId, u -> u));
    for (ScopeGrant g : grants) {
        if (!g.getRole().satisfies(required)) continue;                    // ✅ role at-least-as-strong
        OrgUnit grantedNode = grantedNodes.get(g.getOrgUnitId());
        if (grantedNode != null && target.isContainedBy(grantedNode)) {   // ✅ DOWNWARD containment:
            return new ScopeDecision(true, g.getOrgUnitId(), g.getRole()); //    target.path startsWith
        }                                                                  //    grantedNode.path
    }
    throw OrgScopeException.outOfScope();                                  // ✅ 403, fail-closed
}
```

`OrgUnit.isContainedBy(ancestor)` is the whole cascade: `target.path.startsWith(ancestor.path)` is true for the granted node itself and every descendant at arbitrary depth, and false for siblings and strict ancestors — so a leaf grant never reaches upward. The decision consults only the caller's grants and the tree; no per-node ACL table exists to drift. The grant write path takes the node's `PESSIMISTIC_WRITE` lock and relies on `uq(node, principal, role)` so concurrent same-key grants converge to one row. `ScopeGrant` rows are `@AggregateMember` of `OrgUnit` — root-JPQL reads, `common/MemberWriter` writes; no delete path exists.

Verification: review-tier — confirm the node carries a materialized ancestor path, the containment check is a path-prefix test (downward only), an out-of-scope target is 403 (fail-closed), grants are immutable + idempotent, and the grant write path takes the node lock. The behavioural proof a fork-receiver keeps green: grant at a mid-tree node → allowed on its descendants, 403 on its siblings and ancestors; the N-thread same-key grant race (exactly one row).

Reference: [NIST RBAC FAQ (role hierarchy)](https://csrc.nist.gov/projects/role-based-access-control/faqs)

Reference: [NIST SP 800-162 (ABAC)](https://csrc.nist.gov/pubs/sp/800/162/upd2/final)

Reference: [CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization](https://cwe.mitre.org/data/definitions/362.html)
