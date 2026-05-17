/*
---
template_id: L2/blocks/offline-banner
layer: L2
provenance_class: external_canonical
evidence:
  - source_type: upstream_id
    upstream_id: mdn-navigator-online-2026-05
    section: "navigator.onLine"
    quote: "Navigator.onLine returns the online status of the browser. The property returns a boolean value, with true meaning online and false meaning offline."
  - source_type: upstream_id
    upstream_id: mdn-navigator-online-2026-05
    section: "online / offline events"
    quote: "Listen for network state changes via window events: window.addEventListener('online', ...) window.addEventListener('offline', ...)"
  - source_type: upstream_id
    upstream_id: mdn-navigator-online-2026-05
    section: "visibilitychange pattern"
    quote: "Recheck navigator.onLine when the document becomes visible again to catch transitions that occurred while the tab was in the background."
dependencies: []
imports_from: [L1]
imports_forbidden: [L4, app/, lib/auth/, lib/payment/]
---
*/
'use client'

import * as React from 'react'

// ─── hook ─────────────────────────────────────────────────────────────────────

function useOnlineStatus(): boolean {
  const [isOnline, setIsOnline] = React.useState<boolean>(
    typeof navigator !== 'undefined' ? navigator.onLine : true
  )

  React.useEffect(() => {
    const goOnline = () => setIsOnline(true)
    const goOffline = () => setIsOnline(false)
    const onVisibilityChange = () => {
      if (!document.hidden) {
        setIsOnline(navigator.onLine)
      }
    }

    window.addEventListener('online', goOnline)
    window.addEventListener('offline', goOffline)
    document.addEventListener('visibilitychange', onVisibilityChange)

    return () => {
      window.removeEventListener('online', goOnline)
      window.removeEventListener('offline', goOffline)
      document.removeEventListener('visibilitychange', onVisibilityChange)
    }
  }, [])

  return isOnline
}

// ─── component ────────────────────────────────────────────────────────────────

export interface OfflineBannerProps {
  /** Message shown while offline. */
  message?: string
  /** Custom class name for the banner container. */
  className?: string
}

/**
 * OfflineBanner — shows a sticky banner whenever the user loses network access.
 *
 * Listens to:
 *   - `window` 'online' / 'offline' events (MDN navigator.onLine)
 *   - `document` 'visibilitychange' to recheck after tab switch
 *
 * L4 usage — place near the top of your root layout, inside the `<body>`:
 *   import OfflineBanner from 'templates/L2/blocks/offline-banner'
 *   // app/layout.tsx
 *   <OfflineBanner />
 *   {children}
 */
export default function OfflineBanner({
  message = 'You are offline. Please check your network connection.',
  className,
}: OfflineBannerProps) {
  const isOnline = useOnlineStatus()

  if (isOnline) return null

  return (
    <div
      role="alert"
      aria-live="assertive"
      aria-atomic="true"
      data-testid="offline-banner"
      className={[
        'fixed top-0 left-0 right-0 z-50',
        'flex items-center justify-center gap-2',
        'bg-destructive text-destructive-foreground',
        'px-4 py-2 text-sm font-medium shadow-md',
        className ?? '',
      ]
        .filter(Boolean)
        .join(' ')}
    >
      <span aria-hidden="true">⚠️</span>
      <span>{message}</span>
    </div>
  )
}
