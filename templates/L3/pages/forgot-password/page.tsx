/*
---
template_id: L3/pages/forgot-password
layer: L3
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "Next.js 16 App Router file conventions — page.tsx"
    url: "https://nextjs.org/docs/app/building-your-application/routing/pages"
  - source_type: external
    citation: "OWASP Authentication Cheat Sheet — Forgot Password feature"
    url: "https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html"
  - source_type: internal_design
    rationale: "Generic forgot-password skeleton. Renders email input → submit → confirmation state. Uses email-outbox backend (SP19). No domain coupling — L4 wires the API call via onSubmit prop."
imports_from: [L1, L2]
imports_forbidden: [L4]
---
*/
'use client'

import * as React from 'react'

/**
 * ForgotPasswordPage — generic forgot-password skeleton.
 *
 * Slot props:
 *   - onSubmit    (optional) called with email when form is submitted
 *   - loginHref   (required) back-to-login link href
 *   - successSlot (optional) custom content shown after submit (overrides built-in confirmation)
 *   - description (optional) subtitle below heading
 *
 * Internal state:
 *   - email       controlled input value
 *   - isSubmitted toggle to success state after onSubmit resolves
 *   - isPending   disables submit while inflight
 *
 * L4 usage:
 *   import ForgotPasswordPage from 'templates/L3/pages/forgot-password/page'
 *   export default function ForgotPasswordRoute() {
 *     async function requestReset(email: string) {
 *       await api.post('/auth/forgot-password', { email })
 *     }
 *     return <ForgotPasswordPage onSubmit={requestReset} loginHref="/login" />
 *   }
 */
export interface ForgotPasswordPageProps {
  /** Called with the submitted email address */
  onSubmit?: (email: string) => void | Promise<void>
  /** Href for the back-to-login link */
  loginHref: string
  /** Overrides built-in success confirmation */
  successSlot?: React.ReactNode
  /** Optional subtitle below "Forgot password?" heading */
  description?: string
}

export default function ForgotPasswordPage({
  onSubmit,
  loginHref,
  successSlot,
  description = 'Enter your email and we\'ll send you a reset link.',
}: ForgotPasswordPageProps) {
  const [email, setEmail] = React.useState('')
  const [isPending, setIsPending] = React.useState(false)
  const [isSubmitted, setIsSubmitted] = React.useState(false)

  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault()
    setIsPending(true)
    try {
      await onSubmit?.(email)
      setIsSubmitted(true)
    } finally {
      setIsPending(false)
    }
  }

  if (isSubmitted) {
    return (
      <main className="flex min-h-svh items-center justify-center px-4">
        <div className="w-full max-w-sm text-center space-y-6">
          {successSlot ?? (
            <div className="space-y-4" aria-live="polite">
              <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-green-100 text-green-700 text-xl">
                ✉
              </div>
              <div className="space-y-1">
                <h1 className="text-lg font-semibold">Check your email</h1>
                <p className="text-sm text-muted-foreground">
                  We sent a reset link to <strong>{email}</strong>.
                </p>
              </div>
              <a
                href={loginHref}
                className="text-sm text-muted-foreground hover:text-foreground underline underline-offset-4 transition-colors"
              >
                Back to sign in
              </a>
            </div>
          )}
        </div>
      </main>
    )
  }

  return (
    <main className="flex min-h-svh items-center justify-center px-4">
      <div className="w-full max-w-sm space-y-6">
        <div className="space-y-1 text-center">
          <h1 className="text-2xl font-semibold tracking-tight">Forgot password?</h1>
          {description && (
            <p className="text-sm text-muted-foreground">{description}</p>
          )}
        </div>

        <form onSubmit={handleSubmit} className="space-y-4" noValidate>
          <div className="space-y-1">
            <label
              htmlFor="fp-email"
              className="block text-sm font-medium leading-none"
            >
              Email
            </label>
            <input
              id="fp-email"
              type="email"
              name="email"
              required
              autoComplete="email"
              autoFocus
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="you@example.com"
              className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
            />
          </div>

          <button
            type="submit"
            disabled={isPending || !email.trim()}
            className="inline-flex w-full items-center justify-center rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:opacity-90 transition-opacity disabled:pointer-events-none disabled:opacity-50"
          >
            {isPending ? 'Sending…' : 'Send reset link'}
          </button>
        </form>

        <p className="text-center text-sm text-muted-foreground">
          Remember your password?{' '}
          <a
            href={loginHref}
            className="underline underline-offset-4 hover:text-foreground transition-colors"
          >
            Sign in
          </a>
        </p>
      </div>
    </main>
  )
}
