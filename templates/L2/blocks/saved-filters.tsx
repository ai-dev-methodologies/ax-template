/*
---
template_id: L2/blocks/saved-filters
layer: L2
provenance_class: internal_design
evidence:
  - source_type: internal
    rationale: "L2 data block — named filter presets managed by the server (POST/DELETE /user-prefs/saved-filters); no localStorage. Emits onLoad/onDelete callbacks; L4 owns persistence via API or URL-param serialization."
dependencies: [button]
imports_from: [L1, L2]
imports_forbidden: [L4, app/, lib/]
---
*/
import * as React from 'react'

export interface SavedFilter {
  id: string
  name: string
  /** Serialized filter state (opaque to this component — stored by L4) */
  serialized: string
  createdAt?: string
}

export interface SavedFiltersProps {
  items: SavedFilter[]
  /** Currently active saved filter id, if any */
  activeId?: string | null
  onLoad: (filter: SavedFilter) => void
  onDelete: (id: string) => void
  /** If provided, renders a "Save current" button */
  onSaveCurrent?: (name: string) => void
  isLoading?: boolean
}

/**
 * SavedFilters — list of server-persisted named filter presets.
 *
 * Persistence is handled entirely by L4 (POST /user-prefs/saved-filters).
 * This component never touches localStorage.
 *
 * L4 usage:
 *   const { data: savedFilters } = useSavedFilters()
 *   <SavedFilters
 *     items={savedFilters}
 *     activeId={activeFilterId}
 *     onLoad={applyFilter}
 *     onDelete={deleteFilter}
 *     onSaveCurrent={saveCurrentFilter}
 *   />
 */
export default function SavedFilters({
  items,
  activeId,
  onLoad,
  onDelete,
  onSaveCurrent,
  isLoading = false,
}: SavedFiltersProps) {
  const [newName, setNewName] = React.useState('')
  const [showSaveForm, setShowSaveForm] = React.useState(false)

  function handleSave(e: React.FormEvent) {
    e.preventDefault()
    const trimmed = newName.trim()
    if (!trimmed || !onSaveCurrent) return
    onSaveCurrent(trimmed)
    setNewName('')
    setShowSaveForm(false)
  }

  return (
    <div className="flex flex-col gap-2">
      <div className="flex items-center justify-between">
        <span className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
          Saved filters
        </span>
        {onSaveCurrent && (
          <button
            type="button"
            onClick={() => setShowSaveForm(v => !v)}
            className="text-xs text-primary underline underline-offset-4 hover:text-primary/80"
          >
            {showSaveForm ? 'Cancel' : 'Save current'}
          </button>
        )}
      </div>

      {showSaveForm && onSaveCurrent && (
        <form onSubmit={handleSave} className="flex gap-2">
          <input
            type="text"
            value={newName}
            onChange={e => setNewName(e.target.value)}
            placeholder="Filter name…"
            autoFocus
            maxLength={60}
            className="flex-1 rounded-md border border-input bg-background px-2 py-1 text-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
          />
          <button
            type="submit"
            disabled={!newName.trim()}
            className="rounded-md bg-primary px-2 py-1 text-xs font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
          >
            Save
          </button>
        </form>
      )}

      {isLoading ? (
        <div className="py-4 text-center text-sm text-muted-foreground">Loading…</div>
      ) : items.length === 0 ? (
        <p className="text-sm text-muted-foreground">No saved filters yet.</p>
      ) : (
        <ul role="listbox" aria-label="Saved filters" className="space-y-1">
          {items.map(item => {
            const isActive = item.id === activeId
            return (
              <li key={item.id}>
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
                    onClick={() => onLoad(item)}
                    className="flex-1 text-left text-sm truncate focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring rounded"
                  >
                    {item.name}
                    {isActive && (
                      <span className="ml-1 text-xs text-primary" aria-label="(active)">
                        ●
                      </span>
                    )}
                  </button>
                  <button
                    type="button"
                    aria-label={`Delete saved filter: ${item.name}`}
                    onClick={() => onDelete(item.id)}
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
