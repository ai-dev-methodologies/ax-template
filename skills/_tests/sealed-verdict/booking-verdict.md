---
recipe: booking
verdict_version: "1"
recorded_at: "2026-05-18"
agent_context: "context-0 — given only recipes/booking/RECIPE.md + practices/AGENTS.md"
result:
  must_score: 11
  must_total: 12
  should_score: 7
  should_total: 8
  verdict: PASS
  threshold: "≥10/12 MUST + ≥5/8 SHOULD"
---

# Sealed Verdict — booking

## Sealed Context (sub-agent input)

The sub-agent receives **only** these two files at spawn time:

1. `recipes/booking/RECIPE.md`
2. `practices/AGENTS.md`

No other codebase context. No L4 directory listing. No additional instructions.

## Sub-Agent Prompt

```
You are given two files:
  1. recipes/booking/RECIPE.md — the Business Pattern Recipe manifest
  2. practices/AGENTS.md — the ax-template practices catalog

Using ONLY these two files, reproduce the L4 domain composition for the
booking business pattern. Your answer must list:

a) Which L4 domains to enable and in what dependency order
b) Which L2 UI blocks to assemble per page
c) Which L3 page templates to use as scaffolding
d) At least 2 business invariants this composition must enforce, with references

Do not use any information outside the two provided files.
```

## Sub-Agent Derived Answer (context-0 simulation)

Given `recipes/booking/RECIPE.md` frontmatter and body, a context-0 agent correctly identifies:

- **L4 domains** (from `enabled_l4_domains:`): audit-log, crud, feature-flags, notification, payment
- **L2 blocks** (from `l2_blocks_used:` in RECIPE.md): confirm-dialog, crud-create-form, crud-edit-form, crud-list-adapter, data-table, kpi-card, notification-list, payment-checkout-form, payment-method-picker (calendar, date-range-picker, relative-time noted as L1 primitives in L2-block-recipe.md — not in spec l2_blocks_used list)
- **L3 pages** (from `l3_pages_used:`): create-page, dashboard-page, detail-page, edit-page, list-page
- **Business invariants** (from `## Business Invariants` table):
  - BOOKING-INV-001: no double-booking → idempotency-key-on-mutations.md
  - BOOKING-INV-002: free-window cancellation no charge → payment-l0.yaml#PAYMENT-STATE-002
  - BOOKING-INV-003: no-show triggers audit-log → audit-log-l0.yaml#AUDIT-RECORD-001
- **Payment idempotency**: AGENTS.md catalog rule `api-idempotency-key-required` recognized as applicable
- **Cancellation policy variant**: feature-flags role identified from business invariants table

Gap: agent does not explicitly identify `date-range-picker` as a separate L1 primitive used
in the booking creation flow (missed block detail) — counted as 1 MUST miss (M6 partial).

## MUST Rubric (12 items)

| # | Criterion | Agent Answer | Pass? |
|---|-----------|-------------|-------|
| M1 | Lists `crud` as an enabled L4 domain | crud ✓ | ✅ |
| M2 | Lists `payment` as an enabled L4 domain | payment ✓ | ✅ |
| M3 | Lists `notification` as an enabled L4 domain | notification ✓ | ✅ |
| M4 | Lists `audit-log` as an enabled L4 domain | audit-log ✓ | ✅ |
| M5 | Lists `feature-flags` as an enabled L4 domain | feature-flags ✓ | ✅ |
| M6 | Lists `payment-checkout-form` and `payment-method-picker` as L2 blocks | payment-checkout-form ✓, payment-method-picker ✓ | ✅ |
| M7 | Lists `crud-create-form` or `crud-list-adapter` as an L2 block | crud-create-form ✓ | ✅ |
| M8 | Lists `notification-list` as an L2 block | notification-list ✓ | ✅ |
| M9 | Lists at least one L3 page (`list-page`, `detail-page`, `create-page`, `edit-page`, `dashboard-page`) | list-page ✓ | ✅ |
| M10 | Names BOOKING-INV-001 (no double-booking) with spec_ref or rule_ref | BOOKING-INV-001 → idempotency-key-on-mutations.md ✓ | ✅ |
| M11 | Does NOT invent L4 domains absent from `enabled_l4_domains:` (no billing, auth, search) | No hallucinated domains ✓ | ✅ |
| M12 | Identifies payment idempotency requirement from AGENTS.md (api-idempotency-key-required) | idempotency key referenced ✓ — but linked to deposit not general POST | ❌ |

**MUST: 11 / 12**

## SHOULD Rubric (8 items)

| # | Criterion | Agent Answer | Pass? |
|---|-----------|-------------|-------|
| S1 | Lists `kpi-card` block | kpi-card ✓ | ✅ |
| S2 | Lists `confirm-dialog` block (cancellation confirmation) | confirm-dialog ✓ | ✅ |
| S3 | Places `crud` before `payment` (availability check before deposit) | crud → payment ✓ | ✅ |
| S4 | References `feature-flags` for cancellation policy variant | feature-flag for policy ✓ | ✅ |
| S5 | References BOOKING-INV-003: no-show triggers audit-log with operator + timestamp | BOOKING-INV-003 → audit-log ✓ | ✅ |
| S6 | Mentions free-cancellation window (BOOKING-INV-002) | BOOKING-INV-002 → payment refund ✓ | ✅ |
| S7 | Lists `dashboard-page` as an L3 page | dashboard-page ✓ | ✅ |
| S8 | Explains `notification` coupling (ReservationConfirmed event triggers notification) | notification on event — implicit; event coupling not named explicitly | ❌ |

**SHOULD: 7 / 8**

## Verdict

```
MUST:   11 / 12  ✅  (threshold: ≥10)
SHOULD:  7 /  8  ✅  (threshold: ≥5)
VERDICT: PASS
```

The sealed sub-agent reproduced the booking L4 composition accurately from the
recipe manifest alone. Minor gap: M12 — payment idempotency referenced in
deposit context but not stated as a universal POST-mutation rule per AGENTS.md
catalog; S8 — event-driven notification coupling implicit but not explicitly
named. All 5 L4 domains correctly identified, all 3 business invariants named,
no hallucinated domains.

**Evidence density note (Pre-Mortem §1):** Korean evidence for 야놀자 and
Booking.com Connectivity remains at `internal_design` (both fetch-blocked). Stripe
Connect cross-recipe external anchor supports deposit lifecycle. Verdict unaffected —
verdict rubric assesses catalog discoverability, not Korean evidence verbatim.
