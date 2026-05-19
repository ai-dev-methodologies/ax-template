---
pattern: marketplace
display_name: "Marketplace (Listings + Bids + Escrow + Ratings)"
tenant_model: single  # iter-2: explicit declaration per specs/multi-tenant-l0.yaml#MULTI-TENANT-ISOLATION-DEFAULT-001. Recipe ships single-tenant; fork-receivers adopting multi-tenant MUST switch to `tenant_model: multi` AND adopt ISOLATION-001/002/003 + PROPAGATION-001/002.
schema_version: 1
compatible_with_catalog_version: "v1.3.0-business-patterns"
last_verified_at: "2026-05-18"
enabled_l4_domains:
  - audit-log
  - crud
  - notification
  - payment
  - search
l2_blocks_used:
  - confirm-dialog
  - crud-create-form
  - crud-edit-form
  - crud-list-adapter
  - data-table
  - faceted-filter
  - feature-flag-toggle
  - feature-gate
  - filter-bar
  - kpi-card
  - notification-bell
  - notification-list
  - payment-checkout-form
  - payment-method-picker
  - search-input
  - search-palette
l3_pages_used:
  - create-page
  - dashboard-page
  - detail-page
  - list-page
  - search-results-page
override_allowed:
  # Inline override block — no separate RECIPE_DEVIATION.md file.
  #
  # enabled_l4_domains:
  #   skip: ["feature-flags"]
  #   rationale: "C2C-only no-fiat marketplace; no KYC gating required."
  #   citation: "<internal ticket / PR url>"
---

## Backend Implementation Status

> See [`docs/IMPLEMENTATION-STATUS.md`](../../docs/IMPLEMENTATION-STATUS.md) for the full 12-L4 status taxonomy and fork-receiver expectation alignment (R15+ mandatory section).

| L4 domain | Status | Effort if not impl |
|---|---|---|
| `audit-log` | **spec-only** 📋 | ~5-10 eng-days (implement backend) |
| `crud` | **impl** ✅ | — (ready) |
| `notification` | **spec-only** 📋 | ~5-10 eng-days (implement backend) |
| `payment` | **impl** ✅ | — (ready) |
| `search` | **spec-only** 📋 | ~5-10 eng-days (implement backend) |

**Summary**: 2 impl ready · 3 spec-only (implement) · 0 skeleton (flesh out) · est. ~19-26 engineering days for the gap.

**Reading guide**: `impl` = backend Java reference workload ready in `backend/src/main/java/com/ax/template/authblueprint/<domain>/`. `spec-only` = Spec Trio + Next.js stub only; backend NOT included. `skeleton` = `.skeleton` file present; flesh out controller/service yourself. Sealed verdict PASS validates catalog self-discoverability, NOT runnable backend code.


# Recipe: marketplace

**Business context:** C2C/B2C marketplace — product listings, bids, escrow-style
payment release, dispute window, ratings, and KYC-gated high-value listing creation.

## Enabled L4 Domains

| L4 Domain | Role in this recipe |
|---|---|
| `crud` | Listing, order, and rating CRUD operations |
| `payment` | Escrow payment capture, buyer-confirmation release, dispute refund |
| `search` | Full-text listing search, faceted filtering (category, price, location) |
| `notification` | Bid alert, order confirmed, escrow released, dispute opened |
| `audit-log` | Immutable record of escrow state changes, moderation actions |

> **KYC (optional):** `feature-flags` L4 is recommended for high-value listing creation
> gating. Bind to `specs/identity-verification-l0.yaml` IDV spec downstream;
> no new L4 added — feature-flag + audit-log cover the requirement at recipe level.

## Business Invariants

| ID | Statement | Binding |
|---|---|---|
| MARKETPLACE-INV-001 | Escrow funds released only after buyer confirmation OR dispute-window expiry | `spec_ref: specs/payment-l0.yaml#PAYMENT-STATE-002` + `spec_ref: specs/payment-l0.yaml#PAYMENT-REFUND-001` + `spec_ref: specs/recipes/marketplace-recipe-l0.yaml#MARKETPLACE-ESCROW-LIFECYCLE-001` |
| MARKETPLACE-INV-002 | Listing search results exclude soft-deleted + moderator-hidden listings | `spec_ref: specs/search-l0.yaml#SEARCH-AUTHZ-001` |
| MARKETPLACE-INV-003 | KYC required before high-value listing creation (threshold via feature-flag) | `spec_ref: specs/feature-flags-l0.yaml#FF-AUTHZ-001` + `rule_ref: practices/rules/no-rrn-collection-without-legal-basis.md` |

## Business Observability (advisory — no emitter test enforced this cycle)

| Signal | Type | Notes |
|---|---|---|
| `recipe.marketplace.escrow_pending_total` | Gauge | Funds currently held in escrow |
| `marketplace.listing.created_total{category}` | Counter | New listings by category |
| `marketplace.dispute.opened_total` | Counter | Disputes opened |

## Evidence

```yaml
evidence:
  - provenance_class: external
    source: "Etsy Developer API"
    url: "https://developers.etsy.com/documentation/"
    citation: "a REST API that extends support for inventory, sales orders, and shop management"
    quoted_at: "2026-05-18"
  - provenance_class: external
    source: "Stripe Connect — Marketplace payments"
    url: "https://docs.stripe.com/connect"
    citation: "Collect payments from customers and automatically pay out a portion to sellers or service providers on your marketplace."
    quoted_at: "2026-05-18"
  - provenance_class: internal_design
    source: "당근마켓 (Karrot) — Korean C2C marketplace"
    rationale: "No public API documentation URL. Pattern modeled from internal_design: local listing search, escrow-lite trust mechanism, KYC for high-value transactions."
  - provenance_class: internal_design
    source: "번개장터 (Bunjang) — Korean C2C marketplace"
    rationale: "No public API documentation URL. Pattern modeled from internal_design: category faceted search, bid/buy-now flow, seller rating."
  - provenance_class: internal_design
    derives_from:
      - "SP15 crud"
      - "SP30 payment"
      - "SP26 search"
      - "SP17 audit-log"
    rationale: "Composition derives from existing L4 payment (escrow flow), search (authz filter), audit-log, and crud specs."
```

## Scaffold Usage

```bash
/ax-scaffold business marketplace my-marketplace
```

This will scaffold all 5 enabled L4 domains into `my-marketplace/` and run
`/ax-verify-domain` for each one.
