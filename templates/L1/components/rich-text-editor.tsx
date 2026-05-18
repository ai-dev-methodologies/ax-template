/*
---
template_id: L1/components/rich-text-editor
layer: L1
provenance_class: external_canonical
evidence:
  - source_type: upstream_id
    upstream_id: tiptap-2026-05
    section: useEditor
    quote: "useEditor is a custom React hook that creates and manages a Tiptap editor instance."
  - source_type: external
    citation: "Next.js 16 — Lazy Loading: ssr:false is not allowed with next/dynamic in Server Components. Move it into a Client Component."
    url: "https://nextjs.org/docs/app/guides/lazy-loading"
  - source_type: external
    citation: "TipTap v2 — Getting started with React: Add 'use client' to the component; the Editor is a browser-only construct."
    url: "https://tiptap.dev/docs/editor/getting-started/install/react"
a11y_criteria:
  - "WCAG 2.2 SC 4.1.2 — ProseMirror content area has role='textbox' aria-multiline='true'"
  - "WCAG 2.2 SC 1.4.3 — toolbar icon buttons carry aria-label"
  - "WCAG 2.2 SC 2.1.1 — keyboard accessible: bold Ctrl+B, italic Ctrl+I via Tiptap defaults"
dependencies:
  - "@tiptap/react@^2"
  - "@tiptap/core@^2"
  - "@tiptap/starter-kit@^2"
drift_snapshot_ref: "practices-react/upstream/tiptap-2026-05.snapshot.md#useEditor"
rsc_compat:
  status: compatible
  directive: "use client"
  strategy: |
    This file carries 'use client'. In RSC App Router pages, consume via next/dynamic:
      const RichTextEditor = dynamic(
        () => import('./rich-text-editor').then(m => ({ default: m.RichTextEditor })),
        { ssr: false, loading: () => <textarea className="w-full h-32 rounded border p-2" /> }
      )
    next/dynamic with ssr:false prevents SSR execution of browser-only ProseMirror.
  ssr_fallback: "<textarea> rendered by next/dynamic loading: prop during SSR"
observability:
  - "window.__axMetrics?.increment('form.rich_text.editor_mounted_count')"
---
*/

'use client'

import * as React from 'react'
import { useEditor, EditorContent } from '@tiptap/react'
import StarterKit from '@tiptap/starter-kit'
import { cn } from '../lib/utils'

// ─── Types ────────────────────────────────────────────────────────────────────

export interface RichTextEditorProps {
  /** Controlled HTML content */
  value?: string
  /** Called when content changes (returns HTML string) */
  onChange?: (html: string) => void
  /** Placeholder text shown when editor is empty */
  placeholder?: string
  /** Disables editing */
  disabled?: boolean
  /** Additional className for the editor container */
  className?: string
  /** Minimum height of the editor content area */
  minHeight?: string
}

// ─── Toolbar button ────────────────────────────────────────────────────────────

interface ToolbarButtonProps {
  onClick: () => void
  active?: boolean
  disabled?: boolean
  label: string
  children: React.ReactNode
}

function ToolbarButton({ onClick, active, disabled, label, children }: ToolbarButtonProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      aria-label={label}
      aria-pressed={active}
      className={cn(
        'inline-flex h-8 w-8 items-center justify-center rounded',
        'text-[length:--text-sm]',
        'transition-colors duration-[--duration-fast]',
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[--color-focus-ring]',
        'disabled:cursor-not-allowed disabled:opacity-40',
        active
          ? 'bg-[--color-accent-subtle] text-[--color-accent]'
          : 'text-[--color-text-muted] hover:bg-[--color-surface-subtle] hover:text-[--color-text]'
      )}
    >
      {children}
    </button>
  )
}

// ─── RichTextEditor ───────────────────────────────────────────────────────────

/**
 * RichTextEditor — TipTap WYSIWYG editor with design-token styling.
 *
 * **RSC compatibility (CRITICAL):** This file carries `'use client'`. In Next.js
 * App Router pages (Server Components), always consume via `next/dynamic`:
 *
 * ```tsx
 * import dynamic from 'next/dynamic'
 * const RichTextEditor = dynamic(
 *   () => import('@/components/rich-text-editor').then(m => ({ default: m.RichTextEditor })),
 *   { ssr: false, loading: () => <textarea className="w-full h-32 rounded border p-2" /> }
 * )
 * ```
 *
 * **Why next/dynamic with ssr:false?** TipTap's ProseMirror is a browser-only
 * construct. Even with `'use client'`, Next.js attempts SSR unless `ssr:false`
 * is set. The `loading:` fallback renders a plain `<textarea>` during SSR so
 * the page CLS budget is not harmed.
 *
 * **Observability:** fires `form.rich_text.editor_mounted_count` via
 * `window.__axMetrics` shim on mount.
 *
 * **Peer deps:** `@tiptap/react@^2`, `@tiptap/core@^2`, `@tiptap/starter-kit@^2`
 */
export function RichTextEditor({
  value = '',
  onChange,
  placeholder = '내용을 입력하세요…',
  disabled = false,
  className,
  minHeight = '8rem',
}: RichTextEditorProps) {
  // Mount-gate: prevents hydration mismatch.
  // ProseMirror builds DOM structure client-side that differs from SSR output.
  const [isMounted, setIsMounted] = React.useState(false)

  const editor = useEditor({
    extensions: [StarterKit],
    content: value,
    editable: !disabled,
    onUpdate({ editor: e }) {
      onChange?.(e.getHTML())
    },
    editorProps: {
      attributes: {
        'aria-multiline': 'true',
        role: 'textbox',
        'data-placeholder': placeholder,
        style: `min-height: ${minHeight}`,
        class: 'focus:outline-none px-3 py-2',
      },
    },
  })

  React.useEffect(() => {
    setIsMounted(true)

    // Observability shim
    if (typeof window !== 'undefined') {
      (window as Window & { __axMetrics?: { increment: (k: string) => void } })
        .__axMetrics?.increment('form.rich_text.editor_mounted_count')
    }
  }, [])

  // Sync external value changes (controlled mode)
  React.useEffect(() => {
    if (editor && !editor.isDestroyed && editor.getHTML() !== value) {
      editor.commands.setContent(value, false)
    }
  }, [editor, value])

  // Sync disabled state
  React.useEffect(() => {
    if (editor && !editor.isDestroyed) {
      editor.setEditable(!disabled)
    }
  }, [editor, disabled])

  // ── SSR / pre-mount fallback ─────────────────────────────────────────────
  // Renders a plain <textarea> until ProseMirror is client-ready.
  // When consumed via next/dynamic, the loading: prop renders this instead.
  if (!isMounted) {
    return (
      <textarea
        defaultValue={value}
        placeholder={placeholder}
        disabled={disabled}
        aria-label={placeholder}
        className={cn(
          'w-full rounded-[--radius-md] border border-[--color-border]',
          'bg-[--color-surface] p-[--space-3]',
          'text-[length:--text-sm] text-[--color-text]',
          'placeholder:text-[--color-text-placeholder]',
          'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[--color-focus-ring]',
          'disabled:cursor-not-allowed disabled:opacity-50',
          'resize-y',
          className
        )}
        style={{ minHeight }}
      />
    )
  }

  return (
    <div
      className={cn(
        'rounded-[--radius-md] border border-[--color-border]',
        'bg-[--color-surface]',
        'focus-within:ring-2 focus-within:ring-[--color-focus-ring]',
        disabled && 'cursor-not-allowed opacity-50',
        className
      )}
    >
      {/* Toolbar */}
      <div
        className="flex flex-wrap gap-[--space-1] border-b border-[--color-border] px-[--space-2] py-[--space-1]"
        role="toolbar"
        aria-label="텍스트 편집 도구"
      >
        <ToolbarButton
          onClick={() => editor?.chain().focus().toggleBold().run()}
          active={editor?.isActive('bold') ?? false}
          disabled={disabled}
          label="굵게 (Ctrl+B)"
        >
          <strong className="text-[length:--text-xs]">B</strong>
        </ToolbarButton>
        <ToolbarButton
          onClick={() => editor?.chain().focus().toggleItalic().run()}
          active={editor?.isActive('italic') ?? false}
          disabled={disabled}
          label="기울임 (Ctrl+I)"
        >
          <em className="text-[length:--text-xs]">I</em>
        </ToolbarButton>
        <ToolbarButton
          onClick={() => editor?.chain().focus().toggleStrike().run()}
          active={editor?.isActive('strike') ?? false}
          disabled={disabled}
          label="취소선"
        >
          <span className="text-[length:--text-xs] line-through">S</span>
        </ToolbarButton>
        <div className="mx-[--space-1] w-px bg-[--color-border]" aria-hidden="true" />
        <ToolbarButton
          onClick={() => editor?.chain().focus().toggleBulletList().run()}
          active={editor?.isActive('bulletList') ?? false}
          disabled={disabled}
          label="글머리 목록"
        >
          <span className="text-[length:--text-xs]">•—</span>
        </ToolbarButton>
        <ToolbarButton
          onClick={() => editor?.chain().focus().toggleOrderedList().run()}
          active={editor?.isActive('orderedList') ?? false}
          disabled={disabled}
          label="번호 목록"
        >
          <span className="text-[length:--text-xs]">1.</span>
        </ToolbarButton>
        <ToolbarButton
          onClick={() => editor?.chain().focus().toggleBlockquote().run()}
          active={editor?.isActive('blockquote') ?? false}
          disabled={disabled}
          label="인용구"
        >
          <span className="text-[length:--text-xs]">"</span>
        </ToolbarButton>
      </div>

      {/* Editor content area */}
      <EditorContent editor={editor} />
    </div>
  )
}
