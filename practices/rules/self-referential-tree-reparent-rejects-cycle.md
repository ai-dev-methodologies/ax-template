---
title: Reparenting a node in a mutable self-referential tree MUST reject a move that creates a cycle — the parent relation stays a DAG
impact: HIGH
impactDescription: "Setting node.parent to the node itself or to one of its own descendants detaches that subtree into an unreachable cycle: recursive descendant traversals (org-chart rollups, category breadcrumbs, BOM explosion, folder paths, comment threads, GL account sums) loop indefinitely or silently drop the orphaned ring"
tags:
  - tree
  - hierarchy
  - self-referential
  - acyclicity
  - referential-integrity
  - reparent
spec_ref: "specs/optimistic-locking-l0.yaml#OPTLOCK-REPARENT-CYCLE-001"
verification:
  type: review
  source: "specs/optimistic-locking-l0.yaml#OPTLOCK-REPARENT-CYCLE-001"
  pattern: "The move/reparent handler of any mutable self-referential hierarchy (entity with a parent_id pointing at its own table) MUST, inside the SAME @Transactional unit as the parent write, compute descendants(node) (transitive closure over parent edges via recursive CTE, closure table, or in-memory walk) and reject the move when target == node OR target ∈ descendants(node), returning a 409/422 RFC 9457 ProblemDetail of type=urn:problem:tree-cycle and leaving the parent column unchanged — never an unconditional node.setParent(target); save(node). The check MUST run under the optimistic-lock guard (If-Match / @Version) so a concurrent reparent cannot slip a cycle through the check-to-write window."
upstream:
  - "https://en.wikipedia.org/wiki/Directed_acyclic_graph"
  - "https://www.postgresql.org/docs/current/queries-with.html"
evidence:
  - source_type: external
    citation: "Wikipedia — Directed acyclic graph (definition: a directed graph with no directed cycles)"
    url: "https://en.wikipedia.org/wiki/Directed_acyclic_graph"
    quote: "That is, it consists of vertices and edges (also called arcs), with each edge directed from one vertex to another, such that following those directions will never form a closed loop."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "PostgreSQL Documentation — 7.8 WITH Queries (Common Table Expressions), Recursive Queries"
    url: "https://www.postgresql.org/docs/current/queries-with.html"
    quote: "When working with recursive queries it is important to be sure that the recursive part of the query will eventually return no tuples, or else the query will loop indefinitely."
    quoted_at: "2026-06-01"
decided_at: "2026-06-01"
---

## Reparenting a node in a mutable self-referential tree MUST reject a move that creates a cycle — the parent relation stays a DAG

**Impact: HIGH — a reparent into a descendant detaches a subtree into an unreachable cycle, and every recursive traversal of the tree either loops forever or silently loses the orphaned ring**

A *self-referential* hierarchy is any entity whose `parent_id` points back at its own table: an org chart (employee → manager), a category/taxonomy tree, a folder/file tree, a bill-of-materials (assembly → sub-assembly), a threaded comment (reply → parent comment), a GL account tree (sub-account → account). Its parent relation is meant to be a **directed acyclic graph** — a forest. As a directed acyclic graph, *following those directions will never form a closed loop*. The moment a `move(node, target)` sets `node.parent = target` where `target` **is** the node itself, or **is one of the node's own descendants**, that invariant breaks: the node's subtree points back into itself, severing from the root. The node and its descendants form a ring reachable from nobody.

The damage is not theoretical — it is exactly the failure PostgreSQL warns about for the traversal that reads such a tree: *when working with recursive queries it is important to be sure that the recursive part of the query will eventually return no tuples, or else the query will loop indefinitely.* A category breadcrumb walk, an org-chart headcount rollup, a BOM explosion, a folder full-path resolve, a comment-thread render, a GL account-sum query — each climbs or descends the parent edges with a recursive CTE (or an in-memory walk). Introduce one cycle and that traversal spins until a `LIMIT`, a `CYCLE` clause, or a stack-overflow stops it. The write that *created* the cycle returned 200; the corruption surfaces later as a hung query or a subtree that vanished from every listing.

The rule: before any reparent of a self-referential node, **compute the node's descendant set and reject the move if the target is the node or any of its descendants**, inside the same transaction as the parent write.

**1. Compute `descendants(node)` as a transitive closure over the parent edges.** A recursive CTE (`WITH RECURSIVE`, *typically used to deal with hierarchical or tree-structured data*), a closure table, or a bounded in-memory walk from `node` down its children. The forbidden target set is `{node} ∪ descendants(node)`.

**2. Reject `target ∈ {node} ∪ descendants(node)`.** A self-parent (`target == node`) or a descendant-parent both create a cycle. Reject with `409 Conflict` (or `422 Unprocessable Entity`) + RFC 9457 ProblemDetail of `type=urn:problem:tree-cycle`, and leave the parent column **unchanged**. Never apply then repair.

**3. Run the check under the optimistic-lock guard, in the SAME transaction as the write.** Two admins reparenting nodes of the same tree concurrently can each pass an acyclicity check computed on a stale snapshot, then both commit — together forming a cycle neither created alone. The descendant check MUST be evaluated inside the same `@Transactional` unit as the `setParent` write, behind the If-Match / `@Version` optimistic-lock guard (`specs/optimistic-locking-l0.yaml#OPTLOCK-VERSION-001`), so a concurrent reparent loses on the version bump rather than slipping a cycle through the check-to-write window.

**Incorrect — unconditional reparent; a move under a descendant creates a cycle that hangs the next tree walk:**

```java
@Transactional
public void move(UUID nodeId, UUID targetParentId) {
    Node node = nodeRepo.findById(nodeId).orElseThrow();
    node.setParent(targetParentId);   // ❌ no check: target may be node itself
    nodeRepo.save(node);              //    or one of node's own descendants → cycle
}
```

**Correct — descendant closure computed in-transaction; self/descendant target rejected 409 tree-cycle; parent left unchanged:**

```java
@Transactional
public void move(UUID nodeId, UUID targetParentId) {
    Node node = nodeRepo.findById(nodeId)
        .orElseThrow(() -> new ResourceNotFoundException("node", nodeId));

    // transitive closure over parent edges, inside this txn (recursive CTE,
    // closure table, or in-memory walk). target must not be node nor below it.
    if (targetParentId.equals(nodeId)
            || nodeRepo.descendantIds(nodeId).contains(targetParentId)) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            "Cannot move a node under itself or one of its descendants — it would create a cycle.");
        pd.setType(URI.create("urn:problem:tree-cycle"));
        pd.setProperty("node_id", nodeId);
        pd.setProperty("target_parent_id", targetParentId);
        throw new ErrorResponseException(HttpStatus.CONFLICT, pd, null);
    }
    node.setParent(targetParentId);   // ✅ parent relation stays a DAG (forest)
    nodeRepo.save(node);              //    @Version bump rejects a racing reparent
}
```

**When to apply**: any mutable entity with a self-referential `parent_id` whose parent is re-assignable at runtime — org chart, category/taxonomy tree, folder/file tree, BOM, threaded comments, GL account tree. **When NOT to apply**: append-only hierarchies where a node's parent is set once at creation and is immutable (no reparent path exists, so no cycle can be introduced), or flat lists with no self-reference. Pair with `destructive-remove-checks-inbound-references.md` (the delete side of structural integrity) and `specs/optimistic-locking-l0.yaml#OPTLOCK-VERSION-001` (the concurrent-reparent guard).

Verification: review-tier. Tree acyclicity under reparent is a cross-row runtime invariant of the move handler with no compile-time signal — a single happy-path move test passes on a handler that omits the descendant check entirely, and the cycle only manifests as a later hung recursive query. Verify by review against `specs/optimistic-locking-l0.yaml#OPTLOCK-REPARENT-CYCLE-001`: the reparent path computes `descendants(node)` and rejects `target ∈ {node} ∪ descendants(node)` with a 409/422 `tree-cycle` ProblemDetail, inside the same transaction as the parent write and under the optimistic-lock guard. When a fork-receiver wires a real `@Tag("OPTLOCK-REPARENT-CYCLE-001")` negative IT (reparent under a descendant → 409 + parent unchanged), this rule's verification block may be upgraded from review to gradle_task+tag.

Reference: [Wikipedia — Directed acyclic graph (a directed graph with no directed cycles)](https://en.wikipedia.org/wiki/Directed_acyclic_graph)

Reference: [PostgreSQL — 7.8 WITH Queries: Recursive Queries (hierarchical/tree traversal; must terminate or loop indefinitely)](https://www.postgresql.org/docs/current/queries-with.html)
