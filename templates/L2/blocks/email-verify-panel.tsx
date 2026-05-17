/*
---
template_id: L2/blocks/email-verify-panel
layer: L2
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "OWASP Authentication Cheat Sheet — email verification"
    url: "https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html"
  - source_type: internal
    rationale: "L2 auth block — renders email verification state; resend callback stays in L4."
dependencies: []
imports_from: [L1]
imports_forbidden: [L4, app/, lib/auth/]
---
*/
import * as React from 'react'

export type EmailVerifyStatus = 'pending' | 'success' | 'error'

export interface EmailVerifyPanelProps {
  status: EmailVerifyStatus
  /** Redacted email address shown to the user */
  email?: string
  /** Called when user requests resend */
  onResend?: () => void
  /** Show resend in-flight */
  isResending?: boolean
  errorMessage?: string
}

export default function EmailVerifyPanel({
  status,
  email,
  onResend,
  isResending = false,
  errorMessage,
}: EmailVerifyPanelProps) {
  return (
    <div
      role="status"
      aria-live="polite"
      className="flex flex-col items-center gap-4 py-10 text-center"
    >
      {status === 'pending' && (
        <>
          <p className="text-sm text-muted-foreground">
            We sent a verification link to{' '}
            {email ? <strong>{email}</strong> : 'your email'}.
          </p>
          <p className="text-sm text-muted-foreground">
            Check your inbox and click the link to verify your account.
          </p>
          {onResend && (
            <button
              type="button"
              onClick={onResend}
              disabled={isResending}
              className="text-sm underline underline-offset-4 hover:text-primary disabled:pointer-events-none disabled:opacity-50"
            >
              {isResending ? 'Sending…' : 'Resend email'}
            </button>
          )}
        </>
      )}

      {status === 'success' && (
        <p className="text-sm font-medium text-green-600 dark:text-green-400">
          Email verified — you're all set!
        </p>
      )}

      {status === 'error' && (
        <p role="alert" className="text-sm font-medium text-destructive">
          {errorMessage ?? 'Verification failed. The link may have expired.'}
        </p>
      )}
    </div>
  )
}
