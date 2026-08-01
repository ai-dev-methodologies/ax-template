/*
---
template_id: L2/blocks/offline-banner
layer: L2
provenance_class: external_canonical
evidence:
  # All three anchors re-anchored 2026-08-01 (BACKLOG P2-73). The first sentence was
  # rewritten upstream; the second was a colon-terminated lead-in someone wrote to introduce
  # a code block; the third was authored advice that MDN does not give (the page says
  # nothing about visibilitychange). PROTECTED LEDGER IDENTITY — re-anchored, not deleted.
  # Quotes below are copied verbatim from the 2026-08-01 extractor output appended to the
  # snapshot. The visibilitychange recheck this component performs is therefore an
  # ax-template design decision; what MDN does support is that the property is unreliable
  # and should only produce hints — which is what the third quote now says.
  - source_type: upstream_id
    upstream_id: mdn-navigator-online-2026-05
    section: "navigator.onLine"
    quote: "The onLine property of the Navigator interface returns whether the device is connected to the network, with true meaning online and false meaning offline."
  - source_type: upstream_id
    upstream_id: mdn-navigator-online-2026-05
    section: "online / offline events"
    quote: "To see changes in the network state, use addEventListener to listen for the events on window.online and window.offline , as in the following example:"
  - source_type: upstream_id
    upstream_id: mdn-navigator-online-2026-05
    section: "visibilitychange pattern"
    quote: "Therefore, this property is inherently unreliable, and you should not disable features based on the online status, only provide hints when the user may seem offline."
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
