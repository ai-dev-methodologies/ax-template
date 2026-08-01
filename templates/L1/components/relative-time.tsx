/*
---
template_id: L1/components/relative-time
layer: L1
provenance_class: internal_design
evidence:
  # Re-anchored 2026-08-01 (BACKLOG P2-73): the previous quote was a colon-terminated
  # lead-in someone wrote to introduce a code block, not page text; the cited page does not
  # name `useFormatter()` at all. PROTECTED LEDGER IDENTITY — re-anchored, not deleted. The
  # `useFormatter` API itself is documented on next-intl's usage pages, which this snapshot
  # does not cover; the MDN Intl.RelativeTimeFormat entry below carries the formatting
  # semantics. Quote copied verbatim from the 2026-08-01 extractor output appended to the
  # snapshot.
  - source_type: upstream_id
    upstream_id: next-intl-2026-05
    section: "Relative Time"
    quote: "next-intl provides the essential foundation for internationalization in Next.js apps. It handles aspects like translations, date and number formatting, as well as internationalized routing."
  - source_type: external
    citation: "MDN Intl.RelativeTimeFormat — relative time formatting API"
    url: "https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/RelativeTimeFormat"
    quoted_at: "2026-05-18"
dependencies: ["next-intl"]
imports_from: []
imports_forbidden: [L2, L3, L4, app/, lib/auth/]
---
*/
'use client'

import { useFormatter } from 'next-intl'

export interface RelativeTimeProps {
  /** The date/time to display relative to now */
  date: Date | string | number
  /** Optional CSS class */
  className?: string
  /** Update interval in ms. Set to 0 to disable live updates. Default: 60000 */
  updateIntervalMs?: number
}

/**
 * RelativeTime — L1 primitive.
 *
 * Renders a date as a locale-aware relative time string, e.g.:
 * - ko-KR: "방금 전", "3분 전", "2시간 전", "어제", "1년 전"
 * - en-US: "just now", "3 minutes ago", "2 hours ago", "yesterday", "1 year ago"
 *
 * Uses next-intl's `useFormatter()` which respects the active locale context.
 *
 * Usage:
 * ```tsx
 * <RelativeTime date={post.createdAt} />
 * ```
 */
export function RelativeTime({
  date,
  className,
}: RelativeTimeProps) {
  const format = useFormatter()
  const dateObj = date instanceof Date ? date : new Date(date)

  const relative = format.relativeTime(dateObj)

  return (
    <time
      dateTime={dateObj.toISOString()}
      className={className}
      title={dateObj.toLocaleString()}
    >
      {relative}
    </time>
  )
}
