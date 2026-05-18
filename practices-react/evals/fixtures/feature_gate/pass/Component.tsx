// TDD anchor — SP28 fixture: PASS case for prefer-feature-gate-over-env-check
// This file uses FeatureGate for feature flag control — no env-based flag checks.
// The rule scanner must detect NO violations and return exit 0.
// Created: 2026-05-18 (within applies_to scope)

// Stub FeatureGate so the fixture is self-contained (real app imports from L2 block)
function FeatureGate({
  name,
  children,
  fallback,
}: {
  name: string
  children: React.ReactNode
  fallback?: React.ReactNode
}) {
  // stub — real implementation fetches /api/v1/feature-flags/{name}/active
  return <>{children}</>
}

function LegacyCheckout() {
  return <div>Legacy Checkout</div>
}

function NewCheckout() {
  return <div>New Checkout</div>
}

// ✅ CORRECT — runtime-controlled via admin API; no rebuild needed
export default function CheckoutPage() {
  return (
    <FeatureGate name="new-checkout" fallback={<LegacyCheckout />}>
      <NewCheckout />
    </FeatureGate>
  )
}

import React from 'react'
