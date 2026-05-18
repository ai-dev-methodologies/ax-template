// TDD anchor — SP28 fixture: FAIL case for prefer-feature-gate-over-env-check
// This file is INTENTIONALLY WRONG — it uses process.env for feature flag control.
// The rule scanner must detect this and return a non-zero exit code.
// Created: 2026-05-18 (within applies_to scope)

// ❌ WRONG — build-time constant; cannot toggle without redeployment
const isNewCheckoutEnabled =
  process.env.NEXT_PUBLIC_FEATURE_NEW_CHECKOUT === 'true'

function LegacyCheckout() {
  return <div>Legacy Checkout</div>
}

function NewCheckout() {
  return <div>New Checkout</div>
}

export default function CheckoutPage() {
  if (!isNewCheckoutEnabled) return <LegacyCheckout />
  return <NewCheckout />
}
