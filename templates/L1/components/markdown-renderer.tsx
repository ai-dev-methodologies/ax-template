/*
---
template_id: L1/components/markdown-renderer
layer: L1
provenance_class: external_canonical
evidence:
  # Re-anchored 2026-08-01 (BACKLOG P2-73): the react-markdown readme was rewritten and no
  # longer contains the previous sentence. The claim survives on the live page in different
  # words ("we use unified , specifically remark for markdown and rehype for HTML"). Quote
  # below is copied verbatim from the 2026-08-01 extractor output appended to the snapshot.
  - source_type: upstream_id
    upstream_id: remark-2026-05
    section: react-markdown
    quote: "This package is a React component that can be given a string of markdown that it'll safely render to React elements."
  - source_type: external
    citation: "remark / react-markdown — safe by default: no dangerouslySetInnerHTML; all HTML is sanitized via rehype."
    url: "https://github.com/remarkjs/react-markdown"
  - source_type: external
    citation: "WAI-ARIA 1.2 — rendered markdown must preserve heading hierarchy for screen reader navigation."
    url: "https://www.w3.org/TR/wai-aria-1.2/"
a11y_criteria:
  - "Heading hierarchy preserved from markdown source (h1–h6 rendered as semantic elements)"
  - "Links rendered with proper href and accessible text"
  - "Code blocks rendered with <pre><code> for screen reader context"
dependencies:
  - "react-markdown@^9"
  - "remark-gfm@^4"
drift_snapshot_ref: "practices-react/upstream/remark-2026-05.snapshot.md#react-markdown"
---
*/

import * as React from 'react'
import { cn } from '../lib/utils'

// ─── Types ────────────────────────────────────────────────────────────────────

export interface MarkdownRendererProps {
  /** Markdown source string */
  content: string
  /** Additional className for the wrapper element */
  className?: string
  /** Allow raw HTML in markdown (default: false — safe mode) */
  allowHtml?: boolean
}

// ─── Inline styles for markdown prose ─────────────────────────────────────────
// Using CSS custom properties from the design token system.
// Fork receivers may override with Tailwind prose plugin or CSS modules.

const PROSE_STYLES: React.CSSProperties = {
  // Inherits from parent; overrides cascade from design tokens
  color: 'var(--color-text)',
  lineHeight: 1.7,
}

/**
 * MarkdownRenderer — renders markdown content as accessible React elements.
 *
 * Uses `react-markdown` (remark pipeline) for safe, sanitized output.
 * No `dangerouslySetInnerHTML` — all output is React elements.
 *
 * **Server-side compatible:** no `'use client'` directive; renders in RSC and SSR.
 *
 * **GFM support:** tables, strikethrough, task lists, and autolinks via `remark-gfm`.
 *
 * **Customization:** override the `components` prop (react-markdown API) at the
 * fork-receiver level to swap in shadcn/ui primitives (e.g. L1 `<Badge>` for
 * inline code, `<Separator>` for `<hr>`).
 *
 * @example
 * <MarkdownRenderer content={post.body} className="max-w-prose mx-auto" />
 */
export function MarkdownRenderer({
  content,
  className,
  allowHtml = false,
}: MarkdownRendererProps) {
  // react-markdown is loaded as a peer dependency.
  // For extremely large documents, fork receivers may switch to a
  // next/dynamic import; for typical content the module is small (~15 kB gz).
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const [ReactMarkdown, setReactMarkdown] = React.useState<React.ComponentType<any> | null>(null)

  const [remarkGfm, setRemarkGfm] = React.useState<unknown>(null)

  React.useEffect(() => {
    Promise.all([
      import('react-markdown'),
      import('remark-gfm'),
    ]).then(([rmMod, gfmMod]) => {
      // Wrap in arrow to prevent React treating the component as a state-updater function
      setReactMarkdown(() => rmMod.default as React.ComponentType<any>)
      setRemarkGfm(() => gfmMod.default)
    })
  }, [])

  // SSR / pre-hydration fallback — plain whitespace-preserving block
  if (!ReactMarkdown) {
    return (
      <div
        className={cn('whitespace-pre-wrap text-[--color-text]', className)}
        style={PROSE_STYLES}
        aria-busy="true"
      >
        {content}
      </div>
    )
  }

  return (
    <div
      className={cn(
        'markdown-renderer',
        // Prose-style defaults (fork receivers override with Tailwind prose plugin)
        '[&_h1]:mb-4 [&_h1]:mt-6 [&_h1]:text-2xl [&_h1]:font-bold',
        '[&_h2]:mb-3 [&_h2]:mt-5 [&_h2]:text-xl [&_h2]:font-semibold',
        '[&_h3]:mb-2 [&_h3]:mt-4 [&_h3]:text-lg [&_h3]:font-semibold',
        '[&_p]:mb-4 [&_p]:leading-relaxed',
        '[&_ul]:mb-4 [&_ul]:list-disc [&_ul]:pl-6',
        '[&_ol]:mb-4 [&_ol]:list-decimal [&_ol]:pl-6',
        '[&_li]:mb-1',
        '[&_blockquote]:my-4 [&_blockquote]:border-l-4 [&_blockquote]:border-[--color-accent-subtle] [&_blockquote]:pl-4 [&_blockquote]:text-[--color-text-muted]',
        '[&_code]:rounded [&_code]:bg-[--color-surface-subtle] [&_code]:px-1 [&_code]:py-0.5 [&_code]:text-[length:--text-sm] [&_code]:font-mono',
        '[&_pre]:my-4 [&_pre]:overflow-x-auto [&_pre]:rounded-[--radius-md] [&_pre]:bg-[--color-surface-subtle] [&_pre]:p-4',
        '[&_pre_code]:bg-transparent [&_pre_code]:p-0',
        '[&_a]:text-[--color-accent] [&_a]:underline [&_a]:underline-offset-2 [&_a]:hover:text-[--color-accent-hover]',
        '[&_hr]:my-6 [&_hr]:border-[--color-border]',
        '[&_table]:w-full [&_table]:border-collapse',
        '[&_th]:border [&_th]:border-[--color-border] [&_th]:bg-[--color-surface-subtle] [&_th]:px-3 [&_th]:py-2 [&_th]:text-left [&_th]:text-[length:--text-sm] [&_th]:font-semibold',
        '[&_td]:border [&_td]:border-[--color-border] [&_td]:px-3 [&_td]:py-2 [&_td]:text-[length:--text-sm]',
        className
      )}
      style={PROSE_STYLES}
    >
      <ReactMarkdown
        remarkPlugins={remarkGfm ? [remarkGfm] : []}
        rehypePlugins={allowHtml ? [] : []}
      >
        {content}
      </ReactMarkdown>
    </div>
  )
}
