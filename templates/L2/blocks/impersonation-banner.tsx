/*
---
template_id: L2/blocks/impersonation-banner
layer: L2
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "OWASP Session Management Cheat Sheet — Admin impersonation sessions must be visually distinct and audited; the impersonated identity must always be visible to the operator."
    url: "https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html"
    quoted_at: "2026-05-18"
  - source_type: internal
    rationale: "Security-critical banner. Protects against silent impersonation — operator acts as another user without visible indication. Rule impersonation-banner-required-when-acting-as-other-user enforces this at the call site. Banner must be rendered whenever session.actingAs is non-null, regardless of the helper function name used to set that state."
dependencies: []
imports_from: [L1]
imports_forbidden: [L4, app/, lib/auth/, lib/payment/]
---
*/
'use client'

import * as React from 'react'

/**
 * The canonical session shape this component guards against.
 *
 * The ESLint rule `impersonation-banner-required-when-acting-as-other-user`
 * matches on ANY expression that sets `actingAs` to a non-null value:
 *
 *   session.actingAs = userId            ← direct mutation
 *   return { ...session, actingAs: id }  ← immutable update
 *   runAsUser(id)                        ← helper (any name) that returns {actingAs}
 *
 * The rule does NOT match on a specific helper function name.
 */
export interface ImpersonationSession {
  /** Non-null whenever an operator is acting as another user. */
  actingAs: string | null
  /** Display name of the impersonated user (optional, shown in banner). */
  actingAsDisplayName?: string
  /** The operator's own user ID (shown in banner for audit clarity). */
  operatorId?: string
}

export interface ImpersonationBannerProps {
  /** Current session object. Banner renders iff session.actingAs is non-null. */
  session: ImpersonationSession
  /** Called when the operator clicks "End impersonation". */
  onEndImpersonation?: () => void
  /** Custom banner label prefix (default: "Acting as"). */
  labelPrefix?: string
  /** Custom class name for the banner container. */
  className?: string
}

/**
 * ImpersonationBanner — security-critical sticky banner.
 *
 * Renders ONLY when `session.actingAs` is non-null. Shows who is being
 * impersonated and provides an "End impersonation" action.
 *
 * ## Usage
 *
 * ```tsx
 * import ImpersonationBanner from 'templates/L2/blocks/impersonation-banner'
 *
 * // app/layout.tsx (root layout — server component passes session to client)
 * export default async function RootLayout({ children }) {
 *   const session = await getSession()   // {actingAs: 'user-123', ...}
 *   return (
 *     <>
 *       <ImpersonationBanner session={session} onEndImpersonation={endImpersonation} />
 *       {children}
 *     </>
 *   )
 * }
 * ```
 *
 * ## Rule enforcement
 *
 * The ESLint rule `impersonation-banner-required-when-acting-as-other-user`
 * detects any code that sets `session.actingAs` (or returns `{actingAs: ...}`)
 * without a co-located `<ImpersonationBanner>`. It fires on the canonical state
 * mutation, not on a specific helper name, making helper-rename bypasses
 * impossible.
 *
 * ## WCAG
 * - `role="alert"` + `aria-live="assertive"` ensures screen readers announce
 *   the impersonation context immediately on mount.
 * - High-contrast amber palette meets WCAG 1.4.3 (contrast ratio ≥ 4.5:1).
 */
export default function ImpersonationBanner({
  session,
  onEndImpersonation,
  labelPrefix = 'Acting as',
  className,
}: ImpersonationBannerProps) {
  if (!session.actingAs) return null

  const displayName = session.actingAsDisplayName ?? session.actingAs

  return (
    <div
      role="alert"
      aria-live="assertive"
      aria-atomic="true"
      data-testid="impersonation-banner"
      className={[
        'fixed top-0 left-0 right-0 z-50',
        'flex items-center justify-between gap-3',
        'bg-amber-400 text-amber-950',
        'px-4 py-2 text-sm font-semibold shadow-md',
        className ?? '',
      ]
        .filter(Boolean)
        .join(' ')}
    >
      <span className="flex items-center gap-2">
        <span aria-hidden="true">👤</span>
        <span>
          {labelPrefix}:{' '}
          <strong data-testid="impersonation-banner-target">{displayName}</strong>
          {session.operatorId && (
            <span className="ml-2 font-normal opacity-70">
              (operator: {session.operatorId})
            </span>
          )}
        </span>
      </span>

      {onEndImpersonation && (
        <button
          type="button"
          onClick={onEndImpersonation}
          className="rounded border border-amber-700 bg-amber-100 px-3 py-0.5 text-xs font-semibold text-amber-900 hover:bg-amber-200 focus:outline-none focus:ring-2 focus:ring-amber-700"
          data-testid="end-impersonation-btn"
        >
          End impersonation
        </button>
      )}
    </div>
  )
}
