/*
---
template_id: L1/components/slider
layer: L1
provenance_class: external_canonical
evidence:
  - source_type: upstream_id
    upstream_id: shadcn-ui-2026-05
    section: slider
    quote: "An input where the user selects a value from within a given range."
  - source_type: upstream_id
    upstream_id: wcag-2-2
    section: 4.1.2-name-role-value
    quote: "For all user interface components, the name and role can be programmatically determined."
a11y_criteria:
  - "WCAG 2.2 SC 4.1.2 — role='slider', aria-valuenow/min/max via Radix"
  - "WCAG 2.2 SC 2.5.8 — thumb hit area ≥24×24px"
  - "Must add aria-label or aria-labelledby"
dependencies: ["@radix-ui/react-slider"]
drift_snapshot_ref: "practices-react/upstream/shadcn-registry-2026-05.snapshot.md#slider"
---
*/
import * as React from 'react'
import * as SliderPrimitive from '@radix-ui/react-slider'
import { cn } from '../lib/utils'

const Slider = React.forwardRef<
  React.ElementRef<typeof SliderPrimitive.Root>,
  React.ComponentPropsWithoutRef<typeof SliderPrimitive.Root>
>(({ className, ...props }, ref) => (
  <SliderPrimitive.Root
    ref={ref}
    className={cn(
      'relative flex w-full touch-none select-none items-center',
      className
    )}
    {...props}
  >
    <SliderPrimitive.Track
      className="relative h-1.5 w-full grow overflow-hidden rounded-full bg-[--color-border]"
    >
      <SliderPrimitive.Range className="absolute h-full bg-[--color-accent]" />
    </SliderPrimitive.Track>
    <SliderPrimitive.Thumb
      className={cn(
        'block h-4 w-4 rounded-full border border-[--color-accent]/50',
        'bg-[--color-surface] shadow-[--shadow-md]',
        'transition-colors duration-[--duration-fast]',
        'focus-visible:outline-none focus-visible:ring-2',
        'focus-visible:ring-[--color-focus-ring] focus-visible:ring-offset-2',
        'disabled:pointer-events-none disabled:opacity-50'
      )}
    />
  </SliderPrimitive.Root>
))
Slider.displayName = SliderPrimitive.Root.displayName

export { Slider }
