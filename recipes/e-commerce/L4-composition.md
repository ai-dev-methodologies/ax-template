# L4 Composition — e-commerce

> Which L4 domains to enable and how they wire together.

## Domain Wiring

```
crud (product)
 └── product catalog CRUD → product search index
      ↓
search
 └── full-text + faceted product search
      ↓
payment
 └── checkout: POST /api/payments with { orderId, amount, idempotencyKey }
      ↓
      payment.captured event
      ↓
crud (order)
 └── order.status = CONFIRMED (atomic with payment capture)
      ↓
notification
 └── receives OrderConfirmed event → sends order confirmation email/push
audit-log
 └── records every order mutation (created, updated, cancelled, refunded)
```

## Domain Configuration Notes

### `crud`
- Two CRUD entities in this recipe: `Product` and `Order`
- Product: enable search indexing on save (`@SearchIndexed`)
- Order: lifecycle states — PENDING → CONFIRMED → SHIPPED → DELIVERED → CANCELLED
- Reference: `templates/L4/crud/`

### `payment`
- Provider: Toss Payments payment widget
- Idempotency key required on POST /api/payments per `rule_ref: practices/rules/api-idempotency-key-required.md`
- Capture is atomic with order confirmation: `TransactionTemplate` wraps both operations
- Refund flow: POST /api/payments/{id}/refund → logged via audit-log
- Reference: `templates/L4/payment/`

### `notification`
- Subscribe to `OrderConfirmedEvent` and `OrderShippedEvent` via `@EventListener`
- Channels: email (via email-outbox), in-app notification
- Templates: `order_confirmed`, `order_shipped`, `payment_failed`, `order_cancelled`
- Reference: `templates/L4/notification/`

### `audit-log`
- Annotate order service methods with `@Audited(action = "order.status.changed")`
- Annotate refund service methods with `@Audited(action = "payment.refunded")`
- Retention: ≥90 days per `spec_ref: specs/audit-log-l0.yaml`
- Reference: `templates/L4/audit-log/`

### `search`
- Full-text search over product title, description, tags
- Faceted filtering: category, price range, availability
- PostgreSQL FTS (default); opt-in to Meilisearch for performance
- Reference: `templates/L4/search/`

## Applied Recipe Annotation

Every L4 domain wired under this recipe **must** declare in its README.md or Spec Trio metadata:
```
applied_recipe: e-commerce
```
(Enforced by rule `business-domain-must-declare-applied-recipe` — SP37)
