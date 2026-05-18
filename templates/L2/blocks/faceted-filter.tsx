/*
---
template_id: L2/blocks/faceted-filter
layer: L2
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "Algolia Faceted Search UX — facets expose item counts per value so users understand hit distribution before applying a filter"
    url: "https://www.algolia.com/doc/guides/managing-results/refine-results/faceting/"
    quoted_at: "2026-05-18"
  - source_type: internal
    rationale: "L2 data block — facet counts injected by server (L4 fetches aggregation); component is display-only, calls onSelect to propagate state upward."
dependencies: [badge]
imports_from: [L1]
imports_forbidden: [L4, app/, lib/]
---
*/
import * as React from 'react'

export interface FacetValue {
  value: string
  label: string
  /** Server-computed count of matching records */
  count: number
  /** If true, count is shown as ≥count (server capped) */
  capped?: boolean
}

export interface FacetDef {
  key: string
  label: string
  values: FacetValue[]
  /** Max visible before collapse */
  maxVisible?: number
}

export interface FacetedFilterProps {
  facets: FacetDef[]
  /** Currently active selections keyed by facet.key */
  activeValues?: Record<string, string[]>
  onSelect: (key: string, values: string[]) => void
}

/**
 * FacetedFilter — sidebar/panel facets with item counts.
 *
 * Counts are always server-provided — never computed client-side.
 * Each facet collapses after `maxVisible` entries (default 5) with a "Show more" toggle.
 *
 * L4 usage:
 *   const { facets } = useFacets({ query, filters })   // server aggregation
 *   <FacetedFilter facets={facets} activeValues={urlFilters} onSelect={updateUrlFilter} />
 */
export default function FacetedFilter({
  facets,
  activeValues = {},
  onSelect,
}: FacetedFilterProps) {
  const [expandedKeys, setExpandedKeys] = React.useState<Set<string>>(new Set())

  function toggleValue(facetKey: string, value: string) {
    const current = activeValues[facetKey] ?? []
    const next = current.includes(value)
      ? current.filter(v => v !== value)
      : [...current, value]
    onSelect(facetKey, next)
  }

  function toggleExpand(key: string) {
    setExpandedKeys(prev => {
      const next = new Set(prev)
      if (next.has(key)) next.delete(key)
      else next.add(key)
      return next
    })
  }

  return (
    <div className="flex flex-col gap-4">
      {facets.map(facet => {
        const maxVisible = facet.maxVisible ?? 5
        const isExpanded = expandedKeys.has(facet.key)
        const visibleValues = isExpanded
          ? facet.values
          : facet.values.slice(0, maxVisible)
        const hasMore = facet.values.length > maxVisible

        return (
          <fieldset key={facet.key} className="space-y-1.5">
            <legend className="mb-2 text-sm font-semibold text-foreground">
              {facet.label}
            </legend>

            {visibleValues.map(fv => {
              const active = (activeValues[facet.key] ?? []).includes(fv.value)
              return (
                <label
                  key={fv.value}
                  className="flex cursor-pointer items-center justify-between gap-2 rounded-sm px-1 py-0.5 text-sm hover:bg-accent"
                >
                  <span className="flex items-center gap-2">
                    <input
                      type="checkbox"
                      checked={active}
                      onChange={() => toggleValue(facet.key, fv.value)}
                      className="h-3.5 w-3.5 rounded border-border"
                    />
                    <span className={active ? 'font-medium text-foreground' : 'text-muted-foreground'}>
                      {fv.label}
                    </span>
                  </span>
                  <span
                    aria-label={`${fv.count}${fv.capped ? ' or more' : ''} results`}
                    className="rounded bg-muted px-1.5 py-0.5 text-xs text-muted-foreground"
                  >
                    {fv.capped ? `≥${fv.count}` : fv.count}
                  </span>
                </label>
              )
            })}

            {hasMore && (
              <button
                type="button"
                onClick={() => toggleExpand(facet.key)}
                className="mt-1 text-xs text-primary underline underline-offset-4 hover:text-primary/80"
              >
                {isExpanded
                  ? 'Show less'
                  : `Show ${facet.values.length - maxVisible} more`}
              </button>
            )}
          </fieldset>
        )
      })}
    </div>
  )
}
