---
title: Provenance / lineage / dependency-DAG traversal MUST be cycle-safe, depth-bounded and result-size-bounded
impact: HIGH
impactDescription: "A where-used / blast-radius / lineage / BOM-explosion read that walks a many-to-many derivation graph with no cycle guard and no depth/size cap will, on a dirty or pathological graph (a back-edge, a runaway fan-out), recurse forever — stack-overflow, OOM, or a query that never returns — turning a routine read into an outage. Cross-industry: recall blast-radius, data/ETL lineage, ML model provenance, dependency-impact analysis, BOM explosion + where-used, WBS / cost rollup, org headcount / comp rollup, nested kit / bundle pricing."
tags:
  - provenance
  - lineage
  - dag
  - graph-traversal
  - recursive-cte
  - cycle-detection
  - bounded-traversal
  - blast-radius
  - bom-explosion
spec_ref: "specs/provenance-dag-l0.yaml#DAG-TRAVERSE-BOUNDED-001"
verification:
  type: review
  source: "backend forward (where-used / blast-radius) and backward (what-went-into) provenance read path over the many-to-many edge set (recursive CTE service / repository or in-memory graph walk)"
  pattern: "The recursive traversal carries an explicit cycle guard (PostgreSQL `CYCLE` clause or a visited-set / path-tracking set), a max-depth cap (or CTE LIMIT) and a result-size cap with pagination; on a graph that contains a back-edge or exceeds the bound the read degrades DETERMINISTICALLY to a truncation-marker (truncated=true + next-cursor) or 422 + RFC 9457 type=urn:problem:graph-traversal-bound, and a bounded-cardinality fan-out metric (nodes-visited / max-depth-reached) is emitted — it never hangs, OOMs, or stack-overflows"
upstream:
  - "https://www.postgresql.org/docs/current/queries-with.html"
  - "https://en.wikipedia.org/wiki/Directed_acyclic_graph"
evidence:
  - source_type: external
    citation: "PostgreSQL Documentation — 7.8. WITH Queries (Common Table Expressions), §7.8.2.2 Cycle Detection"
    url: "https://www.postgresql.org/docs/current/queries-with.html"
    quote: "The CYCLE clause specifies first the list of columns to track for cycle detection, then a column name that will show whether a cycle has been detected, and finally the name of another column that will track the path."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "PostgreSQL Documentation — 7.8. WITH Queries (Common Table Expressions), §7.8.2.2 Cycle Detection (LIMIT-in-parent loop-testing trick)"
    url: "https://www.postgresql.org/docs/current/queries-with.html"
    quote: "A helpful trick for testing queries when you are not certain if they might loop is to place a LIMIT in the parent query."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "Wikipedia — Directed acyclic graph (graph theory)"
    url: "https://en.wikipedia.org/wiki/Directed_acyclic_graph"
    quote: "A directed acyclic graph is a directed graph that has no cycles."
    quoted_at: "2026-06-01"
---

## Provenance / lineage / dependency-DAG traversal MUST be cycle-safe, depth-bounded and result-size-bounded

**Impact: HIGH — a graph read with no cycle guard and no bound is a latent outage. The moment the underlying many-to-many derivation graph holds a single back-edge — a dirty import, a corrupted edge, a legitimately cyclic dependency someone recorded by mistake — an unbounded recursive walk recurses forever: stack-overflow, OOM, or a query that never returns. A routine "what did this part go into?" / "what feeds this dataset?" read becomes an incident.**

Provenance, lineage, dependency and bill-of-materials data are all the same shape: a MANY-TO-MANY directed graph of input→output edges. One output derives from N inputs; one input feeds N outputs. The two reads everyone wants are FORWARD — *where-used / blast-radius*: every output reachable from this input (which finished lots used this recalled ingredient? which dashboards consume this column? which services depend on this library?) — and BACKWARD — *what-went-into*: every input of this output. Both are recursive transitive-closure walks, and both are unsafe by default. A directed acyclic graph is a directed graph that has no cycles — but nothing in your *data* guarantees acyclicity unless your *write side* enforces it, and even then a read path must survive a graph that was corrupted out-of-band. So the read itself must be the thing that is bounded.

Three bounds are mandatory, together:

1. **Cycle safety.** Track a visited-set / path so a back-edge cannot drive infinite recursion. In PostgreSQL this is the built-in `CYCLE` clause on a recursive CTE; in an in-memory walk it is an explicit visited `Set`. The `CYCLE` clause specifies the columns to track for cycle detection, a column that shows whether a cycle has been detected, and a column that tracks the path — exactly the state a safe walk needs.
2. **Depth bound.** Cap recursion depth (a `WHERE depth < :max` term, or the parent-query `LIMIT` trick PostgreSQL documents for queries that might loop). A finite depth budget means even an acyclic-but-very-deep graph returns.
3. **Result-size bound.** Cap the number of nodes returned and paginate. A wide fan-out (one popular input feeding millions of outputs) is a denial-of-service payload even with no cycle and shallow depth.

When any bound is reached the read MUST degrade DETERMINISTICALLY — a truncation-marker (`truncated=true` + a next-cursor) or, when the graph is detected as cyclic or the bound is structurally exceeded, `422 Unprocessable Entity` + an RFC 9457 body of `type=urn:problem:graph-traversal-bound`. It must never hang, OOM, or stack-overflow. And it must emit a bounded-cardinality fan-out metric (nodes-visited, max-depth-reached) so an operator can see a blast-radius blow up before it hurts.

This is **distinct** from the single-parent tree acyclicity rule (`OPTLOCK-REPARENT-CYCLE-001`): that rule guards the WRITE side of a *single-parent forest* (one mutable parent column) so a reparent cannot close a cycle. This rule guards the READ side of a *many-to-many* graph, where one node has many parents and many children and a single FK cannot even express the relation. The two compose: the reparent rule (and `DAG-ACYCLIC-PRECOND-001`) keep the graph acyclic at write time so a weighted rollup is well-defined; this rule keeps every *read* safe regardless, because data can be dirtied out-of-band.

The pattern is **generic and cross-cutting** — not manufacturing-only, not Korea-specific. The identical bounded-traversal shows up in: recall / **blast-radius** (which shipments contain the recalled lot?), **data & ETL lineage** (which downstream tables/dashboards depend on this source?), **ML model provenance** (which models were trained on this dataset version?), **dependency-impact analysis** (what breaks if I bump this package?), **BOM explosion + where-used**, **WBS / cost rollup**, **org headcount / comp rollup**, and **nested kit / bundle pricing**. In every case the invariant is the same: the read terminates within a fixed depth and size budget no matter the graph's shape.

**Incorrect — naive recursive walk: no cycle guard, no depth cap, no size cap; one back-edge = infinite recursion / stack overflow:**

```java
// Forward "where-used" read — walks input -> outputs with nothing bounding it.
List<Long> whereUsed(Long inputId) {
  List<Long> out = new ArrayList<>();
  for (Edge e : edgeRepo.findBySourceId(inputId)) {
    out.add(e.getTargetId());
    out.addAll(whereUsed(e.getTargetId()));   // ❌ no visited-set: a back-edge recurses forever (StackOverflowError)
  }                                            // ❌ no depth cap, no size cap: a wide fan-out OOMs the heap
  return out;
}
```

```sql
-- Or the SQL form, equally unsafe: recursive CTE with no CYCLE clause and no bound.
WITH RECURSIVE used(id) AS (
  SELECT target_id FROM edge WHERE source_id = :inputId
  UNION ALL
  SELECT e.target_id FROM edge e JOIN used u ON e.source_id = u.id   -- ❌ a cycle never terminates
)
SELECT id FROM used;                                                  -- ❌ no LIMIT, no depth column
```

**Correct — cycle-safe (CYCLE clause / visited-set), depth-bounded, size-bounded; degrades to a truncation-marker or 422 graph-traversal-bound; emits a fan-out metric:**

```sql
-- PostgreSQL recursive CTE: CYCLE clause guards against back-edges,
-- depth column + parent LIMIT bound the walk, pagination caps result size.
WITH RECURSIVE used(id, depth) AS (
    SELECT target_id, 1 FROM edge WHERE source_id = :inputId
  UNION ALL
    SELECT e.target_id, u.depth + 1
    FROM edge e JOIN used u ON e.source_id = u.id
    WHERE u.depth < :maxDepth                      -- ✅ depth bound
) CYCLE id SET is_cycle USING path                  -- ✅ cycle detection (back-edge marked, not recursed)
SELECT id, is_cycle FROM used
WHERE NOT is_cycle
LIMIT :pageSize + 1;                                 -- ✅ size bound + pagination (sentinel row -> truncated=true)
```

```java
// Service: on a detected cycle or an exceeded bound, degrade deterministically.
WhereUsedPage whereUsed(Long inputId, int maxDepth, int pageSize, Cursor cursor) {
  TraversalResult r = traversalRepo.forward(inputId, maxDepth, pageSize, cursor);
  metrics.summary("dag_traversal_nodes_visited", "operation", "where_used").record(r.visited());
  if (r.cycleDetected()) {                           // ✅ never recurses a cycle
    metrics.counter("dag_traversal_bound_hit_total", "operation", "where_used", "reason", "cycle").increment();
    throw new GraphTraversalBoundException("urn:problem:graph-traversal-bound");   // ✅ -> 422 RFC 9457
  }
  boolean truncated = r.hitSizeCap() || r.hitDepthCap();
  if (truncated) {
    metrics.counter("dag_traversal_bound_hit_total", "operation", "where_used",
                    "reason", r.hitDepthCap() ? "depth" : "size").increment();
  }
  return new WhereUsedPage(r.nodes(), truncated, r.nextCursor());   // ✅ truncation-marker, never a hang/OOM
}
```

Verification: review the forward (where-used / blast-radius) and backward (what-went-into) provenance read paths — confirm the recursive traversal carries an explicit cycle guard (PostgreSQL `CYCLE` clause or an in-memory visited-set / path set), a max-depth cap (or CTE `LIMIT`) and a result-size cap with pagination; confirm that a fixture graph containing a back-edge yields a `422 urn:problem:graph-traversal-bound` (or a `truncated=true` marker) rather than a `StackOverflowError` / OOM / hang, that an oversize fan-out paginates rather than returning an unbounded payload, and that a bounded-cardinality fan-out metric is emitted per traversal.

Reference: [PostgreSQL — WITH Queries (Common Table Expressions): Cycle Detection + Limiting Recursion Depth](https://www.postgresql.org/docs/current/queries-with.html)

Reference: [Directed acyclic graph (graph theory)](https://en.wikipedia.org/wiki/Directed_acyclic_graph)
