/*
---
template_id: L3/pages/account-locked
layer: L3
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "Next.js 16 App Router file conventions — page.tsx"
    url: "https://nextjs.org/docs/app/building-your-application/routing/pages"
  - source_type: external
    citation: "OWASP Authentication Cheat Sheet — Account Lockout"
    url: "https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html#account-lockout"
  - source_type: internal_design
    rationale: "Generic account-locked display skeleton. Shows lock reason (admin-action or time-based) and unlock instructions. No interactive elements — purely informational. L4 provides context-specific content via slots."
imports_from: [L1, L2]
imports_forbidden: [L4]
---
*/
import * as React from 'react'

/**
 * AccountLockedPage — generic account-locked informational skeleton.
 *
 * Slot props:
 *   - reason        (optional) short description of why the account is locked
 *   - unlockAt      (optional) ISO-8601 string when time-based unlock happens
 *   - adminContact  (optional) email or link for contacting admin
 *   - loginHref     (optional) sign-in link href (shown once user can retry)
 *   - customSlot    (optional) fully custom body content (replaces built-in reason block)
 *
 * L4 usage:
 *   import AccountLockedPage from 'templates/L3/pages/account-locked/page'
 *   export default async function LockedRoute() {
 *     const lock = await getLockInfo(session.userId)
 *     return (
 *       <AccountLockedPage
 *         reason={lock.reason}
 *         unlockAt={lock.unlockAt}
 *         adminContact="support@example.com"
 *         loginHref="/login"
 *       />
 *     )
 *   }
 */
export interface AccountLockedPageProps {
  /** Short reason string (e.g. "Too many failed sign-in attempts") */
  reason?: string
  /** ISO-8601 timestamp when time-based unlock occurs */
  unlockAt?: string
  /** Admin contact email or URL for manual unlock requests */
  adminContact?: string
  /** Sign-in link href (renders once user can try again) */
  loginHref?: string
  /** Overrides built-in body content */
  customSlot?: React.ReactNode
}

export default function AccountLockedPage({
  reason = 'Too many failed sign-in attempts.',
  unlockAt,
  adminContact,
  loginHref,
  customSlot,
}: AccountLockedPageProps) {
  const unlockDate = unlockAt ? new Date(unlockAt) : null
  const unlockLabel = unlockDate
    ? unlockDate.toLocaleString(undefined, {
        dateStyle: 'medium',
        timeStyle: 'short',
      })
    : null

  return (
    <main className="flex min-h-svh items-center justify-center px-4">
      <div className="w-full max-w-sm text-center space-y-6">
        {/* Lock icon */}
        <div
          className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-red-100 text-red-700 text-2xl"
          role="img"
          aria-label="Account locked"
        >
          🔒
        </div>

        <div className="space-y-1">
          <h1 className="text-xl font-semibold tracking-tight">Account locked</h1>
          <p className="text-sm text-muted-foreground">
            Your account has been temporarily locked.
          </p>
        </div>

        {/* Custom slot overrides built-in reason block */}
        {customSlot ?? (
          <div className="rounded-lg border bg-muted/40 px-4 py-4 text-sm space-y-3 text-left">
            <p>
              <span className="font-medium">Reason: </span>
              {reason}
            </p>

            {unlockLabel && (
              <p>
                <span className="font-medium">Unlocks at: </span>
                {unlockLabel}
              </p>
            )}

            {adminContact && (
              <p>
                <span className="font-medium">Need help? </span>
                {adminContact.startsWith('http') ? (
                  <a
                    href={adminContact}
                    className="underline underline-offset-4 hover:text-foreground transition-colors"
                  >
                    Contact support
                  </a>
                ) : (
                  <a
                    href={`mailto:${adminContact}`}
                    className="underline underline-offset-4 hover:text-foreground transition-colors"
                  >
                    {adminContact}
                  </a>
                )}
              </p>
            )}
          </div>
        )}

        {loginHref && (
          <a
            href={loginHref}
            className="inline-flex items-center justify-center rounded-md border px-6 py-2 text-sm font-medium hover:bg-muted transition-colors"
          >
            Back to sign in
          </a>
        )}
      </div>
    </main>
  )
}
