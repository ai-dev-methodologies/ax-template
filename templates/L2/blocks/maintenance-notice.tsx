/*
---
template_id: L2/blocks/maintenance-notice
layer: L2
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "WCAG 2.2 SC 4.1.3 — Status Messages must be programmatically determinable through role or properties so they can be announced by screen readers without receiving focus."
    url: "https://www.w3.org/WAI/WCAG22/Understanding/status-messages.html"
    quoted_at: "2026-05-18"
  - source_type: internal
    rationale: "Scheduled-maintenance / degraded-service banner. Renders based on a CallerProvided flag or timestamp range. Dismissible via localStorage key so admins can hide it per-session."
dependencies: []
imports_from: [L1]
imports_forbidden: [L4, app/, lib/auth/, lib/payment/]
---
*/
'use client'

import * as React from 'react'

export type MaintenanceSeverity = 'info' | 'warning' | 'critical'

export interface MaintenanceNoticeProps {
  /** Whether maintenance mode is active. Banner renders only when true. */
  active: boolean
  /** Title line (default: "Scheduled Maintenance"). */
  title?: string
  /** Body message shown below the title. */
  message?: string
  /** Severity affects visual style (default: "warning"). */
  severity?: MaintenanceSeverity
  /** ISO timestamp when maintenance ends — displayed if provided. */
  endsAt?: string
  /** If set, show a dismiss button that hides the banner for this session. */
  dismissKey?: string
  /** Custom class name for the container. */
  className?: string
}

const SEVERITY_STYLES: Record<MaintenanceSeverity, string> = {
  info: 'bg-blue-50 border-blue-300 text-blue-900',
  warning: 'bg-yellow-50 border-yellow-400 text-yellow-900',
  critical: 'bg-red-50 border-red-400 text-red-900',
}

const SEVERITY_ICONS: Record<MaintenanceSeverity, string> = {
  info: 'ℹ️',
  warning: '⚠️',
  critical: '🔴',
}

/**
 * MaintenanceNotice — a prominently styled banner for announcing system maintenance
 * or degraded service. Renders only when `active` is true.
 *
 * ## Usage
 * ```tsx
 * import MaintenanceNotice from 'templates/L2/blocks/maintenance-notice'
 *
 * // app/layout.tsx
 * <MaintenanceNotice
 *   active={maintenanceModeFlag}
 *   title="Scheduled Maintenance"
 *   message="The platform will be unavailable from 02:00–04:00 KST."
 *   endsAt="2026-05-19T04:00:00+09:00"
 *   severity="warning"
 *   dismissKey="maintenance-2026-05-19"
 * />
 * ```
 */
export default function MaintenanceNotice({
  active,
  title = 'Scheduled Maintenance',
  message,
  severity = 'warning',
  endsAt,
  dismissKey,
  className,
}: MaintenanceNoticeProps) {
  const [dismissed, setDismissed] = React.useState<boolean>(() => {
    if (!dismissKey || typeof window === 'undefined') return false
    return sessionStorage.getItem(`maintenance-dismissed:${dismissKey}`) === '1'
  })

  if (!active || dismissed) return null

  const handleDismiss = () => {
    if (dismissKey) {
      sessionStorage.setItem(`maintenance-dismissed:${dismissKey}`, '1')
    }
    setDismissed(true)
  }

  const formattedEnd = endsAt
    ? new Date(endsAt).toLocaleString('ko-KR', {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        timeZoneName: 'short',
      })
    : null

  return (
    <div
      role="status"
      aria-live="polite"
      aria-atomic="true"
      data-testid="maintenance-notice"
      className={[
        'w-full border-b px-4 py-3',
        'flex items-start gap-3',
        SEVERITY_STYLES[severity],
        className ?? '',
      ]
        .filter(Boolean)
        .join(' ')}
    >
      <span aria-hidden="true" className="mt-0.5 text-lg leading-none">
        {SEVERITY_ICONS[severity]}
      </span>

      <div className="flex-1 min-w-0">
        <p className="font-semibold text-sm">{title}</p>
        {message && <p className="text-sm mt-0.5">{message}</p>}
        {formattedEnd && (
          <p className="text-xs mt-1 opacity-75">Expected to end: {formattedEnd}</p>
        )}
      </div>

      {dismissKey && (
        <button
          type="button"
          onClick={handleDismiss}
          aria-label="Dismiss maintenance notice"
          className="shrink-0 rounded p-0.5 opacity-60 hover:opacity-100 focus:outline-none focus-visible:ring-2 focus-visible:ring-current"
        >
          ✕
        </button>
      )}
    </div>
  )
}
