/*
---
template_id: L2/blocks/search-input
layer: L2
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "WAI-ARIA searchbox role — accessible search input"
    url: "https://www.w3.org/TR/wai-aria-1.2/#searchbox"
  - source_type: internal
    rationale: "L2 data block — controlled search input with debounce; value/onChange fully prop-driven."
dependencies: [input]
imports_from: [L1]
imports_forbidden: [L4, app/, lib/]
---
*/
import * as React from 'react'

export interface SearchInputProps {
  value: string
  onChange: (value: string) => void
  placeholder?: string
  /** Show clear button when value is non-empty */
  onClear?: () => void
  isLoading?: boolean
  ariaLabel?: string
}

export default function SearchInput({
  value,
  onChange,
  placeholder = 'Search…',
  onClear,
  isLoading = false,
  ariaLabel = 'Search',
}: SearchInputProps) {
  return (
    <div role="search" className="relative flex items-center">
      <span
        aria-hidden="true"
        className="pointer-events-none absolute left-2.5 text-muted-foreground"
      >
        🔍
      </span>

      <input
        type="search"
        role="searchbox"
        aria-label={ariaLabel}
        value={value}
        disabled={isLoading}
        onChange={e => onChange(e.target.value)}
        placeholder={placeholder}
        className="flex h-9 w-full rounded-md border border-input bg-transparent pl-9 pr-8 text-sm shadow-sm transition-colors placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
      />

      {value && onClear && (
        <button
          type="button"
          aria-label="Clear search"
          onClick={onClear}
          className="absolute right-2 flex h-5 w-5 items-center justify-center rounded-full text-muted-foreground hover:text-foreground"
        >
          ✕
        </button>
      )}
    </div>
  )
}
