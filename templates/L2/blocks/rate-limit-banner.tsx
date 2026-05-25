/*
---
template_id: L2/blocks/rate-limit-banner
layer: L2
provenance_class: external_canonical
evidence:
  - source_type: external
    citation: "RFC 6585 §4 — HTTP Status Code 429 (Too Many Requests). The response representations SHOULD include details explaining the condition, and MAY include a Retry-After header indicating how long to wait before making a new request."
    url: "https://www.rfc-editor.org/rfc/rfc6585#section-4"
  - source_type: external
    citation: "RFC 9110 §10.2.3 — Retry-After. The Retry-After header field indicates how long the user agent ought to wait before making a follow-up request. Either an HTTP-date or a non-negative decimal integer (delta-seconds)."
    url: "https://www.rfc-editor.org/rfc/rfc9110.html#name-retry-after"
  - source_type: external
    citation: "WCAG 2.2 SC 4.1.3 Status Messages (Level AA) — Status messages can be programmatically determined through role or properties so they can be presented by assistive technologies without receiving focus."
    url: "https://www.w3.org/WAI/WCAG22/Understanding/status-messages.html"
  - source_type: internal
    rationale: "R56 ratelimit pivot — the ratelimit L4 spec is server-only (`scope: api-rate-limiting`, items RATELIMIT-1/2/3/4 all describe server behavior), so a frontend full-trio would have no spec-anchored surface. Instead, R56 ships a cross-cutting L2 primitive that turns any 429 response (with optional Retry-After) into a user-visible countdown banner — useful for every L4 vertical, not just one."
dependencies: []
imports_from: [L1]
imports_forbidden: [L4, app/, lib/]
---
*/
'use client'

import * as React from 'react'

// ─── helpers ──────────────────────────────────────────────────────────────────

/**
 * Parse the Retry-After header value per RFC 9110 §10.2.3.
 *
 * Two forms:
 *  - delta-seconds: a non-negative integer count of seconds
 *  - HTTP-date    : e.g. "Wed, 21 Oct 2026 07:28:00 GMT"
 *
 * Returns the wait time in seconds, or `null` if the value is missing /
 * unparseable. We refuse to fabricate a default — callers can pick one.
 */
export function parseRetryAfter(value: string | null | undefined, now: Date = new Date()): number | null {
  if (value == null) return null
  const trimmed = value.trim()
  if (trimmed.length === 0) return null

  // delta-seconds form: must be a non-negative integer
  if (/^\d+$/.test(trimmed)) {
    const n = parseInt(trimmed, 10)
    return Number.isFinite(n) && n >= 0 ? n : null
  }

  // HTTP-date form
  const parsed = Date.parse(trimmed)
  if (Number.isNaN(parsed)) return null
  const deltaMs = parsed - now.getTime()
  if (deltaMs <= 0) return 0
  return Math.ceil(deltaMs / 1000)
}

/**
 * Convenience helper for fetch wrappers — extracts Retry-After (or null).
 * Returns null for non-429 responses so callers can chain
 * `extractRetryAfterFrom429(res)` without an outer if.
 */
export function extractRetryAfterFrom429(res: Response): number | null {
  if (res.status !== 429) return null
  return parseRetryAfter(res.headers.get('Retry-After'))
}

// ─── context ──────────────────────────────────────────────────────────────────

interface RateLimitState {
  /** Seconds remaining until the banner should auto-dismiss. */
  secondsRemaining: number
  /** Optional descriptive source so the banner reads "Rate-limited on <source>". */
  source: string | null
}

interface RateLimitBannerContextValue {
  /**
   * Trigger the banner with a known wait time. Pass `null` for unknown
   * (no Retry-After header) — the banner uses {@link unknownDurationSec}.
   */
  notify429(retryAfterSeconds: number | null, source?: string): void
  /** Dismiss the banner immediately. */
  dismiss(): void
  /** Current state (null when no rate limit active). */
  state: RateLimitState | null
}

const RateLimitBannerContext = React.createContext<RateLimitBannerContextValue | null>(null)

export function useRateLimitBanner(): RateLimitBannerContextValue {
  const ctx = React.useContext(RateLimitBannerContext)
  if (!ctx) throw new Error('useRateLimitBanner must be used within <RateLimitBannerProvider>')
  return ctx
}

// ─── provider ─────────────────────────────────────────────────────────────────

export interface RateLimitBannerProviderProps {
  children: React.ReactNode
  /**
   * Wait time displayed when the server omits a Retry-After header. Default 30s
   * — a value that is short enough to feel responsive yet long enough that the
   * user is unlikely to immediately re-fire the same request.
   */
  unknownDurationSec?: number
  /**
   * Banner copy template. Receives the current `secondsRemaining` and optional
   * source. Default produces "Too many requests — retry in {n}s" / "...on
   * {source} — retry in {n}s".
   */
  formatMessage?: (secondsRemaining: number, source: string | null) => string
}

const defaultFormat = (sec: number, source: string | null): string => {
  const tail = sec > 0 ? `retry in ${sec}s` : 'you can retry now'
  return source ? `Too many requests on ${source} — ${tail}` : `Too many requests — ${tail}`
}

/**
 * RateLimitBannerProvider — exposes {@link useRateLimitBanner} and renders the
 * sticky countdown banner.
 *
 * Setup (root layout):
 *
 * ```tsx
 * <RateLimitBannerProvider>{children}</RateLimitBannerProvider>
 * ```
 *
 * Usage (any fetch wrapper):
 *
 * ```tsx
 * const { notify429 } = useRateLimitBanner()
 * const res = await fetch('/api/items')
 * if (res.status === 429) {
 *   notify429(extractRetryAfterFrom429(res), 'items list')
 * }
 * ```
 */
export function RateLimitBannerProvider({
  children,
  unknownDurationSec = 30,
  formatMessage = defaultFormat,
}: RateLimitBannerProviderProps) {
  const [state, setState] = React.useState<RateLimitState | null>(null)

  const notify429 = React.useCallback(
    (retryAfterSeconds: number | null, source?: string) => {
      const seconds =
        retryAfterSeconds !== null && retryAfterSeconds >= 0
          ? retryAfterSeconds
          : unknownDurationSec
      setState({ secondsRemaining: seconds, source: source ?? null })
    },
    [unknownDurationSec],
  )

  const dismiss = React.useCallback(() => setState(null), [])

  // Countdown ticker — runs while a banner is active. Single interval shared
  // across all triggers so back-to-back 429s update the same timer instead of
  // spawning multiple intervals.
  React.useEffect(() => {
    if (!state) return
    if (state.secondsRemaining <= 0) {
      // The banner shows "you can retry now" for one render then dismisses.
      const finalTimer = window.setTimeout(() => setState(null), 1500)
      return () => window.clearTimeout(finalTimer)
    }
    const interval = window.setInterval(() => {
      setState((prev) =>
        prev ? { ...prev, secondsRemaining: Math.max(0, prev.secondsRemaining - 1) } : prev,
      )
    }, 1000)
    return () => window.clearInterval(interval)
  }, [state])

  const value = React.useMemo(
    () => ({ notify429, dismiss, state }),
    [notify429, dismiss, state],
  )

  return (
    <RateLimitBannerContext.Provider value={value}>
      {children}
      {state && (
        <RateLimitBanner
          source={state.source}
          message={formatMessage(state.secondsRemaining, state.source)}
          onDismiss={dismiss}
        />
      )}
    </RateLimitBannerContext.Provider>
  )
}

// ─── presentation component ───────────────────────────────────────────────────

interface RateLimitBannerInternalProps {
  source: string | null
  message: string
  onDismiss: () => void
}

function RateLimitBanner({ source, message, onDismiss }: RateLimitBannerInternalProps) {
  // WCAG SC 4.1.3 — status (polite) is right for a non-critical wait: an alert
  // would interrupt the user's current screen-reader context, which is heavier
  // than the 429 warrants. The banner is also visually present, so polite is
  // sufficient.
  return (
    <div
      role="status"
      aria-live="polite"
      aria-atomic="true"
      data-testid="rate-limit-banner"
      data-source={source ?? undefined}
      className={[
        'fixed top-0 left-0 right-0 z-50',
        'flex items-center justify-between gap-3',
        'bg-amber-100 text-amber-900',
        'px-4 py-2 text-sm font-medium shadow-md',
      ].join(' ')}
    >
      <span className="flex items-center gap-2">
        <span aria-hidden="true">⏳</span>
        <span>{message}</span>
      </span>
      <button
        type="button"
        className="shrink-0 text-xs underline hover:no-underline focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
        onClick={onDismiss}
        // Dismiss is non-destructive — the user just acknowledges. The seconds
        // counter is informational, not a hard gate.
      >
        Dismiss
      </button>
    </div>
  )
}
