/*
---
template_id: L1/components/progress
layer: L1
provenance_class: external_canonical
evidence:
  - source_type: upstream_id
    upstream_id: shadcn-ui-2026-05
    section: progress
    quote: "Displays an indicator showing the completion progress of a task."
  - source_type: upstream_id
    upstream_id: wcag-2-2
    section: 4.1.2-name-role-value
    quote: "For all user interface components, the name and role can be programmatically determined."
a11y_criteria:
  - "WCAG 2.2 SC 4.1.2 — role='progressbar', aria-valuenow/min/max via Radix"
  - "WCAG 2.2 SC 4.1.3 Status Messages"
  - "Indeterminate: aria-valuenow absent or aria-live parent with aria-label='Loading'"
dependencies: ["@radix-ui/react-progress"]
drift_snapshot_ref: "practices-react/upstream/shadcn-registry-2026-05.snapshot.md#progress"
---
*/
import * as React from 'react'
import * as ProgressPrimitive from '@radix-ui/react-progress'
import { cn } from '../lib/utils'

const Progress = React.forwardRef<
  React.ElementRef<typeof ProgressPrimitive.Root>,
  React.ComponentPropsWithoutRef<typeof ProgressPrimitive.Root>
>(({ className, value, ...props }, ref) => (
  <ProgressPrimitive.Root
    ref={ref}
    className={cn(
      'relative h-2 w-full overflow-hidden rounded-full bg-[--color-surface-subtle]',
      className
    )}
    {...props}
  >
    <ProgressPrimitive.Indicator
      className="h-full w-full flex-1 bg-[--color-accent] transition-all duration-[--duration-normal]"
      style={{ transform: `translateX(-${100 - (value || 0)}%)` }}
    />
  </ProgressPrimitive.Root>
))
Progress.displayName = ProgressPrimitive.Root.displayName

export { Progress }
