---
title: Observability MUST be convention-driven — declared SLIs/SLOs, error-budget burn-rate alerting, RED/USE coverage, exemplar metric→trace links, dashboards-as-code
impact: MEDIUM
impactDescription: "Alerting on raw cause metrics (CPU high, disk 80%) instead of declared SLOs produces pager noise that does not correlate with user pain, and on-call burns out chasing non-incidents while real symptom regressions slip through. Without a declared SLI/SLO, error budget, and burn-rate alerts, 'is the service healthy?' has no objective answer; without RED/USE coverage and metric→trace exemplars, a regression cannot be drilled from symptom to cause."
tags:
  - observability
  - slo
  - error-budget
  - sre
  - alerting
  - dashboards-as-code
spec_ref: "specs/observability-convention-l0.yaml#OBSCONV-SLO-001"
verification:
  type: review
  source: "specs/observability-convention-l0.yaml#OBSCONV-SLO-001"
  pattern: "Observability MUST follow a declared convention. Each service declares SLIs (quantitative measures of service level) and SLO targets as first-class, version-controlled artifacts — not implicit (OBSCONV-SLO-001). An error budget derived from the SLO drives multi-window multi-burn-rate alerting, so alerts fire on SLO-threatening burn, not on arbitrary cause thresholds (OBSCONV-ERROR-BUDGET-001). Alerts carry a severity that maps to a declared routing convention (page vs ticket vs log) (OBSCONV-ALERT-ROUTING-001). Metric labels obey the bounded-cardinality budget — fixed enums only (OBSCONV-CARDINALITY-001, see domain-metrics-bounded-cardinality). Request services expose RED (Rate/Errors/Duration); resources expose USE (Utilization/Saturation/Errors) (OBSCONV-RED-USE-001). Metrics carry exemplars linking a data point to a trace for metric→trace drill-down (OBSCONV-TRACE-METRIC-LINK-001). Dashboards and alert rules are version-controlled as code, not hand-edited in a UI (OBSCONV-DASHBOARD-AS-CODE-001). Reject cause-based paging with no SLO, an undeclared SLI, alerts with no severity/routing, and dashboards that exist only as un-versioned UI state."
upstream:
  - "https://sre.google/sre-book/service-level-objectives/"
  - "https://sre.google/workbook/alerting-on-slos/"
evidence:
  - source_type: external
    citation: "Google SRE Book — Service Level Objectives (SLO definition)"
    url: "https://sre.google/sre-book/service-level-objectives/"
    quote: "An SLO is a service level objective: a target value or range of values for a service level that is measured by an SLI."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "Google SRE Book — Service Level Objectives (SLI definition)"
    url: "https://sre.google/sre-book/service-level-objectives/"
    quote: "An SLI is a service level indicator—a carefully defined quantitative measure of some aspect of the level of service that is provided."
    quoted_at: "2026-06-06"
decided_at: "2026-06-06"
---

## Observability MUST be convention-driven — SLO/error-budget alerting, RED/USE, exemplars, dashboards-as-code

**Impact: MEDIUM — The difference between observability that helps and observability that burns out on-call is convention. Per the Google SRE Book, *an SLO is a service level objective: a target value or range of values for a service level that is measured by an SLI*, where *an SLI is a service level indicator—a carefully defined quantitative measure of some aspect of the level of service that is provided*. Alerting on raw cause metrics (CPU, memory, disk) instead of on SLO-threatening error-budget burn produces a flood of pages that do not correlate with user pain: on-call chases non-incidents while a real latency regression that IS hurting users slips past because no symptom SLO was watching it. The fix is a declared convention — SLIs/SLOs as artifacts, error-budget burn-rate alerts, RED/USE coverage, metric→trace exemplars, dashboards-as-code.**

There are seven requirements — the items of `specs/observability-convention-l0.yaml`, all governed by this rule (CARDINALITY is additionally governed by `domain-metrics-bounded-cardinality`).

**1. Declared SLIs + SLO targets (OBSCONV-SLO-001).** Each service declares its SLIs and SLO targets as first-class, version-controlled artifacts — "99.9% of requests < 300ms over 28 days" — not an implicit notion in someone's head.

**2. Error budget + multi-window multi-burn-rate alerting (OBSCONV-ERROR-BUDGET-001).** The SLO yields an error budget; alerts fire on burn rate across multiple windows (a fast page for a sharp burn, a slow ticket for a gradual one), so paging tracks SLO threat — not an arbitrary "CPU > 80%".

**3. Severity → routing (OBSCONV-ALERT-ROUTING-001).** Every alert carries a severity that maps to a declared routing convention: page (wake someone), ticket (next business day), or log (record only).

**4. Cardinality budget (OBSCONV-CARDINALITY-001).** Metric labels are bounded fixed enums — governed by `domain-metrics-bounded-cardinality`.

**5. RED + USE coverage (OBSCONV-RED-USE-001).** Request-driven services expose RED — Rate, Errors, Duration; resources (pools, queues, disks) expose USE — Utilization, Saturation, Errors. The two methods together give symptom + cause coverage.

**6. Metric→trace exemplars (OBSCONV-TRACE-METRIC-LINK-001).** Metrics carry exemplars (a sampled trace id attached to a data point), so a spike on a latency histogram drills straight to an example slow trace — symptom to cause in one click.

**7. Dashboards + alerts as code (OBSCONV-DASHBOARD-AS-CODE-001).** Dashboards and alert rules live in version control (Grafana JSON / Terraform / Jsonnet), reviewed and deployed like code — not hand-edited in a UI where changes are unaudited and lost.

**Incorrect — pages on a raw cause metric; no SLO, no severity, dashboard only in the UI:**

```yaml
# VIOLATION: cause-based paging with no SLO/error-budget link (OBSCONV-SLO/ERROR-BUDGET)
- alert: HighCPU
  expr: node_cpu_utilization > 0.8     # not a symptom; not user-facing; pages at 3am for nothing
  # VIOLATION: no severity/routing (OBSCONV-ALERT-ROUTING); dashboard exists only as hand-edited UI state
```

**Correct — SLO-derived multi-burn-rate alert with severity; SLI/SLO + dashboard as code:**

```yaml
# slo.yaml (version-controlled artifact) — OBSCONV-SLO-001
slo: { service: orders-api, sli: "requests < 300ms / total", target: 0.999, window: 28d }
---
# Multi-window multi-burn-rate alert off the error budget — OBSCONV-ERROR-BUDGET-001 / OBSCONV-ALERT-ROUTING-001
- alert: OrdersApiFastBurn
  expr: burnrate_1h{service="orders-api"} > 14.4 and burnrate_5m{service="orders-api"} > 14.4
  labels: { severity: page }          # severity → routing convention
  annotations: { exemplar: "trace_id link via exemplars (OBSCONV-TRACE-METRIC-LINK-001)" }
# RED for the API + USE for its pool (OBSCONV-RED-USE-001); this file + the dashboard live in git (OBSCONV-DASHBOARD-AS-CODE-001).
```

Verification: review-tier. Observability convention is an operational property with no compile-time signal — cause-based alerts run fine and only reveal their cost as pager fatigue and missed symptom regressions. Verify by review against `specs/observability-convention-l0.yaml`: SLIs/SLOs are declared version-controlled artifacts; alerts are error-budget multi-burn-rate, not raw-cause; alerts carry severity→routing; labels are bounded; RED covers request services and USE covers resources; metrics carry trace exemplars; dashboards/alerts are code. When a fork-receiver wires a check that the SLO/alert/dashboard definitions exist in the repo and parse, this rule's verification may be upgraded from review to gradle_task+tag.

Reference: [Google SRE Book — Service Level Objectives](https://sre.google/sre-book/service-level-objectives/)

Reference: [Google SRE Workbook — Alerting on SLOs](https://sre.google/workbook/alerting-on-slos/)
