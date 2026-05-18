---
pattern: booking
display_name: "Booking (Calendar + Availability + Reservation + Cancellation)"
schema_version: 1
compatible_with_catalog_version: "v1.3.0-business-patterns"
last_verified_at: "2026-05-18"
enabled_l4_domains:
  - audit-log
  - crud
  - feature-flags
  - notification
  - payment
l2_blocks_used:
  - calendar
  - confirm-dialog
  - crud-create-form
  - crud-edit-form
  - crud-list-adapter
  - data-table
  - date-range-picker
  - kpi-card
  - notification-list
  - payment-checkout-form
  - payment-method-picker
  - relative-time
l3_pages_used:
  - create-page
  - dashboard-page
  - detail-page
  - edit-page
  - list-page
override_allowed:
  # Inline override block — no separate RECIPE_DEVIATION.md file.
  #
  # enabled_l4_domains:
  #   skip: ["payment"]
  #   rationale: "Booking is free; no deposit or payment capture needed."
  #   citation: "<internal ticket / PR url>"
  #
  # enabled_l4_domains:
  #   skip: ["feature-flags"]
  #   rationale: "No guest-checkout toggle required; all bookings require auth."
  #   citation: "<internal ticket / PR url>"
---

# Recipe: booking

**Business context:** Calendar-based resource reservation — availability checking,
reservation creation, cancellation windows, and no-show handling for hospitality,
event, and service-booking products.

## Enabled L4 Domains

| L4 Domain | Role in this recipe |
|---|---|
| `crud` | Resource and reservation CRUD operations |
| `payment` | Deposit capture, refund on cancellation within free window |
| `notification` | Booking confirmation, cancellation, no-show alert emails/push |
| `audit-log` | Immutable record of reservation mutations and no-show events |
| `feature-flags` | Guest-checkout toggle, cancellation policy variants |

## Business Invariants

| ID | Statement | Binding |
|---|---|---|
| BOOKING-INV-001 | Reservation must not double-book a resource for overlapping time windows | `rule_ref: practices/rules/idempotency-key-on-mutations.md` + `spec_ref: specs/recipes/booking-recipe-l0.yaml#BOOKING-INV-001` |
| BOOKING-INV-002 | Cancellation within free-window does not charge deposit | `spec_ref: specs/payment-l0.yaml#PAYMENT-STATE-002` |
| BOOKING-INV-003 | No-show triggers audit-log event with operator + timestamp | `spec_ref: specs/audit-log-l0.yaml#AUDIT-RECORD-001` |

## Business Observability (advisory — no emitter test enforced this cycle)

| Signal | Type | Notes |
|---|---|---|
| `recipe.booking.active_total` | Counter | Active reservations count |
| `booking.cancellation.total{within_free_window}` | Counter | Cancellations by free-window eligibility |
| `booking.noshow.total` | Counter | No-show events triggered |

## Evidence

```yaml
evidence:
  - provenance_class: external
    source: "Stripe Connect — Marketplace / platform payments"
    url: "https://docs.stripe.com/connect"
    citation: "Collect payments from customers and automatically pay out a portion to sellers or service providers on your marketplace."
    quoted_at: "2026-05-18"
    fidelity_note: "Cross-recipe external anchor — Stripe Connect payment lifecycle (deposit-hold + release/refund flow) applies directly to booking deposit management."
  - provenance_class: internal_design
    source: "Booking.com Connectivity Provider API"
    url: "https://partners.booking.com/en-us/help/integrations-channel-manager/connectivity-providers"
    rationale: "ECONNREFUSED at fetch time (2026-05-18). No verbatim snippet available. Retained as inspirational context only."
  - provenance_class: internal_design
    source: "야놀자 (Yanolja) — Korean booking platform"
    url: "https://developers.naver.com/docs/login/api/api.md"
    rationale: "Fetch blocked by fetcher (2026-05-18). No verbatim Korean snippet available. Calendar-availability pattern modeled from internal_design."
  - provenance_class: internal_design
    derives_from:
      - "SP30 payment"
      - "SP17 audit-log"
      - "SP15 crud"
      - "SP26 notification"
    rationale: "Booking composition derives from existing L4 payment, audit-log, crud, and notification specs."
```

## Scaffold Usage

```bash
/ax-scaffold business booking my-booking-app
```

This will scaffold all 5 enabled L4 domains into `my-booking-app/` and run
`/ax-verify-domain` for each one.
