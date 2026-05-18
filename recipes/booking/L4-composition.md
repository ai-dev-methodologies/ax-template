# L4 Composition — booking

> Which L4 domains to enable and how they wire together.

## Domain Wiring

```
crud (resource)
 └── resource availability slot CRUD → availability query
      ↓
feature-flags
 └── guest-checkout toggle, cancellation-policy variant flag
      ↓
crud (reservation)
 └── POST /api/reservations (idempotency key required)
      ↓ availability lock held during transaction
payment
 └── deposit capture: POST /api/payments with { reservationId, amount, idempotencyKey }
      ↓ payment.captured event
crud (reservation)
 └── reservation.status = CONFIRMED (atomic with deposit capture)
      ↓
notification
 └── receives ReservationConfirmed event → sends confirmation email/push
audit-log
 └── records every reservation mutation (created, confirmed, cancelled, no-show)
      ↓ no-show path
audit-log
 └── NoShowTriggered event with { operatorId, reservationId, triggeredAt }
```

## Domain Configuration Notes

### `crud`
- Two CRUD entities: `Resource` (rooms, slots, seats) and `Reservation`
- Resource: expose availability window query endpoint (`GET /api/resources/{id}/availability?from=&to=`)
- Reservation: lifecycle states — PENDING → CONFIRMED → CANCELLED | NO_SHOW
- Overlap guard: database-level range exclusion constraint prevents double-booking
- Reference: `templates/L4/crud/`

### `payment`
- Deposit capture at reservation creation
- Free-cancellation window (configurable via feature-flag): no charge if cancelled before deadline
- Idempotency key required on POST /api/payments per `rule_ref: practices/rules/api-idempotency-key-required.md`
- Refund flow: POST /api/payments/{id}/refund → logged via audit-log
- Reference: `templates/L4/payment/`

### `notification`
- Subscribe to `ReservationConfirmedEvent`, `ReservationCancelledEvent`, `NoShowTriggeredEvent`
- Channels: email (via email-outbox), in-app notification push
- Templates: `booking_confirmed`, `booking_cancelled_free`, `booking_cancelled_charged`, `noshow_alert`
- Reference: `templates/L4/notification/`

### `audit-log`
- Annotate reservation service with `@Audited(action = "reservation.status.changed")`
- Annotate no-show trigger with `@Audited(action = "reservation.noshow.triggered")`
- Retention: ≥90 days per `spec_ref: specs/audit-log-l0.yaml`
- Reference: `templates/L4/audit-log/`

### `feature-flags`
- `booking.guest_checkout.enabled` — allow booking without auth; collect email for notification
- `booking.cancellation_policy.variant` — `strict` (no free window) | `flexible` (24h free) | `moderate` (48h free)
- Reference: `templates/L4/feature-flags/`

## Applied Recipe Annotation

Every L4 domain wired under this recipe **must** declare in its README.md:
```
applied_recipes:
  - booking
```
(Enforced by rule `business-domain-must-declare-applied-recipe` — SP37/R6 dual-form guard)
