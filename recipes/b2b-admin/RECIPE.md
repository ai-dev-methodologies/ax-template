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
  # R39 (2026-05-23): R29–R36 domains absorbed via backend-only L4 stubs.
  # Each templates/L4/<domain>/README.md carries the tenant_model declaration;
  # the full Next.js tree is deferred to per-domain follow-up PRs.
  - api-key
  - approval-workflow
  - session-management
  - activity-feed
  - comment-thread
  - tag-categorization
  - favorites-bookmarks
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

| L4 domain | Status | Frontend stub | Effort if not impl |
|---|---|---|---|
| `audit-log` | **impl** ✅ | full-trio | — (ready; `./gradlew testAuditLog` GREEN) |
| `auth` | **impl** ✅ | full-trio | — (ready) |
| `crud` | **impl** ✅ | full-trio | — (ready) |
| `feature-flags` | **impl** ✅ | full-trio | — (ready; `./gradlew testFeatureFlags` GREEN) |
| `search` | **impl** ✅ | full-trio | — (ready; `./gradlew testSearch` GREEN) |
| `api-key` *(R30 / R40)* | **impl** ✅ | **full-trio** | — (ready; `./gradlew testApiKey` GREEN); R40 promoted from backend-only stub |
| `approval-workflow` *(R31 / R43)* | **impl** ✅ | **full-trio** | — (ready; `./gradlew testApprovalWorkflow` GREEN); R43 promoted from backend-only stub through 7-iter 2-persona dogfood (sales-mgr + CFO) to GREEN |
| `session-management` *(R33 / R41)* | **impl** ✅ | **full-trio** | — (ready; `./gradlew testSessionManagement` GREEN); R41 promoted from backend-only stub (PII-at-DTO UI demonstration) |
| `activity-feed` *(R35 / R44)* | **impl** ✅ | **full-trio** | — (ready; `./gradlew testActivityFeed` GREEN); R44 promoted via 4-iter 2-persona dogfood (heavy-user + compliance officer); closes 2 critical (mark-all UI lie + fabricated readAt audit-timeline tampering) |
| `comment-thread` *(R36 / R42)* | **impl** ✅ | **full-trio** | — (ready; `./gradlew testCommentThread` GREEN); R42 promoted from backend-only stub (soft-delete + admin-cannot-rewrite UI demonstration) |
| `tag-categorization` *(R34 / R39)* | **impl** ✅ | **backend-only stub** | — (ready; `./gradlew testTagCategorization` GREEN); Next.js tree deferred |
| `favorites-bookmarks` *(R35a / R39)* | **impl** ✅ | **backend-only stub** | — (ready; `./gradlew testFavorites` GREEN); Next.js tree deferred |

**Summary**: 12 impl ready · 0 spec-only · 0 skeleton · est. 0 eng-days for the recipe backend gap. 5 full-trio L4s + 7 backend-only L4 stubs (R39, each `templates/L4/<domain>/README.md` only) totaling 12 in `enabled_l4_domains`. Multi-tenant infra (row-level filter + AOP guard + tenant propagation) remains fork-receiver responsibility per `tenant_model: multi` declaration above.

### Backend-only L4 stub convention (R39)

For the 7 R29–R36 domains, `templates/L4/<domain>/` contains only a `README.md` carrying:

- the canonical `**Tenant model**: single … MULTI-TENANT-ISOLATION-DEFAULT-001` declaration (`l4_readme_tenant_model_declaration_guard` requirement),
- a backend reference block pointing at the Java package + spec YAML + `./gradlew test<Domain>` task + violation-proof test,
- a "why backend-only at this stage" note explaining the deferral.

The full Next.js tree (app/, next.config.ts, providers) is intentionally not shipped — fork-receivers wanting a UI should clone `templates/L4/audit-log/` (or `feature-flags/`) and adapt to this domain's REST surface. The MULTI-TENANT-INTEGRATION-001..005 cross-domain invariants below define the contract surface that any future frontend tree MUST respect.

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
