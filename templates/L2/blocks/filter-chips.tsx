/*
---
template_id: L2/blocks/filter-chips
layer: L2
provenance_class: internal_design
evidence:
  - source_type: internal
    rationale: "L2 data block — renders active filter state as dismissible chips; each chip calls onRemove with its key+value. State owned by L4 (URL search params). Composes from filter-bar pattern."
dependencies: []
imports_from: [L1]
imports_forbidden: [L4, app/, lib/]
---
*/
import * as React from 'react'

export interface FilterChip {
  /** Filter group key (e.g. 'status', 'assignee') */
  key: string
  /** Human-readable label for the chip (e.g. 'Status: Active') */
  label: string
  value: string
}

export interface FilterChipsProps {
  chips: FilterChip[]
  /** Called when user clicks × on a chip; L4 removes from URL/state */
  onRemove: (key: string, value: string) => void
  /** Called when user clicks "Clear all" */
  onClearAll?: () => void
}

/**
 * FilterChips — displays active filters as a row of dismissible pill badges.
 *
 * Owned by L4; chips derived from URL search params or server filter state.
 * Renders nothing when chips array is empty.
 *
 * L4 usage:
 *   const chips = urlParams → [{ key:'status', value:'active', label:'Status: Active' }]
 *   <FilterChips chips={chips} onRemove={(key, val) => removeParam(key, val)} />
 */
export default function FilterChips({ chips, onRemove, onClearAll }: FilterChipsProps) {
  if (chips.length === 0) return null

  return (
    <div
      role="group"
      aria-label="Active filters"
      className="flex flex-wrap items-center gap-1.5"
    >
      {chips.map((chip, i) => (
        <span
          key={`${chip.key}-${chip.value}-${i}`}
          className="inline-flex items-center gap-1 rounded-full border border-border bg-secondary px-2.5 py-0.5 text-xs font-medium text-secondary-foreground"
        >
          {chip.label}
          <button
            type="button"
            aria-label={`Remove filter: ${chip.label}`}
            onClick={() => onRemove(chip.key, chip.value)}
            className="ml-0.5 rounded-full p-0.5 hover:bg-secondary-foreground/10 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
          >
            <svg
              aria-hidden="true"
              width="10"
              height="10"
              viewBox="0 0 10 10"
              fill="none"
              stroke="currentColor"
              strokeWidth="1.5"
            >
              <path d="M2 2l6 6M8 2l-6 6" />
            </svg>
          </button>
        </span>
      ))}

      {onClearAll && chips.length > 1 && (
        <button
          type="button"
          onClick={onClearAll}
          className="text-xs text-muted-foreground underline underline-offset-4 hover:text-foreground"
        >
          Clear all
        </button>
      )}
    </div>
  )
}
