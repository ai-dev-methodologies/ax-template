/*
---
template_id: L2/blocks/announce-live
layer: L2
provenance_class: external_canonical
evidence:
  - source_type: external
    citation: "WCAG 2.2 SC 4.1.3 Status Messages (Level AA): Status messages can be programmatically determined through role or properties so they can be presented by assistive technologies without receiving focus. aria-live='assertive' is used for urgent messages; aria-live='polite' for non-urgent updates."
    url: "https://www.w3.org/WAI/WCAG22/Understanding/status-messages.html"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "WebAIM — ARIA Live Regions: Create a live region container that is present in the DOM on page load, then update its content to trigger announcements. Do not create live regions dynamically — screen readers may miss the initial announcement."
    url: "https://webaim.org/techniques/aria/#arialive"
    quoted_at: "2026-05-18"
dependencies: []
imports_from: [L1]
imports_forbidden: [L4, app/, lib/auth/, lib/payment/]
---
*/
'use client'

import * as React from 'react'

// ─── Context ──────────────────────────────────────────────────────────────────

interface AnnounceLiveContextValue {
  /** Announce a polite (non-urgent) message to screen readers. */
  announce: (message: string) => void
  /** Announce an assertive (urgent) message to screen readers. */
  announceAssertive: (message: string) => void
}

const AnnounceLiveContext = React.createContext<AnnounceLiveContextValue | null>(null)

export function useAnnounce(): AnnounceLiveContextValue {
  const ctx = React.useContext(AnnounceLiveContext)
  if (!ctx) throw new Error('useAnnounce must be used within <AnnounceLiveProvider>')
  return ctx
}

// ─── Provider ─────────────────────────────────────────────────────────────────

export interface AnnounceLiveProviderProps {
  children: React.ReactNode
}

/**
 * AnnounceLiveProvider — provides `useAnnounce()` to the subtree.
 *
 * Renders two visually-hidden live regions (one polite, one assertive)
 * that persist throughout the page lifetime. Updating their text content
 * triggers a screen-reader announcement without moving focus.
 *
 * ## Setup
 *
 * ```tsx
 * // app/layout.tsx — wrap at the root level
 * import { AnnounceLiveProvider } from 'templates/L2/blocks/announce-live'
 *
 * export default function RootLayout({ children }) {
 *   return (
 *     <html>
 *       <body>
 *         <AnnounceLiveProvider>
 *           {children}
 *         </AnnounceLiveProvider>
 *       </body>
 *     </html>
 *   )
 * }
 * ```
 *
 * ## Usage
 *
 * ```tsx
 * import { useAnnounce } from 'templates/L2/blocks/announce-live'
 *
 * function SaveButton() {
 *   const { announce } = useAnnounce()
 *   const handleSave = async () => {
 *     await save()
 *     announce('Settings saved successfully')
 *   }
 *   return <button onClick={handleSave}>Save</button>
 * }
 * ```
 */
export function AnnounceLiveProvider({ children }: AnnounceLiveProviderProps) {
  const [politeMsg, setPoliteMsg] = React.useState('')
  const [assertiveMsg, setAssertiveMsg] = React.useState('')

  // Brief delay trick: clear then set so same message re-triggers announcement
  const announce = React.useCallback((message: string) => {
    setPoliteMsg('')
    setTimeout(() => setPoliteMsg(message), 50)
  }, [])

  const announceAssertive = React.useCallback((message: string) => {
    setAssertiveMsg('')
    setTimeout(() => setAssertiveMsg(message), 50)
  }, [])

  return (
    <AnnounceLiveContext.Provider value={{ announce, announceAssertive }}>
      {children}

      {/* Polite live region — non-urgent status messages */}
      <div
        role="status"
        aria-live="polite"
        aria-atomic="true"
        data-testid="announce-live-polite"
        className="sr-only"
      >
        {politeMsg}
      </div>

      {/* Assertive live region — urgent alerts */}
      <div
        role="alert"
        aria-live="assertive"
        aria-atomic="true"
        data-testid="announce-live-assertive"
        className="sr-only"
      >
        {assertiveMsg}
      </div>
    </AnnounceLiveContext.Provider>
  )
}

/**
 * AnnounceLive — standalone live region (no context required).
 *
 * Use when you want a local live region without the full provider.
 * Prefer `AnnounceLiveProvider` + `useAnnounce()` for app-level status messages.
 */
export interface AnnounceLiveProps {
  /** Message to announce. Changing this prop triggers a new announcement. */
  message: string
  /** Urgency level (default: "polite"). */
  politeness?: 'polite' | 'assertive'
}

export default function AnnounceLive({
  message,
  politeness = 'polite',
}: AnnounceLiveProps) {
  return (
    <div
      role={politeness === 'assertive' ? 'alert' : 'status'}
      aria-live={politeness}
      aria-atomic="true"
      data-testid="announce-live"
      className="sr-only"
    >
      {message}
    </div>
  )
}
