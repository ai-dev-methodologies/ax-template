---
pattern: saas-subscription
display_name: "Multi-tenant SaaS Subscription"
tenant_model: single  # iter-2: SaaS subscription patterns ARE COMMONLY multi-tenant in production B2B SaaS. Declared as `single` here because backend/src/main/java reference impl is single-tenant. Fork-receivers building a real subscription product MUST stop and decide: `single` (B2C-style per-user subscription) OR `multi` (B2B per-tenant subscription billing with cross-tenant isolation per ISOLATION-001/002/003 + PROPAGATION-001/002). Choice is non-trivial and affects billing schema, RBAC, and tenant context propagation across async billing events.
schema_version: 1
compatible_with_catalog_version: "v1.2.0-p1-absorbed"
last_verified_at: "2026-05-18"
enabled_l4_domains:
  - billing
  - auth
  - feature-flags
  - notification
  - audit-log
l2_blocks_used:
  - pricing-table
  - plan-comparison
  - usage-meter
  - invoice-list
  - billing-history
  - feature-flag-toggle
  - feature-gate
  - kpi-card
l3_pages_used:
  - pricing-page
  - settings-overview
  - admin-overview-page
override_allowed:
  # Inline override block — no separate RECIPE_DEVIATION.md file.
  # Uncomment and fill in a fork-receiver's project if deviating from this recipe.
  # Format: <field-path>: <rationale + url-or-citation>
  #
  # enabled_l4_domains:
  #   skip: ["audit-log"]
  #   rationale: "Project X uses external SIEM via webhook; audit-log L4 redundant."
  #   citation: "<internal ticket / PR url>"
  #
  # l2_blocks_used:
  #   skip: ["usage-meter"]
  #   rationale: "Metering handled by external provider; usage-meter block not needed."
  #   citation: "<internal ticket / PR url>"
---

## Backend Implementation Status

> See [`docs/IMPLEMENTATION-STATUS.md`](../../docs/IMPLEMENTATION-STATUS.md) for the full 12-L4 status taxonomy and fork-receiver expectation alignment (R15+ mandatory section).

| L4 domain | Status | Effort if not impl |
|---|---|---|
| `billing` | **spec-only** 📋 | ~5-10 eng-days (implement backend) |
| `auth` | **impl** ✅ | — (ready) |
| `feature-flags` | **spec-only** 📋 | ~5-10 eng-days (implement backend) |
| `notification` | **spec-only** 📋 | ~5-10 eng-days (implement backend) |
| `audit-log` | **spec-only** 📋 | ~5-10 eng-days (implement backend) |

**Summary**: 1 impl ready · 4 spec-only (implement) · 0 skeleton (flesh out) · est. ~26-33 engineering days for the gap.

**Reading guide**: `impl` = backend Java reference workload ready in `backend/src/main/java/com/ax/template/authblueprint/<domain>/`. `spec-only` = Spec Trio + Next.js stub only; backend NOT included. `skeleton` = `.skeleton` file present; flesh out controller/service yourself. Sealed verdict PASS validates catalog self-discoverability, NOT runnable backend code.


# Recipe: saas-subscription

**Business context:** Multi-tenant B2B SaaS with plan tiers, usage metering, recurring invoicing.

## Enabled L4 Domains

| L4 Domain | Role in this recipe |
|---|---|
| `billing` | Recurring subscription lifecycle, plan tiers, invoicing |
| `auth` | Multi-tenant identity, JWT with `tenantId` claim, RBAC |
| `feature-flags` | Plan-gated feature enforcement |
| `notification` | Billing alerts, trial expiration, invoice delivery |
| `audit-log` | Immutable record of subscription state changes |

## Business Invariants

| ID | Statement | Binding |
|---|---|---|
| SAAS-INV-001 | Subscription must have ≥1 active plan | `spec_ref: specs/billing-l0.yaml#BILLING-AUTHZ-002` + `rule_ref: practices/rules/billing-event-idempotent.md` |
| SAAS-INV-002 | Usage metering resets on billing cycle boundary | `rule_ref: practices/rules/billing-event-idempotent.md` |
| SAAS-INV-003 | Feature-gate enforcement matches plan tier | `spec_ref: specs/feature-flags-l0.yaml` |
| SAAS-INV-004 | Tenant downgrade preserves historical usage logs | `spec_ref: specs/audit-log-l0.yaml` |

## Business Observability (advisory — no emitter test enforced this cycle)

| Signal | Type | Notes |
|---|---|---|
| `saas.mrr.calculated_daily_count` | Counter | Daily MRR calculation event |
| `saas.tenant.active_subscription_count` | Gauge | Live subscription count |
| `saas.usage_metering.cycle_reset_total` | Counter | Resets per billing cycle |
| `saas.plan.upgrade_total{from,to}` | Counter | Upgrade events by plan pair |

## Evidence

```yaml
evidence:
  - provenance_class: external
    source: "Stripe Billing — Subscriptions overview"
    url: "https://stripe.com/docs/billing/subscriptions/overview"
    citation: "Subscriptions allow you to charge a customer on a recurring basis."
    quoted_at: 2026-05-18
  - provenance_class: external
    source: "Toss Payments — 자동결제 (Recurring) Guide"
    url: "https://docs.tosspayments.com/guides/v2/billing"
    citation: "자동결제는 정기 배송, 음악 스트리밍과 같은 구독형 서비스에서 사용할 수 있어요."
    quoted_at: 2026-05-18
    fidelity_note: "iter 3 — URL corrected from /billing/overview (404) to /billing; verbatim snippet re-fetched from current page body."
  - provenance_class: internal_design
    derives_from:
      - "SP30 billing"
      - "SP28 feature-flags"
    rationale: "Composition selected from existing L4 baseline; no novel logic."
```

## Scaffold Usage

```bash
/ax-scaffold business saas-subscription my-saas-app
```

This will scaffold all 5 enabled L4 domains into `my-saas-app/` and run
`/ax-verify-domain` for each one.
