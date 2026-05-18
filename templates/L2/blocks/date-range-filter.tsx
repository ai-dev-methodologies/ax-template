/*
---
template_id: L2/blocks/date-range-filter
layer: L2
provenance_class: internal_design
evidence:
  - source_type: internal
    rationale: "L2 data block — popover date-range picker for table filtering; emits ISO date strings; state owned by L4 (URL search params). Composes L1 calendar/popover."
dependencies: [date-range-picker, popover, button]
imports_from: [L1]
imports_forbidden: [L4, app/, lib/]
---
*/
import * as React from 'react'

/** ISO 8601 date string YYYY-MM-DD */
export type ISODateString = string

export interface DateRangeValue {
  from: ISODateString | null
  to: ISODateString | null
}

export interface DateRangeFilterProps {
  label?: string
  value: DateRangeValue
  /** Called when user confirms a new range; pass null values to clear. */
  onChange: (range: DateRangeValue) => void
  /** Earliest allowed date (ISO) */
  minDate?: ISODateString
  /** Latest allowed date (ISO) */
  maxDate?: ISODateString
}

function formatDisplay(range: DateRangeValue): string {
  if (!range.from && !range.to) return 'Any date'
  if (range.from && !range.to) return `From ${range.from}`
  if (!range.from && range.to) return `Until ${range.to}`
  return `${range.from} – ${range.to}`
}

/**
 * DateRangeFilter — popover date-range picker for table column filters.
 *
 * Emits ISO YYYY-MM-DD strings so L4 can store in URL params without a
 * date library on the consumer side. Clear via onChange({ from: null, to: null }).
 *
 * L4 usage:
 *   const [dateRange, setDateRange] = useUrlState<DateRangeValue>('dateRange', { from: null, to: null })
 *   <DateRangeFilter label="Created at" value={dateRange} onChange={setDateRange} />
 */
export default function DateRangeFilter({
  label = 'Date range',
  value,
  onChange,
  minDate,
  maxDate,
}: DateRangeFilterProps) {
  const [open, setOpen] = React.useState(false)
  const [draft, setDraft] = React.useState<DateRangeValue>(value)

  // Sync draft when external value changes (e.g. URL param reset)
  React.useEffect(() => {
    setDraft(value)
  }, [value.from, value.to])

  const hasValue = Boolean(value.from || value.to)

  function handleApply() {
    onChange(draft)
    setOpen(false)
  }

  function handleClear() {
    const empty: DateRangeValue = { from: null, to: null }
    setDraft(empty)
    onChange(empty)
    setOpen(false)
  }

  function handleFromChange(e: React.ChangeEvent<HTMLInputElement>) {
    setDraft(prev => ({ ...prev, from: e.target.value || null }))
  }

  function handleToChange(e: React.ChangeEvent<HTMLInputElement>) {
    setDraft(prev => ({ ...prev, to: e.target.value || null }))
  }

  return (
    <div className="relative inline-block">
      <button
        type="button"
        aria-haspopup="dialog"
        aria-expanded={open}
        aria-label={`${label}: ${formatDisplay(value)}`}
        onClick={() => setOpen(v => !v)}
        className={[
          'inline-flex items-center gap-1.5 rounded-md border px-3 py-1.5 text-sm font-medium transition-colors',
          'focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring',
          hasValue
            ? 'border-primary bg-primary/10 text-primary hover:bg-primary/20'
            : 'border-input bg-background text-foreground hover:bg-accent',
        ].join(' ')}
      >
        <svg
          aria-hidden="true"
          width="14"
          height="14"
          viewBox="0 0 14 14"
          fill="none"
          stroke="currentColor"
          strokeWidth="1.5"
        >
          <rect x="1" y="2" width="12" height="11" rx="1" />
          <path d="M1 5h12M5 2v2M9 2v2" />
        </svg>
        <span>{label}</span>
        {hasValue && (
          <span className="text-xs text-muted-foreground">
            {formatDisplay(value)}
          </span>
        )}
      </button>

      {open && (
        <div
          role="dialog"
          aria-label={`${label} picker`}
          className="absolute left-0 top-full z-50 mt-1 min-w-[220px] rounded-md border border-border bg-background p-4 shadow-md"
        >
          <fieldset className="space-y-3">
            <legend className="text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-2">
              {label}
            </legend>
            <label className="block">
              <span className="mb-1 block text-xs text-muted-foreground">From</span>
              <input
                type="date"
                value={draft.from ?? ''}
                min={minDate ?? undefined}
                max={draft.to ?? maxDate ?? undefined}
                onChange={handleFromChange}
                className="w-full rounded-md border border-input bg-background px-2 py-1 text-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
              />
            </label>
            <label className="block">
              <span className="mb-1 block text-xs text-muted-foreground">To</span>
              <input
                type="date"
                value={draft.to ?? ''}
                min={draft.from ?? minDate ?? undefined}
                max={maxDate ?? undefined}
                onChange={handleToChange}
                className="w-full rounded-md border border-input bg-background px-2 py-1 text-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
              />
            </label>
          </fieldset>

          <div className="mt-4 flex justify-between gap-2">
            <button
              type="button"
              onClick={handleClear}
              className="text-xs text-muted-foreground underline underline-offset-4 hover:text-foreground"
            >
              Clear
            </button>
            <button
              type="button"
              onClick={handleApply}
              className="rounded-md bg-primary px-3 py-1 text-xs font-medium text-primary-foreground hover:bg-primary/90 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
            >
              Apply
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
