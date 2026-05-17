/*
---
template_id: L2/blocks/filter-bar
layer: L2
provenance_class: internal_design
evidence:
  - source_type: internal
    rationale: "L2 data block — renders filter chips from a filter schema; state propagated up via onFilter callback."
dependencies: [button, badge]
imports_from: [L1]
imports_forbidden: [L4, app/, lib/]
---
*/
import * as React from 'react'

export interface FilterOption {
  value: string
  label: string
}

export interface FilterDef {
  key: string
  label: string
  options: FilterOption[]
}

export interface FilterBarProps {
  filters: FilterDef[]
  /** Current active values keyed by filter.key */
  activeValues?: Record<string, string[]>
  onFilter: (key: string, values: string[]) => void
  onClearAll?: () => void
}

export default function FilterBar({
  filters,
  activeValues = {},
  onFilter,
  onClearAll,
}: FilterBarProps) {
  const hasActive = Object.values(activeValues).some(v => v.length > 0)

  function toggle(key: string, value: string) {
    const current = activeValues[key] ?? []
    const next = current.includes(value)
      ? current.filter(v => v !== value)
      : [...current, value]
    onFilter(key, next)
  }

  return (
    <div role="group" aria-label="Filters" className="flex flex-wrap items-center gap-2">
      {filters.map(filter => (
        <div key={filter.key} className="flex items-center gap-1">
          <span className="text-xs font-medium text-muted-foreground mr-1">
            {filter.label}:
          </span>
          {filter.options.map(opt => {
            const active = (activeValues[filter.key] ?? []).includes(opt.value)
            return (
              <button
                key={opt.value}
                type="button"
                aria-pressed={active}
                onClick={() => toggle(filter.key, opt.value)}
                className={[
                  'inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold transition-colors',
                  active
                    ? 'border-transparent bg-primary text-primary-foreground hover:bg-primary/80'
                    : 'border-transparent bg-secondary text-secondary-foreground hover:bg-secondary/80',
                ].join(' ')}
              >
                {opt.label}
              </button>
            )
          })}
        </div>
      ))}

      {hasActive && onClearAll && (
        <button
          type="button"
          onClick={onClearAll}
          className="text-xs text-muted-foreground underline underline-offset-4 hover:text-foreground ml-2"
        >
          Clear all
        </button>
      )}
    </div>
  )
}
