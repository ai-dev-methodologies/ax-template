/*
---
template_id: L2/blocks/feature-flag-toggle
layer: L2
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "React Docs — Optimistic UI with useState rollback on error"
    url: "https://react.dev/learn/you-might-not-need-an-effect"
  - source_type: external
    citation: "WAI-ARIA Authoring Practices — Switch role for toggle buttons"
    url: "https://www.w3.org/WAI/ARIA/apg/patterns/switch/"
dependencies: []
imports_from: [L1]
imports_forbidden: [L4, app/, lib/auth/]
spec_ref: "specs/feature-flags-frontend-l0.yaml#FF-FE-002"
---
*/
'use client'

import * as React from 'react'

export interface FeatureFlagToggleProps {
  /**
   * Flag name (used in PATCH URL).
   */
  name: string
  /**
   * Initial enabled state from server data.
   */
  initialEnabled: boolean
  /**
   * Called after a successful PATCH with the updated enabled value.
   */
  onToggled?: (name: string, enabled: boolean) => void
  /**
   * Base URL for the admin feature-flags API.
   * Defaults to '/api/v1' (same-origin).
   */
  apiBase?: string
  /**
   * Accessible label for the toggle (e.g., "Enable new-checkout").
   * Defaults to "Toggle {name}".
   */
  label?: string
}

/**
 * FeatureFlagToggle — L2 block.
 *
 * Admin toggle for a feature flag. Calls PATCH /api/v1/admin/feature-flags/{name}
 * on click with optimistic update; rolls back to previous state on failure.
 *
 * Usage:
 * ```tsx
 * <FeatureFlagToggle
 *   name="new-checkout"
 *   initialEnabled={flag.enabled}
 *   onToggled={(name, enabled) => console.log(name, enabled)}
 * />
 * ```
 *
 * spec_ref: specs/feature-flags-frontend-l0.yaml#FF-FE-002
 * blueprint_ref: blueprints/feature-flags-ui-manifest.yaml#toggle
 */
export function FeatureFlagToggle({
  name,
  initialEnabled,
  onToggled,
  apiBase = '/api/v1',
  label,
}: FeatureFlagToggleProps) {
  const [enabled, setEnabled] = React.useState(initialEnabled)
  const [loading, setLoading] = React.useState(false)
  const [error, setError] = React.useState<string | null>(null)

  const ariaLabel = label ?? `Toggle ${name}`

  async function handleClick() {
    const next = !enabled
    setEnabled(next) // optimistic update
    setLoading(true)
    setError(null)

    try {
      const res = await fetch(
        `${apiBase}/admin/feature-flags/${encodeURIComponent(name)}`,
        {
          method: 'PATCH',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ enabled: next }),
        },
      )

      if (!res.ok) {
        setEnabled(!next) // rollback
        setError(`Update failed (${res.status})`)
      } else {
        onToggled?.(name, next)
      }
    } catch {
      setEnabled(!next) // rollback on network error
      setError('Network error — update rolled back')
    } finally {
      setLoading(false)
    }
  }

  return (
    <span style={{ display: 'inline-flex', flexDirection: 'column', gap: 4 }}>
      <button
        type="button"
        role="switch"
        aria-checked={enabled}
        aria-label={ariaLabel}
        disabled={loading}
        onClick={handleClick}
        style={{
          cursor: loading ? 'not-allowed' : 'pointer',
          opacity: loading ? 0.6 : 1,
        }}
      >
        {enabled ? 'ON' : 'OFF'}
      </button>
      {error && (
        <span role="alert" style={{ color: 'red', fontSize: '0.75rem' }}>
          {error}
        </span>
      )}
    </span>
  )
}
