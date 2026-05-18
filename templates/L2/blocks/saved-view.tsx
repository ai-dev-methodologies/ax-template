/*
---
template_id: L2/blocks/saved-view
layer: L2
provenance_class: internal_design
evidence:
  - source_type: internal
    rationale: "L2 data block — named table views (columns + sort + filter) persisted via URL state or server API. localStorage is FORBIDDEN per rule saved-view-must-be-url-state-or-server-persisted."
  - source_type: external
    citation: "URL as state — Vercel/Next.js patterns: serialize view config into URL search params so views are bookmarkable and shareable without a backend round-trip"
    url: "https://nextjs.org/docs/app/api-reference/functions/use-search-params"
    quoted_at: "2026-05-18"
dependencies: [button]
imports_from: [L1]
imports_forbidden: [L4, app/, lib/]
---
*/
import * as React from 'react'

/** Allowed persistence modes — localStorage is explicitly excluded */
export type SavedViewPersistence = 'url' | 'server'

export interface SavedViewConfig {
  /** Ordered visible column keys */
  columns: string[]
  /** Active sort field + direction */
  sort?: { field: string; direction: 'asc' | 'desc' }
  /** Serialized filter state (opaque) */
  filter?: string
}

export interface SavedViewItem {
  id: string
  name: string
  config: SavedViewConfig
  /** How this view is persisted — must be 'url' or 'server', never 'localStorage' */
  persistence: SavedViewPersistence
  createdAt?: string
}

export interface SavedViewProps {
  /** Available saved views */
  items: SavedViewItem[]
  /** Currently active view id */
  activeId?: string | null
  /** Current live table config (used for "Save current view") */
  currentConfig?: SavedViewConfig
  onLoad: (view: SavedViewItem) => void
  onDelete: (id: string) => void
  /** L4 persists via URL update or POST /user-prefs/saved-views */
  onSave: (name: string, config: SavedViewConfig, persistence: SavedViewPersistence) => void
  isLoading?: boolean
}

/**
 * SavedView — manage named table views (column layout + sort + filters).
 *
 * Persistence mode must be 'url' or 'server'. localStorage is BANNED
 * (rule: saved-view-must-be-url-state-or-server-persisted).
 *
 * 'url'    → L4 serializes config into URL search params (shareable, no server round-trip)
 * 'server' → L4 calls POST /user-prefs/saved-views (survives browser sessions)
 *
 * L4 usage:
 *   <SavedView
 *     items={serverViews}
 *     currentConfig={currentTableConfig}
 *     onLoad={loadView}
 *     onDelete={deleteView}
 *     onSave={(name, config, mode) => mode === 'url' ? shareUrl(config) : postView(name, config)}
 *   />
 */
export default function SavedView({
  items,
  activeId,
  currentConfig,
  onLoad,
  onDelete,
  onSave,
  isLoading = false,
}: SavedViewProps) {
  const [showForm, setShowForm] = React.useState(false)
  const [name, setName] = React.useState('')
  const [persistence, setPersistence] = React.useState<SavedViewPersistence>('url')

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    const trimmed = name.trim()
    if (!trimmed || !currentConfig) return
    onSave(trimmed, currentConfig, persistence)
    setName('')
    setShowForm(false)
  }

  return (
    <div className="flex flex-col gap-3 min-w-[220px]">
      <div className="flex items-center justify-between">
        <span className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
          Saved views
        </span>
        {currentConfig && (
          <button
            type="button"
            onClick={() => setShowForm(v => !v)}
            className="text-xs text-primary underline underline-offset-4 hover:text-primary/80"
          >
            {showForm ? 'Cancel' : 'Save view'}
          </button>
        )}
      </div>

      {showForm && currentConfig && (
        <form onSubmit={handleSubmit} className="space-y-2">
          <input
            type="text"
            value={name}
            onChange={e => setName(e.target.value)}
            placeholder="View name…"
            autoFocus
            maxLength={60}
            className="w-full rounded-md border border-input bg-background px-2 py-1 text-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
          />
          <fieldset className="flex gap-3">
            <legend className="sr-only">Persistence mode</legend>
            <label className="flex items-center gap-1.5 text-xs cursor-pointer">
              <input
                type="radio"
                name="persistence"
                value="url"
                checked={persistence === 'url'}
                onChange={() => setPersistence('url')}
                className="h-3 w-3"
              />
              URL (shareable)
            </label>
            <label className="flex items-center gap-1.5 text-xs cursor-pointer">
              <input
                type="radio"
                name="persistence"
                value="server"
                checked={persistence === 'server'}
                onChange={() => setPersistence('server')}
                className="h-3 w-3"
              />
              Server (personal)
            </label>
          </fieldset>
          <button
            type="submit"
            disabled={!name.trim()}
            className="w-full rounded-md bg-primary py-1 text-xs font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
          >
            Save
          </button>
        </form>
      )}

      {isLoading ? (
        <div className="py-4 text-center text-sm text-muted-foreground">Loading…</div>
      ) : items.length === 0 ? (
        <p className="text-sm text-muted-foreground">No saved views yet.</p>
      ) : (
        <ul role="listbox" aria-label="Saved views" className="space-y-1">
          {items.map(view => {
            const isActive = view.id === activeId
            return (
              <li key={view.id}>
                <div
                  className={[
                    'flex items-center justify-between gap-2 rounded-md px-2 py-1.5',
                    isActive ? 'bg-primary/10' : 'hover:bg-accent',
                  ].join(' ')}
                >
                  <button
                    type="button"
                    role="option"
                    aria-selected={isActive}
                    onClick={() => onLoad(view)}
                    className="flex-1 text-left truncate focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring rounded"
                  >
                    <span className="text-sm">{view.name}</span>
                    <span
                      className="ml-1.5 text-xs text-muted-foreground"
                      aria-label={`persistence: ${view.persistence}`}
                    >
                      {view.persistence === 'url' ? '🔗' : '☁️'}
                    </span>
                    {isActive && (
                      <span className="ml-1 text-xs text-primary" aria-label="(active)">●</span>
                    )}
                  </button>
                  <button
                    type="button"
                    aria-label={`Delete saved view: ${view.name}`}
                    onClick={() => onDelete(view.id)}
                    className="rounded p-1 text-muted-foreground hover:bg-destructive/10 hover:text-destructive focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                  >
                    <svg aria-hidden="true" width="12" height="12" viewBox="0 0 12 12" fill="none" stroke="currentColor" strokeWidth="1.5">
                      <path d="M1 1l10 10M11 1L1 11" />
                    </svg>
                  </button>
                </div>
              </li>
            )
          })}
        </ul>
      )}
    </div>
  )
}
