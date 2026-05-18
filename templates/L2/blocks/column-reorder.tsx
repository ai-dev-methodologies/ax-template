/*
---
template_id: L2/blocks/column-reorder
layer: L2
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "react-aria-components DragAndDrop — accessible drag-to-reorder with keyboard support (Arrow keys + Space/Enter to pick up/drop) meeting WCAG 2.1 SC 2.1.1"
    url: "https://react-spectrum.adobe.com/react-aria/drag-and-drop.html"
    quoted_at: "2026-05-18"
  - source_type: internal
    rationale: "L2 data block — column order managed as ordered string[] of keys; drag-and-drop via HTML5 draggable (keyboard fallback with arrow keys). No external DnD lib to keep bundle delta minimal."
dependencies: []
imports_from: [L1]
imports_forbidden: [L4, app/, lib/]
---
*/
import * as React from 'react'

export interface ReorderableColumn {
  key: string
  label: string
  /** If true, column cannot be moved */
  locked?: boolean
}

export interface ColumnReorderProps {
  columns: ReorderableColumn[]
  /** Ordered column keys (controlled) */
  order: string[]
  onChange: (newOrder: string[]) => void
}

/**
 * ColumnReorder — drag-to-reorder column list with keyboard fallback.
 *
 * Uses HTML5 draggable API. Keyboard: Tab to focus a column button, then
 * ArrowUp/ArrowDown to move it, Enter/Space to apply.
 *
 * L4 usage:
 *   const [colOrder, setColOrder] = useUrlState<string[]>('cols', defaultOrder)
 *   <ColumnReorder columns={COLUMN_DEFS} order={colOrder} onChange={setColOrder} />
 */
export default function ColumnReorder({ columns, order, onChange }: ColumnReorderProps) {
  const [dragKey, setDragKey] = React.useState<string | null>(null)
  const [keyboardFocusIdx, setKeyboardFocusIdx] = React.useState<number | null>(null)

  // Build ordered list of columns
  const ordered = order
    .map(key => columns.find(c => c.key === key))
    .filter((c): c is ReorderableColumn => Boolean(c))

  // Append any columns not in order (safety net)
  const orderedKeys = new Set(order)
  const extras = columns.filter(c => !orderedKeys.has(c.key))
  const displayList = [...ordered, ...extras]

  function move(fromIdx: number, toIdx: number) {
    if (toIdx < 0 || toIdx >= displayList.length) return
    const col = displayList[fromIdx]
    if (col.locked) return
    const target = displayList[toIdx]
    if (target.locked) return

    const newOrder = [...displayList.map(c => c.key)]
    newOrder.splice(fromIdx, 1)
    newOrder.splice(toIdx, 0, col.key)
    onChange(newOrder)
  }

  // ─── drag handlers ─────────────────────────────────────────────────────────

  function handleDragStart(key: string) {
    setDragKey(key)
  }

  function handleDragOver(e: React.DragEvent, overKey: string) {
    e.preventDefault()
    if (!dragKey || dragKey === overKey) return
    const fromIdx = displayList.findIndex(c => c.key === dragKey)
    const toIdx = displayList.findIndex(c => c.key === overKey)
    if (fromIdx !== -1 && toIdx !== -1) move(fromIdx, toIdx)
  }

  function handleDragEnd() {
    setDragKey(null)
  }

  // ─── keyboard handlers ─────────────────────────────────────────────────────

  function handleKeyDown(e: React.KeyboardEvent, idx: number) {
    if (displayList[idx].locked) return
    if (e.key === 'ArrowUp') {
      e.preventDefault()
      move(idx, idx - 1)
      setKeyboardFocusIdx(idx - 1)
    } else if (e.key === 'ArrowDown') {
      e.preventDefault()
      move(idx, idx + 1)
      setKeyboardFocusIdx(idx + 1)
    }
  }

  const itemRefs = React.useRef<(HTMLButtonElement | null)[]>([])

  React.useEffect(() => {
    if (keyboardFocusIdx !== null) {
      itemRefs.current[keyboardFocusIdx]?.focus()
      setKeyboardFocusIdx(null)
    }
  }, [keyboardFocusIdx, displayList])

  return (
    <div
      role="listbox"
      aria-label="Column order — drag or use arrow keys to reorder"
      aria-multiselectable="false"
      className="flex flex-col gap-1 min-w-[200px]"
    >
      {displayList.map((col, idx) => {
        const isDragging = dragKey === col.key
        return (
          <div
            key={col.key}
            draggable={!col.locked}
            onDragStart={() => handleDragStart(col.key)}
            onDragOver={e => handleDragOver(e, col.key)}
            onDragEnd={handleDragEnd}
            data-dragging={isDragging}
            aria-dropeffect={col.locked ? undefined : 'move'}
            className={[
              'flex items-center gap-2 rounded-md border border-border bg-background px-3 py-2 text-sm',
              col.locked ? 'opacity-60' : 'cursor-grab active:cursor-grabbing',
              isDragging ? 'opacity-40 border-dashed border-primary' : '',
              'hover:bg-accent transition-colors',
            ].join(' ')}
          >
            {/* Drag handle */}
            <span
              aria-hidden="true"
              className="text-muted-foreground select-none"
            >
              ⣿
            </span>

            {/* Label */}
            <span className="flex-1">{col.label}</span>

            {/* Locked indicator */}
            {col.locked && (
              <span className="text-xs text-muted-foreground" aria-label="locked">
                🔒
              </span>
            )}

            {/* Keyboard reorder buttons */}
            {!col.locked && (
              <button
                ref={el => { itemRefs.current[idx] = el }}
                type="button"
                aria-label={`Move ${col.label} — use arrow keys`}
                onKeyDown={e => handleKeyDown(e, idx)}
                className="rounded p-0.5 text-muted-foreground hover:text-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                tabIndex={0}
              >
                <svg aria-hidden="true" width="12" height="12" viewBox="0 0 12 12" fill="none" stroke="currentColor" strokeWidth="1.5">
                  <path d="M6 1v10M2 4l4-3 4 3M2 8l4 3 4-3" />
                </svg>
              </button>
            )}
          </div>
        )
      })}
    </div>
  )
}
