/*
---
template_id: L2/blocks/recent-searches
layer: L2
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "MDN Web Docs — Window.localStorage: Synchronous key-value storage persisted across sessions. Used to store and retrieve recent search history. Falls back gracefully when localStorage is unavailable (e.g. Safari private mode)."
    url: "https://developer.mozilla.org/en-US/docs/Web/API/Window/localStorage"
  - source_type: external
    citation: "WCAG 2.2 — 2.4.3 Focus Order: If a Web page can be navigated sequentially and navigation sequences affect meaning, focusable components must receive focus in an order that preserves meaning and operability."
    url: "https://www.w3.org/TR/WCAG22/#focus-order"
imports_from: [L1, L2]
imports_forbidden: [L4, app/, lib/auth/]
---
*/
'use client'

import * as React from 'react'

// ─── types ────────────────────────────────────────────────────────────────────

const STORAGE_KEY = 'ax:recent-searches'
const MAX_ITEMS = 10

export interface RecentSearchesProps {
  /** Called when the user clicks a recent search item. */
  onSelect: (query: string) => void
  /** Maximum items stored. @default 10 */
  maxItems?: number
  /** localStorage key for storage isolation. @default 'ax:recent-searches' */
  storageKey?: string
  className?: string
}

// ─── localStorage helpers ─────────────────────────────────────────────────────

function readStorage(key: string): string[] {
  try {
    const raw = localStorage.getItem(key)
    if (!raw) return []
    const parsed: unknown = JSON.parse(raw)
    if (Array.isArray(parsed) && parsed.every((v) => typeof v === 'string')) {
      return parsed as string[]
    }
    return []
  } catch {
    return []
  }
}

function writeStorage(key: string, items: string[]): void {
  try {
    localStorage.setItem(key, JSON.stringify(items))
  } catch {
    // localStorage unavailable (Safari private mode, storage quota, etc.)
  }
}

// ─── hook ─────────────────────────────────────────────────────────────────────

/**
 * Manages recent search history in localStorage.
 * Exported so it can be used independently from the UI component.
 */
export function useRecentSearches(storageKey = STORAGE_KEY, maxItems = MAX_ITEMS) {
  const [items, setItems] = React.useState<string[]>([])

  // Read from localStorage on mount (client only)
  React.useEffect(() => {
    setItems(readStorage(storageKey))
  }, [storageKey])

  function add(query: string): void {
    const trimmed = query.trim()
    if (!trimmed) return
    setItems((prev) => {
      // Deduplicate: remove existing occurrence, prepend new
      const deduped = [trimmed, ...prev.filter((q) => q !== trimmed)].slice(0, maxItems)
      writeStorage(storageKey, deduped)
      return deduped
    })
  }

  function remove(query: string): void {
    setItems((prev) => {
      const next = prev.filter((q) => q !== query)
      writeStorage(storageKey, next)
      return next
    })
  }

  function clear(): void {
    setItems([])
    writeStorage(storageKey, [])
  }

  return { items, add, remove, clear }
}

// ─── component ───────────────────────────────────────────────────────────────

/**
 * RecentSearches — displays and manages a list of recent search queries.
 *
 * Storage:
 *   - Persists to `localStorage` under `storageKey`.
 *   - Capped at `maxItems` (default 10) entries.
 *   - Deduplicates: re-searching an existing term moves it to the top.
 *   - Gracefully handles localStorage unavailability (Safari private mode).
 *
 * Fork instructions:
 *   1. Call `useRecentSearches()` hook separately to `add()` items on search.
 *   2. Use `storageKey` to namespace if multiple search contexts exist on one page.
 *   3. Render inside SearchPalette's default (empty query) state.
 *
 * @example
 * ```tsx
 * const recent = useRecentSearches()
 *
 * // When user submits a search:
 * function handleSearch(q: string) {
 *   recent.add(q)
 *   router.push(`/search?q=${encodeURIComponent(q)}`)
 * }
 *
 * // In the palette:
 * <RecentSearches onSelect={handleSearch} storageKey="ax:recent-searches" />
 * ```
 */
export default function RecentSearches({
  onSelect,
  maxItems = MAX_ITEMS,
  storageKey = STORAGE_KEY,
  className,
}: RecentSearchesProps) {
  const { items, remove, clear } = useRecentSearches(storageKey, maxItems)

  if (items.length === 0) return null

  return (
    <section className={['text-sm', className ?? ''].join(' ')} aria-label="최근 검색어">
      <div className="flex items-center justify-between px-3 py-1.5">
        <span className="text-xs font-medium text-muted-foreground uppercase tracking-wide">
          최근 검색
        </span>
        <button
          type="button"
          onClick={clear}
          className="text-xs text-muted-foreground hover:text-foreground"
          aria-label="최근 검색 전체 삭제"
        >
          전체 삭제
        </button>
      </div>

      <ul role="list">
        {items.map((query) => (
          <li key={query} className="flex items-center group">
            <button
              type="button"
              onClick={() => onSelect(query)}
              className="flex items-center gap-2 flex-1 px-3 py-2 rounded-lg hover:bg-accent text-left"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                width={14}
                height={14}
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth={2}
                className="text-muted-foreground shrink-0"
                aria-hidden="true"
              >
                <circle cx={12} cy={12} r={10} />
                <polyline points="12 6 12 12 16 14" />
              </svg>
              <span className="truncate">{query}</span>
            </button>

            <button
              type="button"
              onClick={() => remove(query)}
              className="px-2 py-2 text-muted-foreground opacity-0 group-hover:opacity-100 hover:text-foreground transition-opacity"
              aria-label={`'${query}' 삭제`}
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
                <path d="M18 6 6 18M6 6l12 12" />
              </svg>
            </button>
          </li>
        ))}
      </ul>
    </section>
  )
}
