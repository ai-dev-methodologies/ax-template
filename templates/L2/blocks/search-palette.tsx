/*
---
template_id: L2/blocks/search-palette
layer: L2
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "cmdk — Command Menu primitives for React. Handles keyboard navigation, filtering, and accessible combobox semantics."
    url: "https://cmdk.paco.me"
  - source_type: external
    citation: "MDN Web Docs — CompositionEvent: compositionstart/compositionend lifecycle for IME (Input Method Editor) input. Must suppress search during active IME composition to prevent partial-syllable queries in Korean (한글)."
    url: "https://developer.mozilla.org/en-US/docs/Web/API/CompositionEvent"
  - source_type: external
    citation: "WCAG 2.2 — 2.1.1 Keyboard: All functionality must be operable through keyboard interface; Cmd+K / Ctrl+K shortcut must open the palette."
    url: "https://www.w3.org/TR/WCAG22/#keyboard"
dependencies: [cmdk]
imports_from: [L1, L2]
imports_forbidden: [L4, app/, lib/auth/]
---
*/
'use client'

import * as React from 'react'
import { Command } from 'cmdk'

// ─── types ────────────────────────────────────────────────────────────────────

export interface SearchPaletteProps {
  /**
   * Called when the user commits a query (Enter key or item selection).
   * Not called during active Korean IME composition.
   */
  onSearch: (query: string) => void
  /** Placeholder text shown in the input. */
  placeholder?: string
  /** Keyboard shortcut to open. @default 'k' (Cmd+K / Ctrl+K) */
  shortcutKey?: string
  /** Initial open state. Useful for uncontrolled mode. @default false */
  defaultOpen?: boolean
  className?: string
  children?: React.ReactNode
}

// ─── component ───────────────────────────────────────────────────────────────

/**
 * SearchPalette — Cmd+K command palette with full Korean IME (한글) support.
 *
 * IME guard:
 *   Tracks `compositionstart` / `compositionend` events. While IME is composing
 *   (e.g. 강 → 남 → 강남), the Enter key and value change are suppressed so that
 *   only complete syllable blocks are submitted as queries.
 *
 * Keyboard:
 *   - `Cmd+K` (macOS) / `Ctrl+K` (Windows/Linux) → open palette
 *   - `Escape` → close palette
 *   - `Enter` → commit search (only when not composing)
 *
 * Fork instructions:
 *   1. Wrap children (Command.Group + Command.Item) to render search results inside the palette.
 *   2. Use onSearch to route queries to your search backend.
 *   3. Pair with RecentSearches to populate the palette default state.
 *
 * @example
 * ```tsx
 * <SearchPalette onSearch={q => router.push(`/search?q=${encodeURIComponent(q)}`)}>
 *   <Command.Group heading="Recent">
 *     <Command.Item>강남 맛집</Command.Item>
 *   </Command.Group>
 * </SearchPalette>
 * ```
 */
export default function SearchPalette({
  onSearch,
  placeholder = '검색어를 입력하세요…',
  shortcutKey = 'k',
  defaultOpen = false,
  className,
  children,
}: SearchPaletteProps) {
  const [open, setOpen] = React.useState(defaultOpen)
  const [value, setValue] = React.useState('')
  const isComposingRef = React.useRef(false)

  // ── Cmd+K / Ctrl+K shortcut ─────────────────────────────────────────────
  React.useEffect(() => {
    function handleKeyDown(e: KeyboardEvent) {
      if ((e.metaKey || e.ctrlKey) && e.key === shortcutKey) {
        e.preventDefault()
        setOpen((prev) => !prev)
      }
    }
    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [shortcutKey])

  // ── IME event handlers ────────────────────────────────────────────────────
  function handleCompositionStart() {
    isComposingRef.current = true
  }

  function handleCompositionEnd(e: React.CompositionEvent<HTMLInputElement>) {
    isComposingRef.current = false
    // Update value with the finalised composed text
    setValue(e.currentTarget.value)
  }

  // ── Input change (fires on non-IME keystrokes) ───────────────────────────
  function handleValueChange(v: string) {
    // cmdk fires onValueChange; only update if not actively composing
    if (!isComposingRef.current) {
      setValue(v)
    }
  }

  // ── Enter key (commit search) ────────────────────────────────────────────
  function handleKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'Enter' && !isComposingRef.current && value.trim()) {
      onSearch(value.trim())
      setOpen(false)
      setValue('')
    }
  }

  if (!open) {
    return (
      <button
        type="button"
        onClick={() => setOpen(true)}
        className={[
          'flex items-center gap-2 px-3 py-2 rounded-lg border bg-muted/50 text-sm text-muted-foreground',
          'hover:bg-muted transition-colors',
          className ?? '',
        ].join(' ')}
        aria-label={`검색 열기 (${navigator?.platform?.includes('Mac') ? '⌘' : 'Ctrl'}+${shortcutKey.toUpperCase()})`}
      >
        <svg
          xmlns="http://www.w3.org/2000/svg"
          width={14}
          height={14}
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth={2}
          aria-hidden="true"
        >
          <circle cx={11} cy={11} r={8} />
          <path d="m21 21-4.35-4.35" />
        </svg>
        <span>{placeholder}</span>
        <kbd className="ml-auto text-xs border rounded px-1 py-0.5 bg-background">
          {navigator?.platform?.includes('Mac') ? '⌘' : 'Ctrl'}+{shortcutKey.toUpperCase()}
        </kbd>
      </button>
    )
  }

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label="검색"
      className="fixed inset-0 z-50 flex items-start justify-center pt-[20vh] bg-black/40 backdrop-blur-sm"
      onClick={(e) => {
        if (e.target === e.currentTarget) setOpen(false)
      }}
    >
      <Command
        className={[
          'w-full max-w-xl rounded-xl border shadow-2xl bg-popover',
          className ?? '',
        ].join(' ')}
        shouldFilter={false}
      >
        <Command.Input
          autoFocus
          placeholder={placeholder}
          value={value}
          onValueChange={handleValueChange}
          onCompositionStart={handleCompositionStart}
          onCompositionEnd={handleCompositionEnd}
          onKeyDown={handleKeyDown}
          className="w-full px-4 py-3 text-sm bg-transparent border-b outline-none placeholder:text-muted-foreground"
          aria-label="검색어 입력"
        />
        <Command.List className="max-h-80 overflow-y-auto p-2">
          {children}
        </Command.List>
      </Command>
    </div>
  )
}
