# Stripe Billing — Subscriptions + Webhook Events + Price Model

**Source URL(s):** https://docs.stripe.com/billing/subscriptions/overview; https://docs.stripe.com/api/idempotent_requests; https://docs.stripe.com/currencies (original 2026-07-14 fetch, preserved below); https://docs.stripe.com/billing/subscriptions/webhooks; https://docs.stripe.com/api/events/types; https://docs.stripe.com/api/prices/object (2026-07-30 refresh)
**HTTP status:** 200 (all URLs)
**Fetched at:** 2026-07-30T00:51:30Z
**Extractor invocation:** `practices/scripts/snapshot-extract.sh <url>` (once per URL listed above)
**Body SHA-256 (below the `---` divider, header excluded):** d42d9ebc4fc383c6dff3578cfa0057efb42b7a7a87f163c36a71bdc99509f20e

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

## Webhook events

Source: https://docs.stripe.com/billing/subscriptions/webhooks ; https://docs.stripe.com/api/events/types (curl+snapshot-extract.sh, 2026-07-30)

`customer.subscription.updated` — Sent when a subscription starts or changes. For example, renewing a subscription,
adding a coupon, applying a discount, or changing a plan all trigger this event.

`invoice.payment_succeeded` — Occurs whenever an invoice payment attempt succeeds.

## Plan / Price model

Source: https://docs.stripe.com/api/prices/object (curl+snapshot-extract.sh, 2026-07-30)

A Price object's `recurring` field description, verbatim: "The recurring components of a price such as interval and usage_type." (the Price API
reference server-renders this field description directly; other Price attributes such as
`unit_amount` and `currency` are documented on the same reference page.)
