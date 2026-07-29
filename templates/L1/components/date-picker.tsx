/*
---
template_id: L1/components/date-picker
layer: L1
provenance_class: external_canonical
evidence:
  - source_type: upstream_id
    upstream_id: shadcn-ui-2026-05
    section: date-picker
    quote: "A date picker component with range and presets. Built using the Popover and the Calendar components."
  - source_type: external
    citation: "WCAG 2.2 SC 2.4.11 Focus Not Obscured (Minimum) (Level AA) — full normative text, W3C Recommendation 2023-10-05"
    url: "https://www.w3.org/TR/WCAG22/#focus-not-obscured-minimum"
    quote: "When a user interface component receives keyboard focus, the component is not entirely hidden due to author-created content."
    quoted_at: "2026-07-29"
a11y_criteria:
  - "WCAG 2.2 SC 2.4.11 Focus Appearance — trigger button has visible focus ring"
  - "WCAG 2.2 SC 4.1.2 — trigger button aria-label includes selected date or placeholder"
  - "WCAG 2.2 SC 1.3.1 — calendar grid aria announced via react-day-picker"
dependencies: ["react-day-picker@^9", "@radix-ui/react-popover"]
drift_snapshot_ref: "practices-react/upstream/shadcn-registry-2026-05.snapshot.md#date-picker"
---
*/
import * as React from 'react'
import { format } from 'date-fns'
import { ko } from 'date-fns/locale'
import { CalendarIcon } from 'lucide-react'
import { cn } from '../lib/utils'
import { Button } from './button'
import { Calendar } from './calendar'
import { Popover, PopoverContent, PopoverTrigger } from './popover'

export interface DatePickerProps {
  /** Currently selected date */
  value?: Date
  /** Called when a date is selected */
  onSelect?: (date: Date | undefined) => void
  /** Placeholder text when no date selected */
  placeholder?: string
  /** Disable dates before this date */
  fromDate?: Date
  /** Disable dates after this date */
  toDate?: Date
  /** Disables the input */
  disabled?: boolean
  /** Additional className for the trigger button */
  className?: string
  /** Date format string (date-fns format) — default: "yyyy년 MM월 dd일" */
  dateFormat?: string
}

/**
 * DatePicker — Calendar + Popover composition for single date selection.
 *
 * Locale defaults to Korean (`ko`). Override via `dateFormat` prop.
 */
export function DatePicker({
  value,
  onSelect,
  placeholder = '날짜를 선택하세요',
  fromDate,
  toDate,
  disabled = false,
  className,
  dateFormat = 'yyyy년 MM월 dd일',
}: DatePickerProps) {
  const [open, setOpen] = React.useState(false)

  const handleSelect = React.useCallback(
    (date: Date | undefined) => {
      onSelect?.(date)
      setOpen(false)
    },
    [onSelect]
  )

  const label = value ? format(value, dateFormat, { locale: ko }) : placeholder

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          variant="outline"
          aria-label={label}
          disabled={disabled}
          className={cn(
            'w-full justify-start text-left font-normal',
            !value && 'text-[--color-text-placeholder]',
            className
          )}
        >
          <CalendarIcon className="mr-[--space-2] h-4 w-4" aria-hidden="true" />
          {label}
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-auto p-0" align="start">
        <Calendar
          mode="single"
          selected={value}
          onSelect={handleSelect}
          fromDate={fromDate}
          toDate={toDate}
          initialFocus
        />
      </PopoverContent>
    </Popover>
  )
}
