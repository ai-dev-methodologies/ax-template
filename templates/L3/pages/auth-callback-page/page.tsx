/*
---
template_id: L3/pages/auth-callback-page
layer: L3
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "Next.js 15 App Router file conventions — page.tsx"
    url: "https://nextjs.org/docs/app/building-your-application/routing/pages"
  - source_type: external
    citation: "RFC 6749 §4.1.2 — Authorization Code Response and error handling"
    url: "https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.2"
  - source_type: internal
    rationale: "Generic OAuth / email-verify callback skeleton. Reads a status prop and renders success, loading, or failure states — no token exchange logic."
dependencies: []
---
*/
import * as React from 'react'

/**
 * AuthCallbackPage — generic OAuth / email-verify callback skeleton.
 *
 * The L4 caller resolves the callback (exchanges code for token, verifies email,
 * etc.) and passes the resulting status. This template only renders the UI.
 *
 * Slot props:
 *   - provider      (required) identity provider label ("Google", "Kakao", etc.)
 *   - status        (required) current callback state
 *   - successHref   (required) redirect href shown on success
 *   - failureHref   (required) redirect href shown on failure
 *   - statusSlot    (optional) custom status content (overrides built-in states)
 *   - successLabel  (optional) success CTA label (default: "Continue")
 *   - failureLabel  (optional) failure CTA label (default: "Try again")
 *
 * L4 usage:
 *   import AuthCallbackPage from 'templates/L3/pages/auth-callback-page/page'
 *   export default async function GoogleCallbackPage({ searchParams }) {
 *     const status = await exchangeGoogleCode(searchParams.code)
 *     return (
 *       <AuthCallbackPage
 *         provider="Google"
 *         status={status}
 *         successHref="/dashboard"
 *         failureHref="/login"
 *       />
 *     )
 *   }
 */
export type CallbackStatus = 'loading' | 'success' | 'failure'

export interface AuthCallbackPageProps {
  /** Identity provider label shown to the user */
  provider: string
  /** Current callback resolution state */
  status: CallbackStatus
  /** Href for the "Continue" link on success */
  successHref: string
  /** Href for the "Try again" link on failure */
  failureHref: string
  /** Overrides built-in status content */
  statusSlot?: React.ReactNode
  /** Success CTA label (default: "Continue") */
  successLabel?: string
  /** Failure CTA label (default: "Try again") */
  failureLabel?: string
}

export default function AuthCallbackPage({
  provider,
  status,
  successHref,
  failureHref,
  statusSlot,
  successLabel = 'Continue',
  failureLabel = 'Try again',
}: AuthCallbackPageProps) {
  return (
    <main className="flex min-h-svh items-center justify-center px-4">
      <div className="w-full max-w-sm text-center space-y-6">
        {statusSlot ?? (
          <>
            {status === 'loading' && (
              <div className="space-y-3" aria-live="polite" aria-busy="true">
                <div
                  className="mx-auto h-10 w-10 animate-spin rounded-full border-4 border-primary border-t-transparent"
                  role="status"
                  aria-label="Processing"
                />
                <p className="text-sm text-muted-foreground">
                  Completing {provider} sign-in…
                </p>
              </div>
            )}

            {status === 'success' && (
              <div className="space-y-4" aria-live="polite">
                <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-green-100 text-green-700 text-xl">
                  ✓
                </div>
                <div className="space-y-1">
                  <h1 className="text-lg font-semibold">Signed in successfully</h1>
                  <p className="text-sm text-muted-foreground">
                    {provider} authentication complete.
                  </p>
                </div>
                <a
                  href={successHref}
                  className="inline-flex items-center justify-center rounded-md bg-primary px-6 py-2 text-sm font-medium text-primary-foreground hover:opacity-90 transition-opacity"
                >
                  {successLabel}
                </a>
              </div>
            )}

            {status === 'failure' && (
              <div className="space-y-4" aria-live="assertive" role="alert">
                <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-red-100 text-red-700 text-xl">
                  ✕
                </div>
                <div className="space-y-1">
                  <h1 className="text-lg font-semibold">Authentication failed</h1>
                  <p className="text-sm text-muted-foreground">
                    {provider} sign-in could not be completed. Please try again.
                  </p>
                </div>
                <a
                  href={failureHref}
                  className="inline-flex items-center justify-center rounded-md border px-6 py-2 text-sm font-medium hover:bg-muted transition-colors"
                >
                  {failureLabel}
                </a>
              </div>
            )}
          </>
        )}
      </div>
    </main>
  )
}
