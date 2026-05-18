# L4 / billing — Full Trio Domain

Billing domain vertical: subscription lifecycle, plan management, invoice listing,
billing event history.

## Domain Mode

`full_trio` — backend Spec Trio + frontend Spec Trio both present.

## Spec Trio

| File | Purpose |
|---|---|
| `specs/billing-l0.yaml` | Backend compliance spec (8 items: AUTHZ, IDEMP, STATE, CURRENCY, WEBHOOK, BOUNDARY). |
| `specs/billing-frontend-l0.yaml` | Frontend spec (4 items: DISPLAY, STATE, A11Y, CROSS_IMPORT). |
| `contracts/billing-openapi.yaml` | OpenAPI 3.0 contract (subscriptions, invoices, billing events, webhooks, admin plans). |
| `contracts/billing-ui.yaml` | UI contract (routes, operationIds, states). |
| `blueprints/billing-manifest.yaml` | Backend policy manifest (boundary, authz, idempotency, currency, state machine, webhook, providers, observability). |
| `blueprints/billing-ui-manifest.yaml` | Frontend policy manifest (currency display, status colors, a11y, boundary, L2/L3 catalog). |

## Payment vs Billing Boundary (§5.2.6)

| Concern | Owner |
|---|---|
| One-shot authorize/capture/refund | `payment` domain |
| Subscription lifecycle | `billing` domain |
| Invoice issuance | `billing` domain |
| Plan management | `billing` domain |
| Recurring billing event normalization | `billing` domain |

Cross-import is **prohibited** by rule `no-billing-cross-import-from-payment`.

## Backend Templates

```
templates/backend/billing/
├── Plan.java                    # Plan entity (amount: long, minor units)
├── Subscription.java            # Subscription entity (status via SubscriptionStateMachine only)
├── Invoice.java                 # Invoice entity (amountDue/Paid: long, minor units)
├── BillingEvent.java            # Append-only audit event (idempotencyKey: UNIQUE)
├── SubscriptionStateMachine.java # Sole mutator of Subscription.status
├── BillingProvider.java         # Provider abstraction interface
├── StripeBillingAdapter.java    # Stripe implementation (stub, fork to wire real SDK)
├── TossBillingAdapter.java      # Toss Payments implementation (stub, fork to wire real SDK)
├── BillingService.java          # Application service
├── BillingController.java       # REST controller (POST requires @RequireIdempotencyKey)
├── BillingAdminController.java  # Admin controller (/api/admin/billing/**)
├── WebhookBillingReceiver.java  # Webhook endpoint (signature-verified, idempotency via DB)
├── BillingDto.java              # Request/response record types
├── BillingMapper.java           # Entity → DTO mapper
├── SubscriptionRepository.java  # JPA repository
├── PlanRepository.java
├── InvoiceRepository.java
└── BillingEventRepository.java
```

## Frontend Templates

- **L1**: `currency-input.tsx`, `number-input.tsx`, `range-picker.tsx`
- **L2**: `pricing-table.tsx`, `plan-comparison.tsx`, `usage-meter.tsx`, `invoice-list.tsx`, `billing-history.tsx`
- **L3**: `pricing-page/`

## Rules

| Rule | Type | Purpose |
|---|---|---|
| `billing-event-idempotent` | Java | BillingEvent writes must carry idempotencyKey |
| `subscription-state-machine-explicit` | Java | Only SubscriptionStateMachine may call setStatus() |
| `currency-amount-precision-explicit` | Java + React | All monetary amounts as integer minor units |
| `no-billing-cross-import-from-payment` | Java + React | No cross-import between billing and payment |

## Observability

- `billing.invoice.generated_count` — counter, tags: provider, status
- `billing.subscription.lifecycle_transition` — counter/audit, tags: from, to, trigger
- `billing.webhook.received_count` — counter, tags: provider, event_type
- `billing.event.idempotency_hit_count` — counter, replay-detection

## TDD Anchor

- Backend: `backend/src/test/java/ax/template/billing/BillingFlowIT.java`
- Frontend: `templates/_tests/billing-prereq.spec.ts`
