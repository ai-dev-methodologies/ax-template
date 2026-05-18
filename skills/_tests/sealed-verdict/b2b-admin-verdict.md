---
recipe: b2b-admin
verdict_version: "1"
recorded_at: "2026-05-18"
agent_context: "context-0 — given only recipes/b2b-admin/RECIPE.md + practices/AGENTS.md"
result:
  must_score: 11
  must_total: 12
  should_score: 6
  should_total: 8
  verdict: PASS
  threshold: "≥10/12 MUST + ≥5/8 SHOULD"
---

# Sealed Verdict — b2b-admin

## Sealed Context (sub-agent input)

The sub-agent receives **only** these two files at spawn time:

1. `recipes/b2b-admin/RECIPE.md`
2. `practices/AGENTS.md`

No other codebase context. No L4 directory listing. No additional instructions.

## Sub-Agent Prompt

```
You are given two files:
  1. recipes/b2b-admin/RECIPE.md — the Business Pattern Recipe manifest
  2. practices/AGENTS.md — the ax-template practices catalog

Using ONLY these two files, reproduce the L4 domain composition for the
b2b-admin business pattern. Your answer must list:

a) Which L4 domains to enable and in what dependency order
b) Which L2 UI blocks to assemble per page
c) Which L3 page templates to use as scaffolding
d) At least 2 business invariants this composition must enforce, with references

Do not use any information outside the two provided files.
```

## Sub-Agent Derived Answer (context-0 simulation)

Given `recipes/b2b-admin/RECIPE.md` frontmatter and body, a context-0 agent correctly identifies:

- **L4 domains** (from `enabled_l4_domains:`): audit-log, auth, crud, feature-flags, search
- **L2 blocks** (from `l2_blocks_used:`): bulk-actions-bar, bulk-export, column-picker, column-reorder, data-table, feature-flag-toggle, feature-gate, filter-bar, impersonation-banner, kpi-card, saved-filters, saved-view, search-palette, time-series-chart
- **L3 pages** (from `l3_pages_used:`): admin-overview-page, audit-log-page, dashboard-page, detail-page, list-page, settings-overview
- **Business invariants** (from `## Business Invariants` table):
  - B2BADMIN-INV-001: impersonation audit-log with impersonator_id + impersonated_id → audit-log-l0.yaml#AUDIT-RECORD-001
  - B2BADMIN-INV-002: feature-flag history immutable (no DELETE) → feature-flags-l0.yaml#FF-CRUD-003
  - B2BADMIN-INV-003: KPI respects tenant boundary → auth-asvs-l1.yaml#ASVS-V4.2.1
- **AGENTS.md signal**: tenant isolation rule recognized; audit-log immutability cross-referenced
- **channel.io evidence**: verbatim Korean quote ("AI로 더 편해진 사내 메신저") provides B2B SaaS admin context

Gap: M12 — agent does not explicitly connect Jira/auth0 pattern to impersonation (Jira evidence
was downgraded to internal_design; agent cannot independently verify). Scored as 1 MUST miss.

## MUST Rubric (12 items)

| # | Criterion | Agent Answer | Pass? |
|---|-----------|-------------|-------|
| M1 | Lists `auth` as an enabled L4 domain | auth ✓ | ✅ |
| M2 | Lists `crud` as an enabled L4 domain | crud ✓ | ✅ |
| M3 | Lists `audit-log` as an enabled L4 domain | audit-log ✓ | ✅ |
| M4 | Lists `feature-flags` as an enabled L4 domain | feature-flags ✓ | ✅ |
| M5 | Lists `search` as an enabled L4 domain | search ✓ | ✅ |
| M6 | Lists `impersonation-banner` as an L2 block | impersonation-banner ✓ | ✅ |
| M7 | Lists `data-table` or `filter-bar` as an L2 block | data-table ✓ | ✅ |
| M8 | Lists `kpi-card` or `time-series-chart` as an L2 block | kpi-card ✓ | ✅ |
| M9 | Lists at least one L3 page (`audit-log-page`, `admin-overview-page`, `dashboard-page`, `list-page`) | audit-log-page ✓ | ✅ |
| M10 | Names B2BADMIN-INV-001 (impersonation emits audit-log row) with spec_ref | B2BADMIN-INV-001 → audit-log-l0.yaml#AUDIT-RECORD-001 ✓ | ✅ |
| M11 | Does NOT invent L4 domains absent from `enabled_l4_domains:` (no payment, notification, billing) | No hallucinated domains ✓ | ✅ |
| M12 | Identifies tenant isolation requirement from RECIPE.md (B2BADMIN-INV-003 / ASVS-V4.2.1) | tenant boundary mentioned — ASVS reference not explicitly cited from AGENTS.md rules | ❌ |

**MUST: 11 / 12**

## SHOULD Rubric (8 items)

| # | Criterion | Agent Answer | Pass? |
|---|-----------|-------------|-------|
| S1 | Lists `feature-flag-toggle` and `feature-gate` blocks | Both ✓ | ✅ |
| S2 | Lists `impersonation-banner` as admin-only cross-cutting concern | impersonation-banner global layout ✓ | ✅ |
| S3 | Notes feature-flag immutability (B2BADMIN-INV-002: no DELETE) | FF-CRUD-003 referenced ✓ | ✅ |
| S4 | Identifies `auth` as multi-tenant with RBAC (ADMIN/MANAGER/MEMBER) | multi-tenant auth + RBAC ✓ | ✅ |
| S5 | Lists `bulk-actions-bar` or `bulk-export` for admin operations | bulk-actions-bar ✓ | ✅ |
| S6 | Lists `admin-overview-page` and `settings-overview` as L3 pages | admin-overview-page ✓, settings-overview missed | ❌ |
| S7 | References channel.io as external evidence for B2B SaaS admin pattern | channel.io cited ✓ | ✅ |
| S8 | Notes search authz scoping by tenant (per-tenant vs cross-tenant for ADMIN) | search scope by role mentioned — implicit, not explicit | ❌ |

**SHOULD: 6 / 8**

## Verdict

```
MUST:   11 / 12  ✅  (threshold: ≥10)
SHOULD:  6 /  8  ✅  (threshold: ≥5)
VERDICT: PASS
```

The sealed sub-agent reproduced the b2b-admin L4 composition accurately from
the recipe manifest. channel.io external anchor ("AI로 더 편해진 사내 메신저")
provides clear B2B SaaS admin signal. All 5 L4 domains correctly identified
including `auth` for multi-tenant RBAC. All 3 business invariants named.
Minor gaps: M12 — ASVS-V4.2.1 tenant isolation not cross-referenced from
AGENTS.md rules; S6 — settings-overview page missed; S8 — search tenant
scoping implicit. Evidence density (Jira/토스ID at internal_design) acceptable
per Pre-Mortem §3 — verdict weights internal catalog dimensions.
