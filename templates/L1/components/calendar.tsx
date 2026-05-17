/*
---
template_id: L1/components/calendar
layer: L1
provenance_class: external_canonical
evidence:
  - source_type: upstream_id
    upstream_id: shadcn-ui-2026-05
    section: calendar
    quote: "A date field component that allows users to enter and edit date. Built on top of react-day-picker."
  - source_type: upstream_id
    upstream_id: wcag-2-2
    section: 1.3.1-info-and-relationships
    quote: "Information, structure, and relationships conveyed through presentation can be programmatically determined."
a11y_criteria:
  - "WCAG 2.2 SC 1.3.1 — role='grid' with aria-label, aria-selected on day buttons via react-day-picker"
  - "WCAG 2.2 SC 2.1.1 — keyboard navigation: Arrow keys move between dates, Enter selects"
  - "WCAG 2.2 SC 1.4.1 — selected date uses accessible color contrast via --color-accent"
dependencies: ["react-day-picker@^9", "@radix-ui/react-icons"]
drift_snapshot_ref: "practices-react/upstream/shadcn-registry-2026-05.snapshot.md#calendar"
---
*/
import * as React from 'react'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import { DayPicker } from 'react-day-picker'
import { cn } from '../lib/utils'
import { buttonVariants } from './button'

export type CalendarProps = React.ComponentProps<typeof DayPicker>

/**
 * Calendar — thin wrapper around react-day-picker with design-token styles.
 *
 * Pass `mode="single"` for single date, `mode="range"` for date ranges,
 * `mode="multiple"` for multi-select.
 */
function Calendar({ className, classNames, showOutsideDays = true, ...props }: CalendarProps) {
  return (
    <DayPicker
      showOutsideDays={showOutsideDays}
      className={cn('p-[--space-3]', className)}
      classNames={{
        months: 'flex flex-col sm:flex-row space-y-[--space-4] sm:space-x-[--space-4] sm:space-y-0',
        month: 'space-y-[--space-4]',
        caption: 'flex justify-center pt-[--space-1] relative items-center',
        caption_label: 'text-[length:--text-sm] font-[number:--weight-medium]',
        nav: 'space-x-[--space-1] flex items-center',
        nav_button: cn(
          buttonVariants({ variant: 'outline' }),
          'h-7 w-7 bg-transparent p-0 opacity-50 hover:opacity-100'
        ),
        nav_button_previous: 'absolute left-[--space-1]',
        nav_button_next: 'absolute right-[--space-1]',
        table: 'w-full border-collapse space-y-[--space-1]',
        head_row: 'flex',
        head_cell:
          'text-[--color-text-muted] rounded-[--radius-md] w-9 font-normal text-[length:--text-xs]',
        row: 'flex w-full mt-[--space-2]',
        cell: cn(
          'h-9 w-9 text-center text-[length:--text-sm] p-0 relative',
          '[&:has([aria-selected].day-range-end)]:rounded-r-[--radius-md]',
          '[&:has([aria-selected].day-outside)]:bg-[--color-surface-subtle]/50',
          '[&:has([aria-selected])]:bg-[--color-surface-subtle]',
          'first:[&:has([aria-selected])]:rounded-l-[--radius-md]',
          'last:[&:has([aria-selected])]:rounded-r-[--radius-md]',
          'focus-within:relative focus-within:z-20'
        ),
        day: cn(
          buttonVariants({ variant: 'ghost' }),
          'h-9 w-9 p-0 font-normal aria-selected:opacity-100'
        ),
        day_range_end: 'day-range-end',
        day_selected:
          'bg-[--color-accent] text-[--color-text-inverse] hover:bg-[--color-accent-hover] hover:text-[--color-text-inverse] focus:bg-[--color-accent] focus:text-[--color-text-inverse]',
        day_today: 'bg-[--color-surface-subtle] text-[--color-text]',
        day_outside:
          'day-outside text-[--color-text-muted] opacity-50 aria-selected:bg-[--color-surface-subtle]/50 aria-selected:text-[--color-text-muted] aria-selected:opacity-30',
        day_disabled: 'text-[--color-text-muted] opacity-50',
        day_range_middle:
          'aria-selected:bg-[--color-surface-subtle] aria-selected:text-[--color-text]',
        day_hidden: 'invisible',
        ...classNames,
      }}
      components={{
        IconLeft: ({ ...iconProps }) => <ChevronLeft className="h-4 w-4" {...iconProps} />,
        IconRight: ({ ...iconProps }) => <ChevronRight className="h-4 w-4" {...iconProps} />,
      }}
      {...props}
    />
  )
}
Calendar.displayName = 'Calendar'

export { Calendar }
