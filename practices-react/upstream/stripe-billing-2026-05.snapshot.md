---
snapshot_id: stripe-billing-2026-05
source: "https://docs.stripe.com/billing/subscriptions/overview"
fetched_at: "2026-07-14T00:00:00Z"
version_observed: "as published, fetched 2026-07-14"
via: curl
tier: 2
bytes: 1657
sha: "2e7d4ece6d2556a38db90177f281939f173bda8ba73fab25e74cf7ec1af7c88a"
---

# stripe billing 2026 05 — upstream snapshot

Source: https://docs.stripe.com/billing/subscriptions/overview
Fetched: 2026-07-14

## Subscription lifecycle (status table)
Source: https://docs.stripe.com/billing/subscriptions/overview

Status Description trialing The subscription is currently in a trial period and you can safely provision your product for your customer. The subscription transitions automatically to active when a customer makes the first payment. active The subscription is in good standing. For past_due subscriptions, paying the latest associated invoice or marking it uncollectible transitions the subscription to active. Note that active doesn't indicate that all outstanding invoices associated with the subscription have been paid. You can leave other outstanding invoices open for payment, mark them as uncollectible, or void them as you see fit.

## Idempotency
Source: https://docs.stripe.com/api/idempotent_requests

The API supports idempotency for safely retrying requests without accidentally performing the same operation twice. Stripe's idempotency works by saving the resulting status code and body of the first request made for any given idempotency key, regardless of whether it succeeds or fails. Subsequent requests with the same key return the same result, including 500 errors. You can remove keys from the system automatically after they're at least 24 hours old. We generate a new request if a key is reused after the original is pruned.

## Zero-decimal currencies (amounts and currencies)
Source: https://docs.stripe.com/currencies

### Zero-decimal currencies
For the following zero-decimal currencies, the charge and the amount are the same, without requiring multiplication. For example, to charge 500 JPY, provide an amount value of 500.
