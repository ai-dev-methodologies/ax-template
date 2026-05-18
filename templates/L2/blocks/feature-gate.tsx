/*
---
template_id: L2/blocks/feature-gate
layer: L2
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "React Docs — Conditional rendering and lazy data fetching with useEffect"
    url: "https://react.dev/learn/you-might-not-need-an-effect"
  - source_type: external
    citation: "MDN — fetch API and AbortController for cleanup"
    url: "https://developer.mozilla.org/en-US/docs/Web/API/AbortController"
dependencies: []
imports_from: [L1]
imports_forbidden: [L4, app/, lib/auth/]
spec_ref: "specs/feature-flags-frontend-l0.yaml#FF-FE-004"
---
*/
'use client'

import * as React from 'react'

export interface FeatureGateProps {
  /**
   * Feature flag name to evaluate.
   * Must match the backend pattern: ^[a-z][a-z0-9-]{1,62}$
   */
  name: string
  /**
   * Rendered when the flag is active.
   */
  children: React.ReactNode
  /**
   * Rendered when the flag is inactive or on error (fail-closed).
   * Defaults to null (renders nothing).
   */
  fallback?: React.ReactNode
  /**
   * Base URL for the feature-flags API.
   * Defaults to '/api/v1' (same-origin).
   */
  apiBase?: string
}

/**
 * FeatureGate — L2 block.
 *
 * Client-side feature flag gate: fetches /api/v1/feature-flags/{name}/active
 * and renders children when active=true, fallback otherwise.
 *
 * Fail-closed: loading and error states both render fallback (or null).
 *
 * Usage:
 * ```tsx
 * <FeatureGate name="new-checkout" fallback={<LegacyCheckout />}>
 *   <NewCheckout />
 * </FeatureGate>
 * ```
 *
 * spec_ref: specs/feature-flags-frontend-l0.yaml#FF-FE-004
 * blueprint_ref: blueprints/feature-flags-ui-manifest.yaml#feature-gate
 */
export function FeatureGate({
  name,
  children,
  fallback = null,
  apiBase = '/api/v1',
}: FeatureGateProps) {
  const [active, setActive] = React.useState<boolean | null>(null)

  React.useEffect(() => {
    const controller = new AbortController()

    fetch(`${apiBase}/feature-flags/${encodeURIComponent(name)}/active`, {
      signal: controller.signal,
    })
      .then((res) => {
        if (!res.ok) {
          setActive(false) // fail-closed on HTTP error
          return
        }
        return res.json() as Promise<{ active: boolean }>
      })
      .then((data) => {
        if (data !== undefined) setActive(data.active)
      })
      .catch(() => {
        setActive(false) // fail-closed on network error
      })

    return () => controller.abort()
  }, [name, apiBase])

  // Loading → fail-closed (render fallback)
  if (active === null) return <>{fallback}</>
  if (!active) return <>{fallback}</>
  return <>{children}</>
}
