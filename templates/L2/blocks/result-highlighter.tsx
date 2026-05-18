/*
---
template_id: L2/blocks/result-highlighter
layer: L2
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "WCAG 2.2 — 1.4.1 Use of Color: color must not be the only means of conveying information. Highlighted matches use both background color AND bold weight to communicate relevance."
    url: "https://www.w3.org/TR/WCAG22/#use-of-color"
  - source_type: external
    citation: "MDN Web Docs — <mark>: The HTML Mark Text element represents text highlighted for reference or notation purposes. Semantically communicates relevance to assistive technologies."
    url: "https://developer.mozilla.org/en-US/docs/Web/HTML/Element/mark"
imports_from: [L1, L2]
imports_forbidden: [L4, app/, lib/auth/]
---
*/
'use client'

import * as React from 'react'

// ─── types ────────────────────────────────────────────────────────────────────

export interface ResultHighlighterProps {
  /** The full text to display. */
  text: string
  /** The query string to highlight within the text. */
  query: string
  /**
   * Extra className applied to matched `<mark>` elements.
   * @default 'bg-yellow-200 dark:bg-yellow-700/60 font-semibold rounded-sm px-0.5'
   */
  highlightClassName?: string
  /** className applied to the root `<span>`. */
  className?: string
  /** Maximum characters to display. Truncates with ellipsis at word boundary. */
  maxLength?: number
}

// ─── helpers ─────────────────────────────────────────────────────────────────

interface TextSegment {
  text: string
  highlight: boolean
}

/**
 * Splits `text` into segments, marking which portions match `query`.
 * Case-insensitive, safe against regex special characters.
 */
function splitIntoSegments(text: string, query: string): TextSegment[] {
  const trimmed = query.trim()
  if (!trimmed) return [{ text, highlight: false }]

  // Escape regex special characters in query
  const escaped = trimmed.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

  // Split on the query (case-insensitive). Capturing group preserves the matched text.
  const regex = new RegExp(`(${escaped})`, 'gi')
  const parts = text.split(regex)

  return parts.map((part) => ({
    text: part,
    highlight: regex.test(part),
  }))
}

/**
 * Truncates text to maxLength at a word boundary, appending '…'.
 */
function truncate(text: string, maxLength: number): string {
  if (text.length <= maxLength) return text
  const cut = text.slice(0, maxLength)
  const lastSpace = cut.lastIndexOf(' ')
  return (lastSpace > 0 ? cut.slice(0, lastSpace) : cut) + '…'
}

// ─── component ───────────────────────────────────────────────────────────────

/**
 * ResultHighlighter — renders text with query matches wrapped in `<mark>` tags.
 *
 * Safety:
 *   - No `dangerouslySetInnerHTML`. Text is split into segments and rendered
 *     as React elements — XSS-safe by default.
 *   - Query regex special characters are escaped.
 *
 * Accessibility:
 *   - Uses semantic `<mark>` element (communicates relevance to screen readers).
 *   - Matches use both background color AND font-weight (WCAG 1.4.1 — not color alone).
 *
 * Fork instructions:
 *   1. Use inside search result list items to highlight matched text.
 *   2. If the backend returns ts_headline snippets, pass the snippet as `text`.
 *   3. Pass `maxLength` to cap snippet length in compact result cards.
 *
 * @example
 * ```tsx
 * // In a search result list item:
 * <ResultHighlighter text={result.title} query={searchQuery} />
 * <ResultHighlighter
 *   text={result.snippet}
 *   query={searchQuery}
 *   maxLength={120}
 *   className="text-sm text-muted-foreground"
 * />
 * ```
 */
export default function ResultHighlighter({
  text,
  query,
  highlightClassName = 'bg-yellow-200 dark:bg-yellow-700/60 font-semibold rounded-sm px-0.5',
  className,
  maxLength,
}: ResultHighlighterProps) {
  const displayText = maxLength ? truncate(text, maxLength) : text
  const segments = splitIntoSegments(displayText, query)

  return (
    <span className={className}>
      {segments.map((seg, i) =>
        seg.highlight ? (
          <mark key={i} className={highlightClassName}>
            {seg.text}
          </mark>
        ) : (
          <React.Fragment key={i}>{seg.text}</React.Fragment>
        ),
      )}
    </span>
  )
}
