---
pattern: b2b-admin
display_name: "B2B Admin (Multi-tenant Ops + Analytics + Audit + Impersonation)"
schema_version: 1
compatible_with_catalog_version: "v1.3.0-business-patterns"
last_verified_at: "2026-05-18"
enabled_l4_domains:
  - audit-log
  - auth
  - crud
  - feature-flags
  - search
l2_blocks_used:
  - bulk-actions-bar
  - bulk-export
  - column-picker
  - column-reorder
  - data-table
  - feature-flag-toggle
  - feature-gate
  - filter-bar
  - impersonation-banner
  - kpi-card
  - saved-filters
  - saved-view
  - search-palette
  - time-series-chart
l3_pages_used:
  - admin-overview-page
  - audit-log-page
  - dashboard-page
  - detail-page
  - list-page
  - settings-overview
override_allowed:
  # Inline override block — no separate RECIPE_DEVIATION.md file.
  #
  # enabled_l4_domains:
  #   skip: ["search"]
  #   rationale: "Single-tenant deployment; no cross-tenant search needed."
  #   citation: "<internal ticket / PR url>"
---

# Recipe: b2b-admin

**Business context:** Multi-tenant SaaS operations dashboard — tenant management,
KPI analytics, immutable audit trail, impersonation for support, and tenant-scoped
feature-flag administration.

## Enabled L4 Domains

| L4 Domain | Role in this recipe |
|---|---|
| `auth` | Multi-tenant auth, RBAC, impersonation session management |
| `crud` | Tenant CRUD, user management, org-level settings |
| `audit-log` | Immutable impersonation events, feature-flag change history |
| `feature-flags` | Tenant-scoped flag management, per-tenant rollouts |
| `search` | Cross-tenant search (ADMIN only), per-tenant entity search |

## Business Invariants

| ID | Statement | Binding |
|---|---|---|
| B2BADMIN-INV-001 | Impersonation events always emit audit-log row with `impersonator_id`, `impersonated_id`, `started_at`, `ended_at` | `spec_ref: specs/audit-log-l0.yaml#AUDIT-RECORD-001` + `spec_ref: specs/audit-log-l0.yaml#AUDIT-RECORD-002` |
| B2BADMIN-INV-002 | Tenant-scoped feature-flag changes are immutable history (no destructive delete) | `spec_ref: specs/feature-flags-l0.yaml#FF-CRUD-003` + `spec_ref: specs/audit-log-l0.yaml#AUDIT-RETENTION-001` |
| B2BADMIN-INV-003 | KPI aggregation respects tenant boundary (no cross-tenant leakage) | `spec_ref: specs/auth-asvs-l1.yaml#ASVS-V4.2.1` + `spec_ref: specs/auth-asvs-l1.yaml#ASVS-V4.2.2` |

## Business Observability (advisory — no emitter test enforced this cycle)

| Signal | Type | Notes |
|---|---|---|
| `recipe.b2b_admin.impersonation_total` | Counter | Impersonation sessions started |
| `b2b_admin.tenant.active_total` | Gauge | Active tenant count |
| `b2b_admin.feature_flag.change_total{flag_key}` | Counter | Flag mutations by key |

## Evidence

```yaml
evidence:
  - provenance_class: external
    source: "channel.io (채널코퍼레이션) — Korean B2B SaaS"
    url: "https://channel.io/ko"
    citation: "AI로 더 편해진 사내 메신저"
    quoted_at: "2026-05-18"
    fidelity_note: "Verbatim Korean tagline confirming B2B SaaS multi-tenant ops context. Admin dashboard pattern (tenant management + audit trail) derived from channel.io's operator console product direction."
  - provenance_class: internal_design
    source: "Jira REST API v3 (Atlassian)"
    url: "https://developer.atlassian.com/cloud/jira/platform/rest/v3/"
    rationale: "Content truncated / 404 on 3 fetch attempts (2026-05-18). Admin issue-tracker pattern (audit trail, impersonation for support) modeled from internal_design."
  - provenance_class: internal_design
    source: "토스ID (Toss Identity) — Korean enterprise identity"
    rationale: "No public API documentation URL. Multi-tenant RBAC and impersonation patterns modeled from internal_design."
  - provenance_class: internal_design
    derives_from:
      - "SP34 impersonation-banner"
      - "SP17 audit-log"
      - "SP26 feature-flags"
      - "auth ASVS L1 spec"
    rationale: "Composition derives from existing L4 auth (ASVS V4.2.1/V4.2.2 tenant isolation), audit-log, feature-flags, and SP34 impersonation-banner block."
```

## Scaffold Usage

```bash
/ax-scaffold business b2b-admin my-admin-portal
```

This will scaffold all 5 enabled L4 domains into `my-admin-portal/` and run
`/ax-verify-domain` for each one.
