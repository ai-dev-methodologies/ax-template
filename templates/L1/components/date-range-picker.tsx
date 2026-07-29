/*
---
template_id: L1/components/date-range-picker
layer: L1
provenance_class: external_canonical
evidence:
  - source_type: upstream_id
    upstream_id: shadcn-ui-2026-05
    section: date-picker
    quote: "A date picker component with range and presets. Built using the Popover and the Calendar components."
  - source_type: external
    citation: "WCAG 2.2 SC 2.1.1 Keyboard (Level A) — full normative text, W3C Recommendation 2023-10-05"
    url: "https://www.w3.org/TR/WCAG22/#keyboard"
    quote: "All functionality of the content is operable through a keyboard interface without requiring specific timings for individual keystrokes, except where the underlying function requires input that depends on the path of the user's movement and not just the endpoints."
    quoted_at: "2026-07-29"
a11y_criteria:
  - "WCAG 2.2 SC 2.1.1 — keyboard navigation through start/end date selection"
  - "WCAG 2.2 SC 4.1.2 — aria-label reflects selected range or placeholder"
  - "WCAG 2.2 SC 1.3.1 — from/to dates are programmatically determinable"
dependencies: ["react-day-picker@^9", "@radix-ui/react-popover", "date-fns"]
drift_snapshot_ref: "practices-react/upstream/shadcn-registry-2026-05.snapshot.md#date-range-picker"
---
*/
import * as React from 'react'
import { format } from 'date-fns'
import { ko } from 'date-fns/locale'
import { CalendarIcon } from 'lucide-react'
import { type DateRange } from 'react-day-picker'
import { cn } from '../lib/utils'
import { Button } from './button'
import { Calendar } from './calendar'
import { Popover, PopoverContent, PopoverTrigger } from './popover'

export type { DateRange }

export interface DateRangePickerProps {
  /** Currently selected date range */
  value?: DateRange
  /** Called when the range changes */
  onSelect?: (range: DateRange | undefined) => void
  /** Number of months to show side by side — default 2 */
  numberOfMonths?: number
  /** Placeholder text when no range selected */
  placeholder?: string
  /** Disables the input */
  disabled?: boolean
  /** Additional className for the trigger button */
  className?: string
  /** Date format string (date-fns format) — default: "yyyy.MM.dd" */
  dateFormat?: string
}

/**
 * DateRangePicker — Calendar + Popover composition for date range selection.
 *
 * Displays two months side-by-side by default (numberOfMonths=2).
 * Locale defaults to Korean (`ko`).
 */
export function DateRangePicker({
  value,
  onSelect,
  numberOfMonths = 2,
  placeholder = '기간을 선택하세요',
  disabled = false,
  className,
  dateFormat = 'yyyy.MM.dd',
}: DateRangePickerProps) {
  const [open, setOpen] = React.useState(false)

  const label = React.useMemo(() => {
    if (!value?.from) return placeholder
    const from = format(value.from, dateFormat, { locale: ko })
    if (!value.to) return from
    const to = format(value.to, dateFormat, { locale: ko })
    return `${from} ~ ${to}`
  }, [value, dateFormat, placeholder])

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          variant="outline"
          aria-label={label}
          disabled={disabled}
          className={cn(
            'w-full justify-start text-left font-normal',
            !value?.from && 'text-[--color-text-placeholder]',
            className
          )}
        >
          <CalendarIcon className="mr-[--space-2] h-4 w-4" aria-hidden="true" />
          {label}
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-auto p-0" align="start">
        <Calendar
          initialFocus
          mode="range"
          defaultMonth={value?.from}
          selected={value}
          onSelect={onSelect}
          numberOfMonths={numberOfMonths}
        />
      </PopoverContent>
    </Popover>
  )
}
