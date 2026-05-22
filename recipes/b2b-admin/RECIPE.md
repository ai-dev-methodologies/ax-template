---
pattern: b2b-admin
display_name: "B2B Admin (Multi-tenant Ops + Analytics + Audit + Impersonation + R29–R36 admin surface)"
schema_version: 1
compatible_with_catalog_version: "v1.3.0-business-patterns"
last_verified_at: "2026-05-22"
enabled_l4_domains:
  - audit-log
  - auth
  - crud
  - feature-flags
  - search
  # R29–R36 domains (api-key, approval-workflow, session-management,
  # activity-feed, comment-thread, tag-categorization, favorites-bookmarks)
  # are NOT listed here yet — they lack templates/L4/<domain>/ stubs and
  # would fail recipe_spec_referential_integrity. They integrate with b2b-admin
  # via MULTI-TENANT-INTEGRATION-001..005 below as cross-domain invariants.
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
tenant_model: multi   # R16: explicit declaration per MULTI-TENANT-ISOLATION-DEFAULT-001. Fork-receiver MUST adopt ISOLATION-001 (row-level) OR ISOLATION-002 (schema-per-tenant) + ISOLATION-003 (AOP guard) + PROPAGATION-001/002 before production use. backend/src/main/java currently single-tenant — multi-tenant infra is fork-receiver responsibility (~10-15 eng-days).
---

## Backend Implementation Status

> See [`docs/IMPLEMENTATION-STATUS.md`](../../docs/IMPLEMENTATION-STATUS.md) for the full 12-L4 status taxonomy and fork-receiver expectation alignment (R15+ mandatory section).

| L4 domain | Status | Effort if not impl |
|---|---|---|
| `audit-log` | **impl** ✅ | — (ready; `./gradlew testAuditLog` GREEN) |
| `auth` | **impl** ✅ | — (ready) |
| `crud` | **impl** ✅ | — (ready) |
| `feature-flags` | **impl** ✅ | — (ready; `./gradlew testFeatureFlags` GREEN) |
| `search` | **impl** ✅ | — (ready; `./gradlew testSearch` GREEN) |

**Summary**: 5 impl ready · 0 spec-only · 0 skeleton · est. 0 eng-days for the recipe gap. Multi-tenant infra (row-level filter + AOP guard + tenant propagation) remains fork-receiver responsibility per `tenant_model: multi` declaration above.

### R29–R36 cross-domain integration (backend GREEN, frontend L4 stubs pending)

Backend GREEN for: `api-key` (R30), `approval-workflow` (R31), `session-management` (R33), `tag-categorization` (R34), `activity-feed` (R35), `favorites-bookmarks` (R35a), `comment-thread` (R36) — each via its own `./gradlew test{Domain}` task. They integrate with b2b-admin via the MULTI-TENANT-INTEGRATION-001..005 cross-domain invariants below. Next PR (R39+): ship `templates/L4/<domain>/` frontend stubs and promote them into `enabled_l4_domains`.

**Reading guide**: `impl` = backend Java reference workload ready in `backend/src/main/java/com/ax/template/authblueprint/<domain>/`. `spec-only` = Spec Trio + Next.js stub only; backend NOT included. `skeleton` = `.skeleton` file present; flesh out controller/service yourself. Sealed verdict PASS validates catalog self-discoverability, NOT runnable backend code.


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
| `api-key` *(R30)* | Admin manages B2B integration credentials per tenant; bcrypt-hashed secrets + last-used telemetry |
| `approval-workflow` *(R31)* | Multi-step admin operations (cross-tenant moves, plan upgrades, dangerous deletes) with two-person review |
| `session-management` *(R33)* | Admin force-logout, per-user device inventory, masked IP/UA for forensics |
| `activity-feed` *(R35)* | Admin operations stream; polymorphic (object_type, object_id) fan-in |
| `comment-thread` *(R36)* | Internal admin notes on tenants/users/approval-requests with soft-delete audit trail |
| `tag-categorization` *(R34)* | Tenant-scoped tag library applied to any (entity_type, entity_id) admin object |
| `favorites-bookmarks` *(R35a)* | Per-admin saved filters/tenants/dashboards; IDOR-safe caller-only access |

## Business Invariants

| ID | Statement | Binding |
|---|---|---|
| B2BADMIN-INV-001 | Impersonation events always emit audit-log row with `impersonator_id`, `impersonated_id`, `started_at`, `ended_at` | `spec_ref: specs/audit-log-l0.yaml#AUDIT-RECORD-001` + `spec_ref: specs/audit-log-l0.yaml#AUDIT-RECORD-002` |
| B2BADMIN-INV-002 | Tenant-scoped feature-flag changes are immutable history (no destructive delete) | `spec_ref: specs/feature-flags-l0.yaml#FF-CRUD-003` + `spec_ref: specs/audit-log-l0.yaml#AUDIT-RETENTION-001` |
| B2BADMIN-INV-003 | KPI aggregation respects tenant boundary (no cross-tenant leakage) | `spec_ref: specs/multi-tenant-l0.yaml#MULTI-TENANT-ISOLATION-001` + `spec_ref: specs/multi-tenant-l0.yaml#MULTI-TENANT-ISOLATION-003` + `spec_ref: specs/multi-tenant-l0.yaml#MULTI-TENANT-PROPAGATION-001` (R16 re-anchored — see disambiguation below) |
| MULTI-TENANT-INTEGRATION-001 | Every R29–R36 entity adopted above MUST carry `tenant_id` and be filtered by AOP `@TenantScoped` guard before any business read | `spec_ref: specs/multi-tenant-l0.yaml#MULTI-TENANT-ISOLATION-001` + `spec_ref: specs/multi-tenant-l0.yaml#MULTI-TENANT-ISOLATION-003` |
| MULTI-TENANT-INTEGRATION-002 | Caller-authentication-only authorization (no `?userId=` parameters) applies to every absorbed domain's IDOR-sensitive endpoints (favorites, sessions own-only, comments author-only edit) | `practices/rules/caller-authentication-only-no-userid-param.md` + `spec_ref: specs/favorites-bookmarks-l0.yaml#FAV-AUTHZ-002` + `spec_ref: specs/session-management-l0.yaml#SESS-AUTHZ-001` + `spec_ref: specs/comment-thread-l0.yaml#COMMENT-AUTHZ-002` |
| MULTI-TENANT-INTEGRATION-003 | PII at DTO boundary (IP masked, UA summarized, plaintext credentials NEVER returned) for session-management + api-key admin surfaces | `practices/rules/pii-masked-at-dto-boundary.md` + `spec_ref: specs/session-management-l0.yaml#SESS-INTROSPECT-002` + `spec_ref: specs/api-key-l0.yaml#APIKEY-CRUD-002` |
| MULTI-TENANT-INTEGRATION-004 | Soft-delete with body→NULL preserves audit trail across tenant boundary for comment-thread; admin cannot rewrite user content even with elevated tenant role | `practices/rules/soft-delete-audit-trail.md` + `practices/rules/admin-cannot-rewrite-user-content.md` + `spec_ref: specs/comment-thread-l0.yaml#COMMENT-CRUD-003` + `spec_ref: specs/comment-thread-l0.yaml#COMMENT-AUTHZ-002` |
| MULTI-TENANT-INTEGRATION-005 | HTTP DELETE on absorbed domains returns 204 even when target row is absent (RFC 9110 §9.3.5 idempotency), but tenant-scope check still applies before the 204 | `practices/rules/http-delete-idempotency-rfc9110.md` + `spec_ref: specs/favorites-bookmarks-l0.yaml#FAV-CRUD-002` + `spec_ref: specs/multi-tenant-l0.yaml#MULTI-TENANT-ISOLATION-003` |

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
