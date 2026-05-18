/*
---
template_id: L2/blocks/network-status-pill
layer: L2
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "MDN — Navigator.onLine: returns the online status of the browser. Listen for 'online'/'offline' events on window."
    url: "https://developer.mozilla.org/en-US/docs/Web/API/Navigator/onLine"
    quoted_at: "2026-05-18"
  - source_type: internal
    rationale: "Inline pill for embedding network status inside toolbars or status bars. Complements OfflineBanner (full-width alert). Use this when you need an unobtrusive inline indicator rather than a sticky overlay."
dependencies: []
imports_from: [L1]
imports_forbidden: [L4, app/, lib/auth/, lib/payment/]
---
*/
'use client'

import * as React from 'react'

function useNetworkStatus(): 'online' | 'offline' | 'checking' {
  const [status, setStatus] = React.useState<'online' | 'offline' | 'checking'>('checking')

  React.useEffect(() => {
    setStatus(navigator.onLine ? 'online' : 'offline')

    const goOnline = () => setStatus('online')
    const goOffline = () => setStatus('offline')
    window.addEventListener('online', goOnline)
    window.addEventListener('offline', goOffline)

    return () => {
      window.removeEventListener('online', goOnline)
      window.removeEventListener('offline', goOffline)
    }
  }, [])

  return status
}

export interface NetworkStatusPillProps {
  /** When true, pill is only shown while offline (default: false — always shown). */
  offlineOnly?: boolean
  /** Custom class name. */
  className?: string
}

const STATUS_STYLES = {
  online: 'bg-green-100 text-green-800 border-green-300',
  offline: 'bg-red-100 text-red-800 border-red-300',
  checking: 'bg-gray-100 text-gray-600 border-gray-300',
} as const

const STATUS_DOT = {
  online: 'bg-green-500',
  offline: 'bg-red-500',
  checking: 'bg-gray-400',
} as const

const STATUS_LABEL = {
  online: 'Online',
  offline: 'Offline',
  checking: '…',
} as const

/**
 * NetworkStatusPill — compact inline status pill for toolbars and status bars.
 *
 * Distinct from `OfflineBanner` (full-width overlay). Use this when you need
 * an unobtrusive inline network indicator.
 *
 * ```tsx
 * import NetworkStatusPill from 'templates/L2/blocks/network-status-pill'
 *
 * // Inside AdminHeader
 * <div className="flex items-center gap-2">
 *   <NetworkStatusPill offlineOnly />
 *   <UserMenu />
 * </div>
 * ```
 */
export default function NetworkStatusPill({
  offlineOnly = false,
  className,
}: NetworkStatusPillProps) {
  const status = useNetworkStatus()

  if (offlineOnly && status !== 'offline') return null
  if (status === 'checking') return null

  return (
    <span
      role="status"
      aria-label={`Network status: ${STATUS_LABEL[status]}`}
      data-testid="network-status-pill"
      className={[
        'inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 text-xs font-medium',
        STATUS_STYLES[status],
        className ?? '',
      ]
        .filter(Boolean)
        .join(' ')}
    >
      <span
        aria-hidden="true"
        className={['h-1.5 w-1.5 rounded-full', STATUS_DOT[status]].join(' ')}
      />
      {STATUS_LABEL[status]}
    </span>
  )
}
