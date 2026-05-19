---
pattern: crm
display_name: "CRM (Lead → Contact → Deal → Activity)"
tenant_model: single  # iter-2: explicit declaration per specs/multi-tenant-l0.yaml#MULTI-TENANT-ISOLATION-DEFAULT-001. Recipe ships single-tenant; fork-receivers adopting multi-tenant MUST switch to `tenant_model: multi` AND adopt ISOLATION-001/002/003 + PROPAGATION-001/002.
schema_version: 1
compatible_with_catalog_version: "v1.2.0-p1-absorbed"
last_verified_at: "2026-05-18"
enabled_l4_domains:
  - crud
  - audit-log
  - notification
  - search
l2_blocks_used:
  - data-table
  - activity-feed
  - filter-bar
  - saved-view
  - kpi-card
  - event-stream
l3_pages_used:
  - list-page
  - detail-page
  - create-page
  - dashboard-page
override_allowed:
  # Inline override block — no separate RECIPE_DEVIATION.md file.
  # Uncomment and fill in a fork-receiver's project if deviating from this recipe.
  #
  # enabled_l4_domains:
  #   skip: ["search"]
  #   rationale: "CRM data is small; full-text search not needed."
  #   citation: "<internal ticket / PR url>"
  #
  # l2_blocks_used:
  #   skip: ["event-stream"]
  #   rationale: "No real-time activity feed needed; periodic polling sufficient."
  #   citation: "<internal ticket / PR url>"
---

## Backend Implementation Status

> See [`docs/IMPLEMENTATION-STATUS.md`](../../docs/IMPLEMENTATION-STATUS.md) for the full 12-L4 status taxonomy and fork-receiver expectation alignment (R15+ mandatory section).

| L4 domain | Status | Effort if not impl |
|---|---|---|
| `crud` | **impl** ✅ | — (ready) |
| `audit-log` | **spec-only** 📋 | ~5-10 eng-days (implement backend) |
| `notification` | **spec-only** 📋 | ~5-10 eng-days (implement backend) |
| `search` | **spec-only** 📋 | ~5-10 eng-days (implement backend) |

**Summary**: 1 impl ready · 3 spec-only (implement) · 0 skeleton (flesh out) · est. ~19-26 engineering days for the gap.

**Reading guide**: `impl` = backend Java reference workload ready in `backend/src/main/java/com/ax/template/authblueprint/<domain>/`. `spec-only` = Spec Trio + Next.js stub only; backend NOT included. `skeleton` = `.skeleton` file present; flesh out controller/service yourself. Sealed verdict PASS validates catalog self-discoverability, NOT runnable backend code.


# Recipe: crm

**Business context:** Sales pipeline — lead → contact → deal → activity. Track interactions, manage deal stages, record all pipeline events.

## Enabled L4 Domains

| L4 Domain | Role in this recipe |
|---|---|
| `crud` | Lead, contact, deal, activity CRUD operations |
| `audit-log` | Immutable record of pipeline stage transitions and record changes |
| `notification` | Deal stage change alerts, activity reminders |
| `search` | Full-text search across leads, contacts, deals |

## Business Invariants

| ID | Statement | Binding |
|---|---|---|
| CRM-INV-001 | `deal.amount` must be currency-precision compliant | `rule_ref: practices/rules/idempotency-key-on-mutations.md` + `spec_ref: specs/crud-security.yaml` |
| CRM-INV-002 | Lead → contact conversion preserves `source_attribution` | `spec_ref: specs/audit-log-l0.yaml` |
| CRM-INV-003 | `activity.timestamp ≥ activity.created_at` | `spec_ref: specs/crud-security.yaml` |
| CRM-INV-004 | Deal stage transitions follow declared state machine; transitions logged | `spec_ref: specs/audit-log-l0.yaml` |

## Business Observability (advisory — no emitter test enforced this cycle)

| Signal | Type | Notes |
|---|---|---|
| `crm.lead.captured_total{source}` | Counter | Leads captured, by source |
| `crm.deal.stage_transitioned_total{from,to}` | Counter | Stage transitions by pair |
| `crm.deal.win_rate` | Gauge | Win rate (won / (won + lost)) |
| `crm.activity.completed_total{type}` | Counter | Activities completed, by type |

## Evidence

```yaml
evidence:
  - provenance_class: external
    source: "Salesforce Object Reference — Opportunity (Deal) data model"
    url: "https://developer.salesforce.com/docs/atlas.en-us.object_reference.meta/object_reference/sforce_api_objects_opportunity.htm"
    citation: "Represents an opportunity, which is a sale or pending deal."
    quoted_at: 2026-05-18
  - provenance_class: external
    source: "HubSpot CRM API — Deals overview"
    url: "https://developers.hubspot.com/docs/api/crm/deals"
    citation: "Deals represent ongoing transactions that sales teams are pursuing."
    quoted_at: 2026-05-18
  - provenance_class: external
    source: "Channel Talk — 비즈니스 메신저 (landing tagline)"
    url: "https://channel.io/ko"
    citation: "고객 상담의 미래는 AI입니다"
    quoted_at: 2026-05-18
    fidelity_note: "iter 3 — verbatim landing-page tagline replaces iter-2 paraphrase about 고객 데이터/영업 단계/활동 이력 (paraphrase not found verbatim on landing page). Reference retained as inspirational anchor (Korean CRM vendor) plus this short page tagline."
  - provenance_class: internal_design
    derives_from:
      - "SP15 crud"
      - "SP26 search"
      - "SP17 audit-log"
    rationale: "Composition selected from existing L4 baseline; pipeline state machine ships as code-of-customer, not as catalog primitive."
```

## Scaffold Usage

```bash
/ax-scaffold business crm my-crm-app
```

This will scaffold all 4 enabled L4 domains into `my-crm-app/` and run
`/ax-verify-domain` for each one.
