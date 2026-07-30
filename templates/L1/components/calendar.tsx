/*
---
template_id: L1/components/calendar
layer: L1
provenance_class: external_canonical
evidence:
  - source_type: upstream_id
    upstream_id: shadcn-ui-2026-05
    section: calendar
    quote: "A calendar component that allows users to select a date or a range of dates."
  - source_type: external
    citation: "WCAG 2.2 SC 1.3.1 Info and Relationships (Level A) — full normative text, W3C Recommendation 2023-10-05"
    url: "https://www.w3.org/TR/WCAG22/#info-and-relationships"
    quote: "Information, structure, and relationships conveyed through presentation can be programmatically determined or are available in text."
    quoted_at: "2026-07-29"
a11y_criteria:
  - "WCAG 2.2 SC 1.3.1 — role='grid' with aria-label, aria-selected on day buttons via react-day-picker"
  - "WCAG 2.2 SC 2.1.1 — keyboard navigation: Arrow keys move between dates, Enter selects"
  - "WCAG 2.2 SC 1.4.1 — selected date uses accessible color contrast via --color-accent"
api_version: "react-day-picker@9 (v9 UI enum classNames, Chevron component)"
dependencies: ["react-day-picker@^9"]
drift_snapshot_ref: "practices-react/upstream/shadcn-registry-2026-05.snapshot.md#calendar"
---
*/
import * as React from 'react'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import { DayPicker, type ChevronProps } from 'react-day-picker'
import { cn } from '../lib/utils'
import { buttonVariants } from './button'

export type CalendarProps = React.ComponentProps<typeof DayPicker>

/**
 * Calendar — thin wrapper around react-day-picker v9 with design-token styles.
 *
 * API: react-day-picker@^9
 * - classNames use `UI` enum string values (not v8 snake_case)
 * - components uses `Chevron` (not `IconLeft`/`IconRight`)
 *
 * Pass `mode="single"` for single date, `mode="range"` for ranges,
 * `mode="multiple"` for multi-select.
 */
function Calendar({ className, classNames, showOutsideDays = true, ...props }: CalendarProps) {
  return (
    <DayPicker
      showOutsideDays={showOutsideDays}
      className={cn('p-[--space-3]', className)}
      classNames={{
        // v9 UI enum keys (string values of the UI enum)
        months: 'flex flex-col sm:flex-row space-y-[--space-4] sm:space-x-[--space-4] sm:space-y-0',
        month: 'space-y-[--space-4]',
        month_caption: 'flex justify-center pt-[--space-1] relative items-center',
        caption_label: 'text-[length:--text-sm] font-[number:--weight-medium]',
        nav: 'space-x-[--space-1] flex items-center',
        button_previous: cn(
          buttonVariants({ variant: 'outline' }),
          'h-7 w-7 bg-transparent p-0 opacity-50 hover:opacity-100 absolute left-[--space-1]'
        ),
        button_next: cn(
          buttonVariants({ variant: 'outline' }),
          'h-7 w-7 bg-transparent p-0 opacity-50 hover:opacity-100 absolute right-[--space-1]'
        ),
        month_grid: 'w-full border-collapse space-y-[--space-1]',
        weekdays: 'flex',
        weekday: 'text-[--color-text-muted] rounded-[--radius-md] w-9 font-normal text-[length:--text-xs]',
        week: 'flex w-full mt-[--space-2]',
        day: cn(
          'h-9 w-9 text-center text-[length:--text-sm] p-0 relative',
          '[&:has([aria-selected].range_end)]:rounded-r-[--radius-md]',
          '[&:has([aria-selected])]:bg-[--color-surface-subtle]',
          'first:[&:has([aria-selected])]:rounded-l-[--radius-md]',
          'last:[&:has([aria-selected])]:rounded-r-[--radius-md]',
          'focus-within:relative focus-within:z-20'
        ),
        day_button: cn(
          buttonVariants({ variant: 'ghost' }),
          'h-9 w-9 p-0 font-normal aria-selected:opacity-100'
        ),
        range_end: 'range_end',
        selected: 'bg-[--color-accent] text-[--color-text-inverse] hover:bg-[--color-accent-hover] hover:text-[--color-text-inverse] focus:bg-[--color-accent] focus:text-[--color-text-inverse]',
        today: 'bg-[--color-surface-subtle] text-[--color-text]',
        outside: 'text-[--color-text-muted] opacity-50 aria-selected:bg-[--color-surface-subtle]/50 aria-selected:text-[--color-text-muted] aria-selected:opacity-30',
        disabled: 'text-[--color-text-muted] opacity-50',
        range_middle: 'aria-selected:bg-[--color-surface-subtle] aria-selected:text-[--color-text]',
        hidden: 'invisible',
        ...classNames,
      }}
      components={{
        // v9 uses Chevron component with orientation prop (not IconLeft/IconRight)
        Chevron: ({ orientation, ...chevronProps }: ChevronProps) =>
          orientation === 'left' ? (
            <ChevronLeft className="h-4 w-4" {...chevronProps} />
          ) : (
            <ChevronRight className="h-4 w-4" {...chevronProps} />
          ),
      }}
      {...props}
    />
  )
}
Calendar.displayName = 'Calendar'

export { Calendar }
