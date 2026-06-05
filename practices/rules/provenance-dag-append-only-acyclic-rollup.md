---
title: A provenance DAG MUST store edges append-only and immutable, stay acyclic as a rollup precondition, and roll up by product-down-path summed-across-paths
impact: HIGH
impactDescription: "If provenance edges are mutable/deletable, the lineage record can be rewritten and an audit cannot trust it; if the graph is allowed a cycle, a weighted rollup (a BOM cost explosion, a contribution trace) is mathematically undefined and a recursive traversal loops forever; if the rollup sums weights instead of multiplying down each path and summing across paths, the computed quantity is simply wrong. Each defect corrupts the lineage or the number derived from it."
tags:
  - provenance
  - dag
  - append-only
  - recursive-cte
  - rollup
  - postgresql
spec_ref: "specs/provenance-dag-l0.yaml#DAG-EDGE-001"
verification:
  type: review
  source: "specs/provenance-dag-l0.yaml#DAG-EDGE-001"
  pattern: "A provenance graph MUST record lineage as an APPEND-ONLY, immutable many-to-many edge set — edge rows are insert-only with immutable columns (`@Column(updatable=false)`), no edit/delete mutator, never a single nullable FK on the node (DAG-EDGE-001). The graph MUST be acyclic as a precondition for a well-defined rollup: acyclicity is enforced at write time (a reparent/insert that would close a cycle is rejected) OR the rollup defensively re-checks and refuses (422) on a detected cycle (DAG-ACYCLIC-PRECOND-001). A cumulative rollup (BOM explosion, contribution trace) MUST compute the PRODUCT of per-edge weights ALONG each path, then SUM those path-products ACROSS all paths, in scaled BigDecimal — never a flat sum of weights (DAG-ROLLUP-MULTIPLY-001). Traversal is bounded and cycle-safe (DAG-TRAVERSE-BOUNDED-001, the existing traversal rule). Reject a mutable/deletable edge, a graph with no acyclicity guard, and a rollup that sums edge weights instead of product-down-path / sum-across-paths."
upstream:
  - "https://www.postgresql.org/docs/current/queries-with.html"
  - "https://www.postgresql.org/docs/current/ddl-constraints.html"
evidence:
  - source_type: external
    citation: "PostgreSQL Documentation — WITH Queries (Common Table Expressions)"
    url: "https://www.postgresql.org/docs/current/queries-with.html"
    quote: "WITH provides a way to write auxiliary statements for use in a larger query. These statements, which are often referred to as Common Table Expressions or CTEs, can be thought of as defining temporary tables that exist just for one query."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "PostgreSQL Documentation — WITH Queries (RECURSIVE for tree/graph traversal)"
    url: "https://www.postgresql.org/docs/current/queries-with.html"
    quote: "Recursive queries are typically used to deal with hierarchical or tree-structured data."
    quoted_at: "2026-06-06"
decided_at: "2026-06-06"
---

## A provenance DAG MUST store edges append-only, stay acyclic, and roll up by product-down-path summed-across-paths

**Impact: HIGH — A provenance graph records lineage: which inputs produced which outputs, with what weight (a bill-of-materials explosion, a cost/contribution trace, a data-lineage map). Three properties keep it trustworthy. The edges must be append-only and immutable, or the lineage record can be silently rewritten and no audit can rely on it. The graph must stay acyclic, because a weighted rollup over a cyclic graph is mathematically undefined and a recursive traversal — PostgreSQL's *recursive queries are typically used to deal with hierarchical or tree-structured data*, where *WITH provides a way to write auxiliary statements ... thought of as defining temporary tables that exist just for one query* — loops forever without a cycle guard. And the rollup arithmetic must multiply weights down each path and sum across paths; a flat sum of edge weights yields a wrong number.**

This rule governs the structural items of `specs/provenance-dag-l0.yaml`, composing the bounded cycle-safe traversal (`DAG-TRAVERSE-BOUNDED-001`, the existing `provenance-dag-traversal-is-bounded-and-cycle-safe` rule).

**1. Append-only immutable edge set (DAG-EDGE-001).** Provenance is a many-to-many edge table (`from_node`, `to_node`, `weight`), insert-only, with immutable columns (`@Column(updatable=false)`) and NO edit/delete mutator. A node never carries a single nullable parent FK (which cannot express multiple inputs and is mutable). The lineage is thus a tamper-resistant historical record.

**2. Acyclicity precondition (DAG-ACYCLIC-PRECOND-001).** A weighted rollup is well-defined ONLY over an acyclic graph. Acyclicity is enforced at write time — an edge insert/reparent that would close a cycle is rejected — OR the rollup defensively re-checks (a recursive CTE with a CYCLE clause / visited-set) and refuses with 422 on a detected cycle, rather than looping or returning garbage.

**3. Product-down-path, sum-across-paths rollup (DAG-ROLLUP-MULTIPLY-001).** The cumulative quantity for a node is the PRODUCT of the per-edge weights along each path from the source, then the SUM of those path-products across all distinct paths — scaled `BigDecimal` arithmetic. A BOM where part A needs 2×B and each B needs 3×C means A needs 6×C *per path*, summed over every route C reaches A. A flat `SUM(weight)` is simply the wrong number.

**Incorrect — mutable single-parent FK; recursive walk with no cycle guard; flat sum rollup:**

```java
class Node { @ManyToOne Node parent; }            // VIOLATION: single mutable FK, not an append-only edge set (DAG-EDGE-001)

BigDecimal rollup(Node n) {                         // VIOLATION: no cycle guard → infinite recursion on a cycle (DAG-ACYCLIC)
    BigDecimal total = BigDecimal.ZERO;
    for (Edge e : edges(n)) total = total.add(e.weight());   // VIOLATION: flat SUM, not product-down/sum-across (DAG-ROLLUP)
    return total;
}
```

**Correct — append-only immutable edges; cycle-safe recursive CTE; product-down-path summed-across-paths:**

```java
@Entity class ProvenanceEdge {                      // append-only many-to-many (DAG-EDGE-001)
    @Column(updatable=false) Long fromNode;
    @Column(updatable=false) Long toNode;
    @Column(updatable=false) BigDecimal weight;     // immutable; no setter, no delete mutator
}
```
```sql
-- cycle-safe recursive CTE; product down each path, summed across paths (DAG-ACYCLIC / DAG-ROLLUP)
WITH RECURSIVE paths(node, qty) AS (
    SELECT :root, 1::numeric
  UNION ALL
    SELECT e.to_node, p.qty * e.weight                      -- PRODUCT down the path
      FROM paths p JOIN provenance_edge e ON e.from_node = p.node
) CYCLE node SET is_cycle USING path                        -- refuse cycles (DAG-ACYCLIC-PRECOND-001)
SELECT node, SUM(qty) FROM paths WHERE NOT is_cycle GROUP BY node;  -- SUM across paths
```

Verification: review-tier. DAG correctness is a data-model + arithmetic property — a mutable-FK / flat-sum implementation compiles and gives plausible numbers on a single-path graph, corrupting only on multi-path or cyclic graphs. Verify by review against `specs/provenance-dag-l0.yaml`: edges are append-only immutable many-to-many (no edit/delete); acyclicity is guarded at write time or re-checked with 422 on the rollup; the rollup multiplies down each path and sums across paths in scaled BigDecimal; traversal is bounded/cycle-safe. When a fork-receiver wires a real test (a cycle is rejected; a diamond BOM rolls up to the product-sum, not the flat sum), this rule's verification may be upgraded from review to gradle_task+tag.

Reference: [PostgreSQL — WITH Queries (CTEs / RECURSIVE)](https://www.postgresql.org/docs/current/queries-with.html)
