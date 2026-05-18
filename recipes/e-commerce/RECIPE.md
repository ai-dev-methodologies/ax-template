---
pattern: e-commerce
display_name: "E-commerce (Product + Cart + Checkout + Orders)"
schema_version: 1
compatible_with_catalog_version: "v1.2.0-p1-absorbed"
last_verified_at: "2026-05-18"
enabled_l4_domains:
  - crud
  - payment
  - notification
  - audit-log
  - search
l2_blocks_used:
  - crud-list-adapter
  - crud-create-form
  - data-table
  - filter-bar
  - faceted-filter
  - kpi-card
  - event-stream
l3_pages_used:
  - list-page
  - detail-page
  - create-page
  - edit-page
  - search-results-page
override_allowed:
  # Inline override block — no separate RECIPE_DEVIATION.md file.
  # Uncomment and fill in a fork-receiver's project if deviating from this recipe.
  #
  # enabled_l4_domains:
  #   skip: ["search"]
  #   rationale: "Product catalog is small (<500 items); full-text search not needed."
  #   citation: "<internal ticket / PR url>"
  #
  # l2_blocks_used:
  #   skip: ["event-stream"]
  #   rationale: "No real-time order status; polling is sufficient."
  #   citation: "<internal ticket / PR url>"
---

# Recipe: e-commerce

**Business context:** Product catalog + cart + checkout + order management + inventory.

## Enabled L4 Domains

| L4 Domain | Role in this recipe |
|---|---|
| `crud` | Product, order, inventory CRUD operations |
| `payment` | Checkout payment capture, refunds |
| `notification` | Order confirmation, shipping updates |
| `audit-log` | Immutable record of order mutations, refunds |
| `search` | Product full-text search, faceted filtering |

## Business Invariants

| ID | Statement | Binding |
|---|---|---|
| ECOM-INV-001 | `order.total_amount == sum(items.unit_price × items.quantity)` | `rule_ref: practices/rules/idempotency-key-on-mutations.md` + `spec_ref: specs/payment-l0.yaml` |
| ECOM-INV-002 | `payment.captured ⇒ order.confirmed` (atomic) | `spec_ref: specs/payment-l0.yaml` + `rule_ref: practices/rules/api-idempotency-key-required.md` |
| ECOM-INV-003 | All mutating endpoints require idempotency key | `rule_ref: practices/rules/api-idempotency-key-required.md` |
| ECOM-INV-004 | Cancellation/refund actions logged immutably | `spec_ref: specs/audit-log-l0.yaml` |

## Business Observability (advisory — no emitter test enforced this cycle)

| Signal | Type | Notes |
|---|---|---|
| `ecommerce.order.placed_total{channel}` | Counter | Orders placed, by channel |
| `ecommerce.checkout.duration_p99` | Histogram | Checkout flow latency |
| `ecommerce.payment.capture_total` | Counter | Successful payment captures |

## Evidence

```yaml
evidence:
  - provenance_class: external
    source: "Shopify Admin REST API — Order resource"
    url: "https://shopify.dev/docs/api/admin-rest/2024-04/resources/order"
    citation: "An order is a customer's completed request to purchase one or more products from a shop."
    quoted_at: 2026-05-18
  - provenance_class: external
    source: "Toss Payments — 결제위젯 (Payment Widget)"
    url: "https://docs.tosspayments.com/guides/v2/payment-widget"
    citation: "수많은 상점을 분석해서 만든 최적의 결제 UI"
    quoted_at: 2026-05-18
    fidelity_note: "iter 3 — URL narrowed to /payment-widget root; verbatim short tagline replaces iter-2 paraphrase about 결제 승인 (paraphrase not found on cited URL)."
  - provenance_class: internal_design
    source: "Coupang Wing developer portal (referenced for context; not verbatim-citable)"
    url: "https://developers.coupangcorp.com/hc/ko"
    rationale: "iter 3 — Coupang developer portal returned HTTP 403 to WebFetch; no verbatim Korean snippet could be verified. Downgraded from external to internal_design per Critic iter 2 fix-protocol option B. Reference retained for inspirational/context only; not relied on as evidence anchor."
  - provenance_class: internal_design
    derives_from:
      - "SP15 crud"
      - "SP30 payment"
      - "SP26 search"
      - "SP17 audit-log"
    rationale: "Composition selected from existing L4 baseline; cart/checkout flows derive from payment Spec Trio."
```

## Scaffold Usage

```bash
/ax-scaffold business e-commerce my-shop
```

This will scaffold all 5 enabled L4 domains into `my-shop/` and run
`/ax-verify-domain` for each one.
