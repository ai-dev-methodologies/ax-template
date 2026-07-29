/*
---
template_id: L1/components/code-block
layer: L1
provenance_class: external_canonical
evidence:
  - source_type: external
    citation: "MDN Web Docs — <code> element: the Code element represents a fragment of computer code."
    url: "https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/code"
  - source_type: external
    citation: "MDN Web Docs — <pre> element: preformatted text which is to be presented exactly as written in the HTML file."
    url: "https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/pre"
  - source_type: external
    citation: "WCAG 2.2 SC 1.3.1 Info and Relationships (Level A) — full normative text, W3C Recommendation 2023-10-05"
    url: "https://www.w3.org/TR/WCAG22/#info-and-relationships"
    quote: "Information, structure, and relationships conveyed through presentation can be programmatically determined or are available in text."
    quoted_at: "2026-07-29"
a11y_criteria:
  - "WCAG 2.2 SC 1.3.1 — <pre><code> conveys code block semantics programmatically"
  - "WCAG 2.2 SC 2.1.1 — copy button keyboard accessible (Enter/Space triggers copy)"
  - "WCAG 2.2 SC 4.1.3 — copy success state announced via aria-live region"
dependencies: []
---
*/

'use client'

import * as React from 'react'
import { cn } from '../lib/utils'

// ─── Types ────────────────────────────────────────────────────────────────────

export interface CodeBlockProps {
  /** Source code string to display */
  code: string
  /**
   * Language identifier for display label and optional syntax class.
   * Fork receivers may integrate a syntax highlighter (shiki, prism, highlight.js)
   * by reading the `data-language` attribute on the rendered <code> element.
   */
  language?: string
  /** Show line numbers (default: false) */
  showLineNumbers?: boolean
  /** Show copy-to-clipboard button (default: true) */
  showCopyButton?: boolean
  /** Optional filename or title shown in the header */
  filename?: string
  /** Additional className for the outer container */
  className?: string
}

// ─── CodeBlock ────────────────────────────────────────────────────────────────

/**
 * CodeBlock — accessible code display block with copy-to-clipboard.
 *
 * Renders a semantic `<pre><code>` pair styled with design tokens.
 * The `data-language` attribute on `<code>` enables integration with any
 * syntax highlighter (shiki, Prism, highlight.js) at the fork-receiver level:
 *
 * ```tsx
 * // Fork-receiver: swap in shiki highlight via useEffect or server-side
 * const highlighted = await codeToHtml(code, { lang: language, theme: 'github-dark' })
 * <div dangerouslySetInnerHTML={{ __html: highlighted }} />
 * ```
 *
 * **No external syntax library dependency.** This component is intentionally
 * lightweight. Heavy highlighters should follow the `rich-content-must-use-dynamic-import`
 * rule: import them via `next/dynamic` or `React.lazy` + Suspense.
 *
 * @example
 * <CodeBlock code={snippet} language="typescript" showLineNumbers filename="utils.ts" />
 */
export function CodeBlock({
  code,
  language,
  showLineNumbers = false,
  showCopyButton = true,
  filename,
  className,
}: CodeBlockProps) {
  const [copied, setCopied] = React.useState(false)
  const liveRef = React.useRef<HTMLSpanElement>(null)

  const handleCopy = React.useCallback(async () => {
    try {
      await navigator.clipboard.writeText(code)
      setCopied(true)

      // Announce to screen readers
      if (liveRef.current) {
        liveRef.current.textContent = '코드가 복사되었습니다'
      }

      setTimeout(() => {
        setCopied(false)
        if (liveRef.current) liveRef.current.textContent = ''
      }, 2000)
    } catch {
      // clipboard API unavailable (non-HTTPS / old browser)
    }
  }, [code])

  const lines = code.split('\n')

  return (
    <div
      className={cn(
        'group relative overflow-hidden rounded-[--radius-md]',
        'border border-[--color-border]',
        'bg-[--color-surface-code, --color-surface-subtle]',
        className
      )}
    >
      {/* Header bar (filename + language label) */}
      {(filename || language) && (
        <div
          className={cn(
            'flex items-center justify-between',
            'border-b border-[--color-border]',
            'px-[--space-4] py-[--space-2]',
            'bg-[--color-surface-subtle]'
          )}
        >
          <span className="truncate text-[length:--text-xs] text-[--color-text-muted]">
            {filename ?? language}
          </span>
          {language && filename && (
            <span
              className={cn(
                'ml-[--space-2] shrink-0 rounded px-[--space-1] py-px',
                'bg-[--color-surface-code, --color-surface] text-[length:--text-xs]',
                'font-mono text-[--color-text-muted]'
              )}
            >
              {language}
            </span>
          )}
        </div>
      )}

      {/* Code area */}
      <div className="relative overflow-x-auto">
        <pre
          className={cn(
            'px-[--space-4] py-[--space-3]',
            'text-[length:--text-sm] leading-relaxed',
            showLineNumbers && 'pl-[--space-2]'
          )}
        >
          {showLineNumbers ? (
            <code className="flex" data-language={language}>
              {/* Line number column */}
              <span
                aria-hidden="true"
                className={cn(
                  'mr-[--space-4] select-none text-right',
                  'text-[--color-text-placeholder]',
                  'w-[2.5rem] shrink-0'
                )}
              >
                {lines.map((_, i) => (
                  <span key={i} className="block leading-relaxed">
                    {i + 1}
                  </span>
                ))}
              </span>
              {/* Code column */}
              <span className="min-w-0 flex-1 overflow-hidden">
                {lines.map((line, i) => (
                  <span key={i} className="block leading-relaxed">
                    {line || '​' /* zero-width space preserves empty line height */}
                  </span>
                ))}
              </span>
            </code>
          ) : (
            <code
              className="block font-mono text-[--color-text]"
              data-language={language}
            >
              {code}
            </code>
          )}
        </pre>

        {/* Copy button */}
        {showCopyButton && (
          <button
            type="button"
            onClick={handleCopy}
            aria-label={copied ? '복사됨' : '코드 복사'}
            className={cn(
              'absolute right-[--space-2] top-[--space-2]',
              'rounded-[--radius-sm] px-[--space-2] py-[--space-1]',
              'text-[length:--text-xs] font-[number:--weight-medium]',
              'border border-[--color-border]',
              'bg-[--color-surface] text-[--color-text-muted]',
              'opacity-0 transition-opacity duration-[--duration-fast]',
              'group-hover:opacity-100 focus-visible:opacity-100',
              'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[--color-focus-ring]',
              copied && 'text-[--color-success]'
            )}
          >
            {copied ? '✓ 복사됨' : '복사'}
          </button>
        )}
      </div>

      {/* Screen reader live region for copy announcement */}
      <span
        ref={liveRef}
        role="status"
        aria-live="polite"
        aria-atomic="true"
        className="sr-only"
      />
    </div>
  )
}
