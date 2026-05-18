# L4 Composition — marketplace

> Which L4 domains to enable and how they wire together.

## Domain Wiring

```
crud (listing)
 └── listing CRUD → search index (on create/update/delete)
      ↓
search
 └── full-text + faceted listing search
      ↓
crud (listing) — authz filter
 └── soft-deleted + moderator-hidden listings excluded from search results
      ↓
crud (order / bid)
 └── buyer places order / bid → POST /api/orders with { listingId, amount, idempotencyKey }
      ↓
payment
 └── escrow capture: POST /api/payments with { orderId, amount, idempotencyKey }
      ↓ payment.captured → escrow.held
notification
 └── seller receives BidReceivedEvent / OrderPlacedEvent
      ↓
      (await buyer confirmation OR dispute-window expiry)
      ↓
payment
 └── escrow release: POST /api/payments/{id}/release → seller payout
 └── or dispute refund: POST /api/payments/{id}/refund → buyer refund
      ↓
audit-log
 └── records every escrow state change, moderation action, dispute open/close
      ↓
crud (rating)
 └── buyer submits seller rating after escrow release
```

## Domain Configuration Notes

### `crud`
- Three CRUD entities: `Listing`, `Order`, `Rating`
- Listing: soft-delete support; moderator-hide flag; search-indexed on mutation
- Order: lifecycle states — PENDING → ESCROW_HELD → RELEASED | REFUNDED | DISPUTED
- Rating: immutable once submitted; linked to released order

### `payment`
- Provider: Stripe Connect (platform account + connected seller accounts)
- Escrow pattern: capture funds to platform → release to seller after buyer confirmation
- Dispute window: configurable (default 3 days); auto-release on expiry
- Idempotency key required on all payment mutations
- Reference: `templates/L4/payment/`

### `search`
- Full-text search over listing title, description, tags, seller info
- Faceted filtering: category, price range, location, condition
- `SEARCH-AUTHZ-001`: authz filter applied at query time — soft-deleted + hidden listings never returned
- PostgreSQL FTS (default); opt-in to Meilisearch for volume
- Reference: `templates/L4/search/`

### `notification`
- Events: `BidReceivedEvent`, `OrderPlacedEvent`, `EscrowReleasedEvent`, `DisputeOpenedEvent`
- Channels: email, in-app notification bell
- Templates: `bid_received`, `order_confirmed`, `escrow_released`, `dispute_opened`, `dispute_resolved`
- Reference: `templates/L4/notification/`

### `audit-log`
- Annotate payment service with `@Audited(action = "escrow.state.changed")`
- Annotate moderation with `@Audited(action = "listing.moderation.action")`
- Annotate dispute with `@Audited(action = "dispute.opened")` / `@Audited(action = "dispute.resolved")`
- Reference: `templates/L4/audit-log/`

### `feature-flags` (optional — recommended for KYC gating)
- `marketplace.kyc_required_listing_value_threshold` — KRW threshold above which KYC is required
- Bind to `specs/identity-verification-l0.yaml#IDV-PROVIDER-001` downstream
- Reference: `templates/L4/feature-flags/`

## Applied Recipe Annotation

Every L4 domain wired under this recipe **must** declare in its README.md:
```
applied_recipes:
  - marketplace
```
(Enforced by rule `business-domain-must-declare-applied-recipe` — SP37/R6 dual-form guard)
