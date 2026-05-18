/*
---
template_id: L3/pages/reset-password
layer: L3
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "Next.js 16 App Router file conventions — dynamic segments"
    url: "https://nextjs.org/docs/app/building-your-application/routing/dynamic-routes"
  - source_type: external
    citation: "OWASP Authentication Cheat Sheet — Password Reset feature"
    url: "https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html"
  - source_type: internal_design
    rationale: "Generic reset-password token skeleton. Receives token as a prop (L4 extracts from route params). Renders new-password + confirm-password inputs and validates match client-side before calling onSubmit."
imports_from: [L1, L2]
imports_forbidden: [L4]
---
*/
'use client'

import * as React from 'react'

/**
 * ResetPasswordPage — generic reset-password skeleton.
 *
 * Slot props:
 *   - token       (required) reset token (L4 extracts from URL params)
 *   - onSubmit    (optional) called with the new password when form is submitted
 *   - loginHref   (required) back-to-login link href
 *   - successSlot (optional) custom content shown after submit
 *
 * Internal state:
 *   - password, confirmPassword: controlled inputs
 *   - matchError: shown when passwords don't match on submit
 *   - isSubmitted / isPending
 *
 * L4 usage:
 *   import ResetPasswordPage from 'templates/L3/pages/reset-password/[token]/page'
 *   export default async function ResetPasswordRoute({ params }) {
 *     async function doReset(password: string) {
 *       await api.post('/auth/reset-password', { token: params.token, password })
 *     }
 *     return <ResetPasswordPage token={params.token} onSubmit={doReset} loginHref="/login" />
 *   }
 */
export interface ResetPasswordPageProps {
  /** Reset token from URL params */
  token: string
  /** Called with the new password on successful form submission */
  onSubmit?: (password: string) => void | Promise<void>
  /** Href for the back-to-login link */
  loginHref: string
  /** Overrides built-in success state */
  successSlot?: React.ReactNode
}

export default function ResetPasswordPage({
  token: _token,
  onSubmit,
  loginHref,
  successSlot,
}: ResetPasswordPageProps) {
  const [password, setPassword] = React.useState('')
  const [confirmPassword, setConfirmPassword] = React.useState('')
  const [matchError, setMatchError] = React.useState<string | null>(null)
  const [isPending, setIsPending] = React.useState(false)
  const [isSubmitted, setIsSubmitted] = React.useState(false)

  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault()
    if (password !== confirmPassword) {
      setMatchError('Passwords do not match.')
      return
    }
    setMatchError(null)
    setIsPending(true)
    try {
      await onSubmit?.(password)
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
                ✓
              </div>
              <div className="space-y-1">
                <h1 className="text-lg font-semibold">Password updated</h1>
                <p className="text-sm text-muted-foreground">
                  Your password has been reset successfully.
                </p>
              </div>
              <a
                href={loginHref}
                className="inline-flex items-center justify-center rounded-md bg-primary px-6 py-2 text-sm font-medium text-primary-foreground hover:opacity-90 transition-opacity"
              >
                Sign in
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
          <h1 className="text-2xl font-semibold tracking-tight">Reset password</h1>
          <p className="text-sm text-muted-foreground">
            Enter your new password below.
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4" noValidate>
          <div className="space-y-1">
            <label
              htmlFor="rp-password"
              className="block text-sm font-medium leading-none"
            >
              New password
            </label>
            <input
              id="rp-password"
              type="password"
              name="password"
              required
              autoComplete="new-password"
              autoFocus
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
            />
          </div>

          <div className="space-y-1">
            <label
              htmlFor="rp-confirm"
              className="block text-sm font-medium leading-none"
            >
              Confirm new password
            </label>
            <input
              id="rp-confirm"
              type="password"
              name="confirmPassword"
              required
              autoComplete="new-password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              placeholder="••••••••"
              aria-describedby={matchError ? 'rp-error' : undefined}
              className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
            />
          </div>

          {matchError && (
            <p id="rp-error" role="alert" className="text-sm text-destructive">
              {matchError}
            </p>
          )}

          <button
            type="submit"
            disabled={isPending || !password.trim()}
            className="inline-flex w-full items-center justify-center rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:opacity-90 transition-opacity disabled:pointer-events-none disabled:opacity-50"
          >
            {isPending ? 'Updating…' : 'Update password'}
          </button>
        </form>

        <p className="text-center text-sm text-muted-foreground">
          <a
            href={loginHref}
            className="underline underline-offset-4 hover:text-foreground transition-colors"
          >
            Back to sign in
          </a>
        </p>
      </div>
    </main>
  )
}
