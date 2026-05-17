/*
---
template_id: L2/blocks/oauth-callback-panel
layer: L2
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "RFC 6749 §4.1.2 — Authorization Response redirect handling"
    url: "https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.2"
  - source_type: internal
    rationale: "L2 auth block — renders OAuth callback status; domain callout handled by L4 via onRetry prop."
dependencies: []
imports_from: [L1]
imports_forbidden: [L4, app/, lib/auth/]
---
*/
import * as React from 'react'

export type OAuthCallbackStatus = 'loading' | 'success' | 'error'

export interface OAuthCallbackPanelProps {
  status: OAuthCallbackStatus
  errorMessage?: string
  /** Called when user clicks "Try again" in error state */
  onRetry?: () => void
  /** Optional redirect message for success state */
  redirectMessage?: string
}

export default function OAuthCallbackPanel({
  status,
  errorMessage,
  onRetry,
  redirectMessage = 'Redirecting…',
}: OAuthCallbackPanelProps) {
  return (
    <div
      role="status"
      aria-live="polite"
      className="flex flex-col items-center justify-center gap-4 py-12 text-center"
    >
      {status === 'loading' && (
        <>
          <span
            aria-hidden="true"
            className="h-8 w-8 animate-spin rounded-full border-4 border-muted border-t-primary"
          />
          <p className="text-sm text-muted-foreground">Completing sign-in…</p>
        </>
      )}

      {status === 'success' && (
        <p className="text-sm text-muted-foreground">{redirectMessage}</p>
      )}

      {status === 'error' && (
        <>
          <p className="text-sm font-medium text-destructive">
            {errorMessage ?? 'Sign-in failed. Please try again.'}
          </p>
          {onRetry && (
            <button
              type="button"
              onClick={onRetry}
              className="inline-flex items-center rounded-md border border-input bg-background px-4 py-2 text-sm font-medium shadow-sm hover:bg-accent hover:text-accent-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
            >
              Try again
            </button>
          )}
        </>
      )}
    </div>
  )
}
