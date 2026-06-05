---
title: A domain operation's metrics MUST use bounded-cardinality labels — fixed enums only, never ids / PII / unbounded values
impact: HIGH
impactDescription: "A Micrometer meter tagged with an entity id, user id, tenant id, raw 404 path, residual amount, or any user-supplied / unbounded value spawns a new time series per distinct value — a cardinality explosion that bloats the TSDB, slows queries, and can take down the monitoring backend; it also leaks PII into metrics. The per-operation observability counter every domain spec asks for is only safe when its labels are a fixed, low-cardinality enum set."
tags:
  - observability
  - metrics
  - micrometer
  - cardinality
  - bounded-labels
  - no-pii
spec_ref: "specs/observability-convention-l0.yaml#OBSCONV-CARDINALITY-001"
verification:
  type: review
  source: "specs/observability-convention-l0.yaml#OBSCONV-CARDINALITY-001"
  pattern: "Every domain-level Micrometer meter a recipe exposes (the canonical per-operation counter / gauge each domain spec's *-OBSERVABILITY item asks for) MUST tag ONLY fixed low-cardinality enum dimensions — typically {operation_type, outcome} where operation_type is a closed enum (transfer/allocation/journal, granted/exhausted, applied/rejected, ...) and outcome is a small closed enum. The label set MUST EXCLUDE entity ids, user ids, tenant ids (use a coarse bucket if a tenant dimension is needed), correlation/operation/request ids, raw error paths or messages, residual / amount / count numeric VALUES used as labels, and any user-supplied string that is not first normalized to a bounded enum. Reject a meter whose tag value comes from a request path, a thrown message, a primary key, or any source that grows without bound. This is the canonical governing rule for every domain spec's `*-OBSERVABILITY-001` per-operation bounded counter."
upstream:
  - "https://prometheus.io/docs/practices/naming/"
  - "https://docs.micrometer.io/micrometer/reference/concepts/naming.html"
evidence:
  - source_type: external
    citation: "Prometheus Documentation — Metric and label naming (label best practices / CAUTION on cardinality)"
    url: "https://prometheus.io/docs/practices/naming/"
    quote: "Remember that every unique combination of key-value label pairs represents a new time series, which can dramatically increase the amount of data stored."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "Prometheus Documentation — Metric and label naming (high-cardinality caution)"
    url: "https://prometheus.io/docs/practices/naming/"
    quote: "Do not use labels to store dimensions with high cardinality (many different label values), such as user IDs, email addresses, or other unbounded sets of values."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "Micrometer Documentation — Naming Meters (tag cardinality)"
    url: "https://docs.micrometer.io/micrometer/reference/concepts/naming.html"
    quote: "Beware of the potential for tag values coming from user-supplied sources to blow up the cardinality of a metric."
    quoted_at: "2026-06-06"
decided_at: "2026-06-06"
---

## A domain operation's metrics MUST use bounded-cardinality labels — fixed enums only, never ids / PII / unbounded values

**Impact: HIGH — Almost every domain in the catalog asks for one canonical observability meter: `claim_total{resource, outcome}`, `posting_total{operation_type, outcome}`, `quota_rejected_total{resource}`, `stale_event_dropped_total{stream, reason}`, `in_doubt_total{operation, outcome}`, and so on. Each is safe ONLY when its label set is a fixed, low-cardinality enum. The moment a recipe tags one of these with an entity id, a user/tenant id, a correlation id, a raw 404 path, a thrown exception message, or a numeric residual/amount/count VALUE, the meter spawns a brand-new time series for every distinct value. That is a cardinality explosion: it bloats the time-series database, slows every query and alert evaluation, runs up cost, and can topple the monitoring backend — and when the offending dimension is a user id or email it also leaks PII into metrics that are typically retained and broadly readable.**

Prometheus states the mechanism and the prohibition directly — *every unique combination of key-value label pairs represents a new time series, which can dramatically increase the amount of data stored*, and *do not use labels to store dimensions with high cardinality (many different label values), such as user IDs, email addresses, or other unbounded sets of values*. Micrometer gives the same warning for tag values: *beware of the potential for tag values coming from user-supplied sources to blow up the cardinality of a metric* — you must *carefully normalize and add bounds to user-supplied input*.

This is the canonical governing rule for every domain spec's `*-OBSERVABILITY-001` item (the per-operation bounded counter). There are three load-bearing requirements.

**1. Labels are a fixed, closed enum set.** The canonical per-operation meter tags only dimensions whose value space is small and known at compile time — typically `{operation_type, outcome}`. `operation_type` is a closed enum (`transfer` / `allocation` / `journal`; `seat` / `license`; `email` / `sms`). `outcome` is a 2–4 value closed enum (`granted` / `exhausted`; `balanced` / `rejected_unbalanced`; `applied` / `rejected`). The number of distinct label tuples is the product of the enum sizes — a small constant, not a function of traffic or data volume.

**2. Excluded from labels, always.** Entity ids, user ids, email addresses, **tenant ids** (use a coarse bucket enum if a tenant dimension is genuinely needed, never the raw id), correlation / operation / request ids, raw error paths, thrown exception messages, and any **numeric VALUE** (a residual amount, a remaining count, a balance) used as a label. Numbers belong in the metric's *value*, not its *labels*. A request-supplied string MUST be normalized to a bounded enum before it can become a tag — Micrometer's own example: constrain a 404 to `NOT_FOUND` rather than tagging the metric with each missing resource id.

**3. The meter is registered once, canonically.** One named meter per domain operation, registered through a small dedicated metrics component, so the tag schema is defined in exactly one place and cannot drift call-site to call-site.

**Incorrect — labels carry ids and a numeric value; cardinality grows without bound and PII leaks into metrics:**

```java
// VIOLATION: tenantId + userId are unbounded → one new time series per user;
// residual is a numeric VALUE used as a label → unbounded; email leaks PII.
meterRegistry.counter("posting_total",
    "tenant", tenantId,                 // unbounded id
    "user", user.email(),               // PII + unbounded
    "operation", op.id().toString(),    // unique per operation → unbounded
    "residual", residual.toPlainString()// numeric value as label → unbounded
).increment();
```

**Correct — only fixed enums are labels; ids/values are excluded; one canonical meter:**

```java
// CONSERVATION-OBSERVABILITY-001 / *-OBSERVABILITY-001 shape:
// operation_type is a closed enum, outcome is a closed enum → bounded.
@Component
class PostingMetrics {
    private final MeterRegistry registry;
    PostingMetrics(MeterRegistry registry) { this.registry = registry; }

    void balanced(OperationType type) {                 // type is an enum
        registry.counter("posting_total",
            "operation_type", type.name(),              // closed enum
            "outcome", "balanced").increment();         // closed enum
    }
    void rejectedUnbalanced(OperationType type) {
        registry.counter("posting_total",
            "operation_type", type.name(),
            "outcome", "rejected_unbalanced").increment();
    }
    // the residual amount, operation id, tenant id, user id are NEVER labels —
    // they are carried by logs/traces/the entity, not by the time series.
}
```

**Generic across every domain.** A bounded-capacity claim counts `claim_total{resource, outcome}`; a quota gate counts `quota_rejected_total{resource}` and gauges `quota_usage{tenant_bucket, resource}` (a coarse bucket, never the raw tenant id); a monotonic ingest counts `stale_event_dropped_total{stream, reason}` with `reason ∈ {behind_watermark, duplicate}`; an in-doubt outbound call counts `in_doubt_total{operation, outcome}` and gauges `in_doubt_open_count{operation}`. In every case the discipline is identical: the labels are a closed enum cross-product, ids and PII and raw numbers stay out, and a sustained non-zero `rejected`/`dropped`/`exhausted` rate is the operational signal a fork-receiver alerts on. SLO/alert wiring itself is a fork-receiver concern; the bounded-cardinality label contract is the catalog invariant.

Verification: review-tier. Label cardinality is a property of how a meter is constructed, with no compile-time signal — a meter tagged with an id compiles and runs fine, and only degrades the monitoring backend at scale. Verify by review against `specs/observability-convention-l0.yaml#OBSCONV-CARDINALITY-001`: every domain meter tags only fixed enums; no entity/user/tenant id, correlation id, raw path, exception message, or numeric value appears as a label; user-supplied strings are normalized to a bounded enum before tagging. When a fork-receiver wires a real Micrometer registry assertion that enumerates a meter's tag keys against the allowed enum set, this rule's verification block may be upgraded from review to gradle_task+tag.

Reference: [Prometheus — Metric and label naming (high-cardinality caution)](https://prometheus.io/docs/practices/naming/)

Reference: [Micrometer — Naming Meters (tag cardinality)](https://docs.micrometer.io/micrometer/reference/concepts/naming.html)
